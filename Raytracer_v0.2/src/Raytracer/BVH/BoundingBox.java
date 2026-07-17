package Raytracer.BVH;

import Raytracer.Ray;
import Raytracer.Vector3D;

/**
 * Represents an axis-aligned bounding box (AABB) used in the BVH (Bounding Volume Hierarchy).
 * A bounding box is defined by its minimum and maximum corner points in 3D space.
 */
public class BoundingBox {
    // Minimum corner of the bounding box (smallest x, y, z coordinates)
    public Vector3D min;
    // Maximum corner of the bounding box (largest x, y, z coordinates)
    public Vector3D max;
    // Small epsilon value used for floating-point comparisons to handle precision issues
    public static final double EPSILON = 1e-8;

    private static final double MIN_THICKNESS = 1e-4;

    /**
     * Constructor that creates a bounding box with specified minimum and maximum points.
     * @param min The minimum corner point of the bounding box
     * @param max The maximum corner point of the bounding box
     */
    public BoundingBox(Vector3D min, Vector3D max) {
        double px = padding(max.getX() - min.getX());
        double py = padding(max.getY() - min.getY());
        double pz = padding(max.getZ() - min.getZ());

        this.min = new Vector3D(min.getX() - px, min.getY() - py, min.getZ() - pz);
        this.max = new Vector3D(max.getX() + px, max.getY() + py, max.getZ() + pz);
    }

    /**
     * Computes the half-padding needed on one axis so its total extent reaches
     * MIN_THICKNESS. Returns 0 if the axis already has enough extent (idempotent —
     * union()-ing already-padded boxes doesn't keep growing them).
     */
    private static double padding(double extent) {
        return extent < MIN_THICKNESS ? (MIN_THICKNESS - extent) / 2.0 : 0.0;
    }

    /**
     * Creates a new bounding box that encloses both input bounding boxes.
     * This is used to merge bounding boxes when building the BVH structure.
     * @param a First bounding box to merge
     * @param b Second bounding box to merge
     * @return A new BoundingBox that contains both input boxes
     */
    public static BoundingBox union(BoundingBox a, BoundingBox b) {
        return new BoundingBox(
                Vector3D.min(a.min, b.min), // Take minimum of each coordinate component
                Vector3D.max(a.max, b.max)  // Take maximum of each coordinate component
        );
    }

    /**
     * Calculates and returns the centroid (center point) of the bounding box.
     * The centroid is computed as the average of the min and max points.
     * @return Vector3D representing the center point of the bounding box
     */
    public Vector3D getCentroid() {
        // Average min and max coordinates using vector operations
        return min.add(max).divide(2.0);
    }

    /**
     * Tests if a ray intersects with this bounding box using the slab method.
     * The slab method treats the box as the intersection of three pairs of parallel planes.
     * @param ray The ray to test for intersection
     * @param tMin Minimum t parameter value (near clipping plane)
     * @param tMax Maximum t parameter value (far clipping plane)
     * @return true if the ray intersects the bounding box within [tMin, tMax], false otherwise
     */
    public boolean intersect(Ray ray, double tMin, double tMax) {
        // Test intersection against each axis (x, y, z)
        for (int i = 0; i < 3; i++) {
            // Get ray origin and direction components for current axis
            double origin = ray.getOrigin().get(i);
            double dir = ray.getDirection().get(i);

            // Check if ray is parallel to the slab (direction component near zero)
            if (Math.abs(dir) < EPSILON) {
                // Ray is parallel to the slab - check if origin is within slab bounds
                if (origin < min.get(i) || origin > max.get(i)) return false;
            } else {
                // Use the ray's precomputed inverse direction instead of dividing here.
                double invD = ray.getInvDirection().get(i);
                double t0 = (min.get(i) - origin) * invD; // Intersection with min plane
                double t1 = (max.get(i) - origin) * invD; // Intersection with max plane

                // Ensure t0 is the near intersection and t1 is the far intersection
                if (invD < 0.0) {
                    // Direction is negative, so swap t0 and t1
                    double tmp = t0;
                    t0 = t1;
                    t1 = tmp;
                }

                // Update the intersection interval
                tMin = Math.max(tMin, t0); // Move near intersection further if needed
                tMax = Math.min(tMax, t1); // Move far intersection closer if needed

                // If the interval becomes invalid, there's no intersection
                if (tMax <= tMin) return false;
            }
        }
        // If we've processed all three axes without early exit, there is an intersection
        return true;
    }
}