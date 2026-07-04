package Raytracer.BVH;

import Raytracer.Intersection;
import Raytracer.Objects.Object3D;
import Raytracer.Ray;
import Raytracer.Vector3D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a node in the Bounding Volume Hierarchy (BVH) tree structure.
 * Each node contains either child nodes (internal node) or a list of objects (leaf node).
 * The BVH is used to accelerate ray-object intersection tests by organizing objects
 * in a hierarchical spatial data structure.
 */
public class BVHNode {
    // Maximum number of objects allowed in a leaf node before splitting
    private static final int MAX_OBJECTS_PER_LEAF = 2;
    // The axis (0=X, 1=Y, 2=Z) along which this node was split
    private int splitAxis;

    // Bounding box that encloses all objects in this node's subtree
    private BoundingBox bounds;
    // Left child node (contains objects with smaller coordinates along split axis)
    private BVHNode left;
    // Right child node (contains objects with larger coordinates along split axis)
    private BVHNode right;
    // List of objects stored in this node (only used for leaf nodes)
    private List<Object3D> objects; // hojas

    /**
     * Constructor that builds a BVH node from a list of objects.
     * @param objs List of 3D objects to organize in this node
     */
    public BVHNode(List<Object3D> objs) {
        build(objs);
    }

    /**
     * Recursively builds the BVH structure for the given objects.
     * Creates leaf nodes for small object lists, or splits into child nodes for larger lists.
     * @param objs List of objects to organize in this node's subtree
     */
    private void build(List<Object3D> objs) {
        // Compute bounding box that contains all objects in this node
        this.bounds = computeBounds(objs);

        // If we have few enough objects, make this a leaf node
        if (objs.size() <= MAX_OBJECTS_PER_LEAF) {
            objects = objs;
            return; // No need to split further
        }

        // Determine split axis based on bounding box extent (choose longest axis)
        Vector3D extent = bounds.max.subtract(bounds.min); // Calculate box dimensions
        if (extent.getX() > extent.getY() && extent.getX() > extent.getZ()) {
            splitAxis = 0; // X axis has the largest extent
        } else if (extent.getY() > extent.getZ()) {
            splitAxis = 1; // Y axis has the largest extent
        } else {
            splitAxis = 2; // Z axis has the largest extent
        }

        // Sort objects along the chosen axis by their bounding box centroids
        objs.sort(Comparator.comparingDouble(o -> o.getBoundingBox().getCentroid().get(splitAxis)));

        // Split objects into two roughly equal halves
        int mid = objs.size() / 2;
        // Recursively create left child with first half of objects
        left = new BVHNode(objs.subList(0, mid));
        // Recursively create right child with second half of objects
        right = new BVHNode(objs.subList(mid, objs.size()));
    }

    /**
     * Computes a bounding box that encloses all the given objects.
     * @param objs List of objects to compute bounding box for
     * @return BoundingBox that contains all input objects
     */
    private BoundingBox computeBounds(List<Object3D> objs) {
        // Start with the first object's bounding box
        BoundingBox box = objs.get(0).getBoundingBox();
        // Union with all other objects' bounding boxes
        for (int i = 1; i < objs.size(); i++) {
            box = BoundingBox.union(box, objs.get(i).getBoundingBox());
        }
        return box;
    }

    /**
     * Tests for ray intersection with objects in this node's subtree.
     * Uses the BVH structure to efficiently cull non-intersecting branches.
     * @param ray The ray to test for intersections
     * @param tMin Minimum t parameter value for valid intersections
     * @param tMax Maximum t parameter value for valid intersections
     * @return The closest intersection within [tMin, tMax], or null if none found
     */
    public Intersection intersect(Ray ray, double tMin, double tMax) {
        // First check if ray intersects this node's bounding box
        if (!bounds.intersect(ray, tMin, tMax)) return null;

        // If this is a leaf node, test intersection with all contained objects
        if (objects != null) {
            Intersection closest = null;
            double closestT = tMax;

            // Test ray against each object in this leaf
            for (Object3D obj : objects) {
                Intersection hit = obj.intersect(ray);
                // Keep track of the closest intersection found so far
                if (hit != null && hit.getT() < closestT) {
                    closest = hit;
                    closestT = hit.getT();
                }
            }
            return closest;
        }

        // For internal nodes, recursively test both children
        // Test left child first
        Intersection firstHit = left.intersect(ray, tMin, tMax);
        if (firstHit != null) {
            // If left child has intersection, test right child with updated tMax
            // to find potentially closer intersections
            Intersection secondHit = right.intersect(ray, tMin, firstHit.getT());
            // Return the closer of the two intersections
            return (secondHit != null && secondHit.getT() < firstHit.getT()) ? secondHit : firstHit;
        }

        // If left child had no intersection, test right child with original bounds
        return right.intersect(ray, tMin, tMax);
    }

    /**
     * Returns the bounding box for this node.
     * @return BoundingBox that encloses all objects in this node's subtree
     */
    public BoundingBox getBounds() {
        return bounds;
    }

    /**
     * Checks if this node is a leaf node (contains objects directly).
     * @return true if this is a leaf node, false if it's an internal node
     */
    public boolean isLeaf() {
        return left == null && right == null;
    }

    /**
     * Calculates the maximum depth of the BVH tree rooted at this node.
     * @return The depth of the deepest leaf node in this subtree
     */
    public int getDepth() {
        if (isLeaf()) return 1; // Leaf nodes have depth 1
        // Internal nodes have depth 1 + maximum depth of children
        return 1 + Math.max(left.getDepth(), right.getDepth());
    }

    /**
     * Counts the total number of leaf nodes in this subtree.
     * @return Number of leaf nodes in the subtree rooted at this node
     */
    public int countLeaves() {
        if (isLeaf()) return 1; // This node is a leaf
        // Sum leaf counts from both children
        return left.countLeaves() + right.countLeaves();
    }

    /**
     * Counts the total number of objects stored in all leaf nodes of this subtree.
     * @return Total number of objects in the subtree rooted at this node
     */
    public int countTotalObjects() {
        if (isLeaf()) {
            // Leaf node: return number of objects stored directly
            return objects.size();
        }
        // Internal node: sum object counts from both children
        return left.countTotalObjects() + right.countTotalObjects();
    }
}