package Raytracer.BVH;

import Raytracer.Intersection;
import Raytracer.Objects.Object3D;
import Raytracer.Ray;
import Raytracer.Vector3D;

import java.util.List;

/**
 * Represents a node in the Bounding Volume Hierarchy (BVH) tree structure.
 * Uses Surface Area Heuristic (SAH) with binned split-plane search to choose
 * near-optimal splits, and O(n) partitioning (no full sort) at each level.
 */
public class BVHNode {
    // Hard floor on leaf size. Not "4 because it worked for orchids" — this is a general
    // calibration constant. A floor of 2 (tried previously) let the tree split down to depth 28
    // with 1.2M+ nodes; the extra box-traversal cost from that many nodes outweighed the savings
    // from fewer triangle tests per leaf. 4 keeps recursion bounded to a sane leaf granularity
    // while still letting the SAH cost model (below) decide splits above this size.
    private static final int MAX_OBJECTS_PER_LEAF = 4;
    // Number of bins used to approximate the SAH cost function along an axis
    private static final int SAH_BINS = 16;
    // Relative cost of a ray-box test vs a ray-primitive test.
    // The classic SAH textbook ratio (1:2) assumes traversal and intersection cost roughly the
    // same order of magnitude. In practice here, box tests are cheap but numerous per ray, and
    // the per-node overhead (object allocation, pointer chasing, recursive calls) is higher than
    // the pure "cost of an intersect() call" the formula accounts for. Weighting intersection
    // cost higher tells SAH to prefer fewer, larger leaves unless splitting saves substantially
    // more triangle tests than that — reducing tree depth/node count for a given win in
    // avg-tests-per-ray. This is a calibration constant, not scene-specific tuning; if you add
    // box-test instrumentation (see note below) you can re-derive it from real measurements.
    private static final double TRAVERSAL_COST = 1.0;
    private static final double INTERSECTION_COST = 4.0;

    private BoundingBox bounds;
    private BVHNode left;
    private BVHNode right;
    private List<Object3D> objects; // only set for leaf nodes

    // Diagnostic counter — total ray-box tests performed during traversal (mirrors
    // Triangle.triangleTests). Uses LongAdder instead of a plain long: under the parallel
    // render loop, many threads incrementing a shared `long` field cause severe cache-line
    // contention (false sharing) that can seriously slow down the render itself. LongAdder
    // internally stripes the counter across cells to avoid that contention.
    public static java.util.concurrent.atomic.LongAdder boxTests = new java.util.concurrent.atomic.LongAdder();

    public BVHNode(List<Object3D> objs) {
        build(objs);
    }

    private void build(List<Object3D> objs) {
        this.bounds = computeBounds(objs);

        if (objs.size() <= MAX_OBJECTS_PER_LEAF) {
            objects = objs;
            return;
        }

        // Compute centroid bounds per axis (used to place bins), caching each object's
        // bounding box + centroid once so we don't recompute them for every axis evaluated below.
        Vector3D centroidMin = new Vector3D(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Vector3D centroidMax = new Vector3D(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        BoundingBox[] objBoxes = new BoundingBox[objs.size()];
        double[][] centroids = new double[objs.size()][3];
        for (int idx = 0; idx < objs.size(); idx++) {
            Object3D o = objs.get(idx);
            BoundingBox ob = o.getBoundingBox();
            objBoxes[idx] = ob;
            Vector3D c = ob.getCentroid();
            centroids[idx][0] = c.getX();
            centroids[idx][1] = c.getY();
            centroids[idx][2] = c.getZ();
            centroidMin = Vector3D.min(centroidMin, c);
            centroidMax = Vector3D.max(centroidMax, c);
        }
        Vector3D fullExtent = centroidMax.subtract(centroidMin);

        double parentArea = surfaceArea(bounds);
        double leafCost = INTERSECTION_COST * objs.size();

        // === Evaluate all 3 axes, keep the globally cheapest split ===
        // General SAH BVH builders (PBRT, Embree, etc.) always search every axis rather than
        // assuming the axis with the widest centroid spread is best. That assumption only holds
        // for axis-aligned, evenly distributed geometry — for anything rotated or elongated,
        // the widest-centroid axis can produce heavily overlapping child bounding boxes, which
        // makes rays traverse both children more often. Checking all 3 axes and picking the
        // cheapest by actual SAH cost is strictly more robust and never worse than a single axis.
        double bestCost = leafCost;
        int bestAxis = -1;
        int bestSplit = -1;
        int[] bestObjBin = null;

        for (int axis = 0; axis < 3; axis++) {
            double axisMin = centroidMin.get(axis);
            double axisExtent = fullExtent.get(axis);
            if (axisExtent < 1e-12) continue; // degenerate on this axis, skip

            BoundingBox[] binBounds = new BoundingBox[SAH_BINS];
            int[] binCounts = new int[SAH_BINS];
            int[] objBin = new int[objs.size()];

            for (int idx = 0; idx < objs.size(); idx++) {
                double c = centroids[idx][axis];
                int bin = (int) (((c - axisMin) / axisExtent) * SAH_BINS);
                bin = Math.min(SAH_BINS - 1, Math.max(0, bin));
                objBin[idx] = bin;

                BoundingBox ob = objBoxes[idx];
                binBounds[bin] = (binBounds[bin] == null) ? ob : BoundingBox.union(binBounds[bin], ob);
                binCounts[bin]++;
            }

            double[] leftArea = new double[SAH_BINS];
            int[] leftCount = new int[SAH_BINS];
            BoundingBox accLeft = null;
            int accLeftCount = 0;
            for (int i = 0; i < SAH_BINS; i++) {
                if (binBounds[i] != null) {
                    accLeft = (accLeft == null) ? binBounds[i] : BoundingBox.union(accLeft, binBounds[i]);
                    accLeftCount += binCounts[i];
                }
                leftArea[i] = accLeft == null ? 0.0 : surfaceArea(accLeft);
                leftCount[i] = accLeftCount;
            }

            double[] rightArea = new double[SAH_BINS];
            int[] rightCount = new int[SAH_BINS];
            BoundingBox accRight = null;
            int accRightCount = 0;
            for (int i = SAH_BINS - 1; i >= 0; i--) {
                if (binBounds[i] != null) {
                    accRight = (accRight == null) ? binBounds[i] : BoundingBox.union(accRight, binBounds[i]);
                    accRightCount += binCounts[i];
                }
                rightArea[i] = accRight == null ? 0.0 : surfaceArea(accRight);
                rightCount[i] = accRightCount;
            }

            for (int i = 0; i < SAH_BINS - 1; i++) {
                int nL = leftCount[i];
                int nR = rightCount[i + 1];
                if (nL == 0 || nR == 0) continue;

                double cost = TRAVERSAL_COST + INTERSECTION_COST *
                        (leftArea[i] / parentArea * nL + rightArea[i + 1] / parentArea * nR);

                if (cost < bestCost) {
                    bestCost = cost;
                    bestAxis = axis;
                    bestSplit = i;
                    bestObjBin = objBin;
                }
            }
        }

        // No axis produced a split cheaper than just being a leaf — stop here
        if (bestAxis == -1) {
            objects = objs;
            return;
        }

        // === Partition objects in O(n) (Hoare-style) based on the chosen bin boundary ===
        int n = objs.size();
        int i = 0, j = n - 1;
        while (i <= j) {
            while (i <= j && bestObjBin[i] <= bestSplit) i++;
            while (i <= j && bestObjBin[j] > bestSplit) j--;
            if (i < j) {
                java.util.Collections.swap(objs, i, j);
                int tmp = bestObjBin[i]; bestObjBin[i] = bestObjBin[j]; bestObjBin[j] = tmp;
                i++; j--;
            }
        }
        int mid = i;

        // Safety fallback: if binning produced a degenerate partition, fall back to median split
        if (mid == 0 || mid == n) {
            final int splitAxis = bestAxis;
            objs.sort(java.util.Comparator.comparingDouble(o -> o.getBoundingBox().getCentroid().get(splitAxis)));
            mid = n / 2;
        }

        left = new BVHNode(objs.subList(0, mid));
        right = new BVHNode(objs.subList(mid, n));
    }

    /**
     * Surface area of a bounding box — the core term in the SAH cost function.
     */
    private static double surfaceArea(BoundingBox b) {
        Vector3D d = b.max.subtract(b.min);
        double dx = Math.max(0, d.getX());
        double dy = Math.max(0, d.getY());
        double dz = Math.max(0, d.getZ());
        return 2.0 * (dx * dy + dy * dz + dz * dx);
    }

    private BoundingBox computeBounds(List<Object3D> objs) {
        BoundingBox box = objs.get(0).getBoundingBox();
        for (int i = 1; i < objs.size(); i++) {
            box = BoundingBox.union(box, objs.get(i).getBoundingBox());
        }
        return box;
    }

    public Intersection intersect(Ray ray, double tMin, double tMax) {
        boxTests.increment();
        if (!bounds.intersect(ray, tMin, tMax)) return null;

        if (objects != null) {
            Intersection closest = null;
            double closestT = tMax;

            for (Object3D obj : objects) {
                Intersection hit = obj.intersect(ray);
                if (hit != null && hit.getT() < closestT) {
                    closest = hit;
                    closestT = hit.getT();
                }
            }
            return closest;
        }

        Intersection firstHit = left.intersect(ray, tMin, tMax);
        if (firstHit != null) {
            Intersection secondHit = right.intersect(ray, tMin, firstHit.getT());
            return (secondHit != null && secondHit.getT() < firstHit.getT()) ? secondHit : firstHit;
        }

        return right.intersect(ray, tMin, tMax);
    }

    public BoundingBox getBounds() {
        return bounds;
    }

    public boolean isLeaf() {
        return left == null && right == null;
    }

    public int getDepth() {
        if (isLeaf()) return 1;
        return 1 + Math.max(left.getDepth(), right.getDepth());
    }

    public int countLeaves() {
        if (isLeaf()) return 1;
        return left.countLeaves() + right.countLeaves();
    }

    public int countTotalObjects() {
        if (isLeaf()) {
            return objects.size();
        }
        return left.countTotalObjects() + right.countTotalObjects();
    }
}