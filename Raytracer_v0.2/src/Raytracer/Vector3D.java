package Raytracer;

/**
 * Vector3D class represents a 3D vector with x, y, z components
 * Provides comprehensive vector operations for 3D mathematics including
 * arithmetic operations, geometric calculations, and transformations
 */
public class Vector3D {
    // Static constant for zero vector - shared instance for memory efficiency
    private static final Vector3D ZERO = new Vector3D(0, 0, 0);
    public double x, y, z; // 3D vector components

    /**
     * Constructor - creates a new 3D vector with specified components
     * @param x X component of the vector
     * @param y Y component of the vector
     * @param z Z component of the vector
     */
    public Vector3D(double x, double y, double z) {
        setX(x);
        setY(y);
        setZ(z);
    }

    /**
     * Get the X component of the vector
     * @return X component value
     */
    public double getX() {
        return x;
    }

    /**
     * Set the X component of the vector
     * @param x New X component value
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * Get the Y component of the vector
     * @return Y component value
     */
    public double getY() {
        return y;
    }

    /**
     * Set the Y component of the vector
     * @param y New Y component value
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * Get the Z component of the vector
     * @return Z component value
     */
    public double getZ() {
        return z;
    }

    /**
     * Set the Z component of the vector
     * @param z New Z component value
     */
    public void setZ(double z) {
        this.z = z;
    }

    /**
     * Create a copy (clone) of this vector
     * @return New Vector3D instance with same component values
     */
    public Vector3D clone(){
        return new Vector3D(getX(), getY(), getZ());
    }

    /**
     * Static method to get a new zero vector (0, 0, 0)
     * @return New Vector3D instance representing zero vector
     */
    public static Vector3D ZERO(){
        return ZERO.clone();
    }

    /**
     * Vector addition - adds another vector to this vector
     * @param v Vector to add to this vector
     * @return New Vector3D representing the sum of both vectors
     */
    public Vector3D add(Vector3D v) {
        return new Vector3D(this.x + v.x, this.y + v.y, this.z + v.z);
    }

    /**
     * Vector subtraction - subtracts another vector from this vector
     * @param v Vector to subtract from this vector
     * @return New Vector3D representing the difference (this - v)
     */
    public Vector3D subtract(Vector3D v) {
        return new Vector3D(this.x - v.x, this.y - v.y, this.z - v.z);
    }

    /**
     * Scalar multiplication - multiplies vector by a scalar value
     * @param scalar Value to multiply each component by
     * @return New Vector3D with each component multiplied by scalar
     */
    public Vector3D multiply(double scalar) {
        return new Vector3D(this.x * scalar, this.y * scalar, this.z * scalar);
    }

    /**
     * Scalar division - divides vector by a scalar value
     * @param scalar Value to divide each component by
     * @return New Vector3D with each component divided by scalar
     */
    public Vector3D divide(double scalar) {
        return new Vector3D(this.x / scalar, this.y / scalar, this.z / scalar);
    }

    /**
     * Dot product - calculates the dot product between this vector and another
     * Used for calculating angles, projections, and lighting calculations
     * @param v Vector to calculate dot product with
     * @return Scalar value representing the dot product
     */
    public double dot(Vector3D v) {
        return this.x * v.x + this.y * v.y + this.z * v.z;
    }

    /**
     * Cross product - calculates the cross product between this vector and another
     * Results in a vector perpendicular to both input vectors
     * Used for calculating surface normals and rotations
     * @param v Vector to calculate cross product with
     * @return New Vector3D representing the cross product (this × v)
     */
    public Vector3D cross(Vector3D v) {
        return new Vector3D(
                this.y * v.z - this.z * v.y,  // i component
                this.z * v.x - this.x * v.z,  // j component
                this.x * v.y - this.y * v.x   // k component
        );
    }

    /**
     * Calculate the magnitude (length) of the vector
     * Uses Euclidean distance formula: sqrt(x² + y² + z²)
     * @return Length of the vector as a double
     */
    public double magnitude() {
        return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
    }

    /**
     * Normalize the vector - convert to unit vector (magnitude = 1)
     * Preserves direction but makes length exactly 1
     * @return New Vector3D representing the normalized vector
     */
    public Vector3D normalize() {
        double mag = magnitude();
        return new Vector3D(this.x / mag, this.y / mag, this.z / mag);
    }

    /**
     * Negate the vector - flip the direction by negating all components
     * @return New Vector3D with all components negated
     */
    public Vector3D negate() {
        return new Vector3D(-x, -y, -z);
    }

    /**
     * Get vector component by index (0=x, 1=y, 2=z)
     * Useful for iterating over components or array-like access
     * @param i Index of component to retrieve (0, 1, or 2)
     * @return Component value at specified index
     * @throws IndexOutOfBoundsException if index is not 0, 1, or 2
     */
    public double get(int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            case 2: return z;
            default: throw new IndexOutOfBoundsException("Vector3D index must be 0, 1, or 2.");
        }
    }

    /**
     * Static method to find component-wise minimum of two vectors
     * Creates new vector with minimum x, y, z components from both inputs
     * Used for bounding box calculations
     * @param a First vector to compare
     * @param b Second vector to compare
     * @return New Vector3D with minimum components from both vectors
     */
    public static Vector3D min(Vector3D a, Vector3D b) {
        return new Vector3D(
                Math.min(a.getX(), b.getX()),
                Math.min(a.getY(), b.getY()),
                Math.min(a.getZ(), b.getZ())
        );
    }

    /**
     * Static method to find component-wise maximum of two vectors
     * Creates new vector with maximum x, y, z components from both inputs
     * Used for bounding box calculations
     * @param a First vector to compare
     * @param b Second vector to compare
     * @return New Vector3D with maximum components from both vectors
     */
    public static Vector3D max(Vector3D a, Vector3D b) {
        return new Vector3D(
                Math.max(a.getX(), b.getX()),
                Math.max(a.getY(), b.getY()),
                Math.max(a.getZ(), b.getZ())
        );
    }

    /**
     * Static method to rotate a point around the origin using Euler angles
     * Applies rotations in order: X-axis, Y-axis, Z-axis
     * @param point Point to rotate
     * @param rotationX Rotation around X-axis in degrees
     * @param rotationY Rotation around Y-axis in degrees
     * @param rotationZ Rotation around Z-axis in degrees
     * @return New Vector3D representing the rotated point
     */
    public static Vector3D rotate(Vector3D point, double rotationX, double rotationY, double rotationZ) {
        // Early exit if no rotation needed
        if(rotationX == 0 && rotationY == 0 && rotationZ == 0) {
            return point;
        }

        // Initialize result vector
        Vector3D point2 = new Vector3D(0, 0, 0);

        // Convert degrees to radians
        rotationX = Math.toRadians(rotationX);
        rotationY = Math.toRadians(rotationY);
        rotationZ = Math.toRadians(rotationZ);

        // Pre-calculate trigonometric values for efficiency
        double cosX = Math.cos(rotationX), sinX = Math.sin(rotationX);
        double cosY = Math.cos(rotationY), sinY = Math.sin(rotationY);
        double cosZ = Math.cos(rotationZ), sinZ = Math.sin(rotationZ);

        // Get initial coordinates
        double x = point.getX(), y = point.getY(), z = point.getZ();

        // Rotation around X-axis (affects Y and Z)
        double y1 = y * cosX - z * sinX;
        double z1 = y * sinX + z * cosX;
        y = y1;
        z = z1;

        // Rotation around Y-axis (affects X and Z)
        double x2 = x * cosY + z * sinY;
        double z2 = -x * sinY + z * cosY;
        x = x2;
        z = z2;

        // Rotation around Z-axis (affects X and Y)
        double x3 = x * cosZ - y * sinZ;
        double y3 = x * sinZ + y * cosZ;
        x = x3;
        y = y3;

        // Set final coordinates
        point2.setX(x);
        point2.setY(y);
        point2.setZ(z);

        return point2;
    }

    /**
     * Safe normalize - normalizes the vector with protection against zero-length vectors
     * If vector magnitude is very small, returns default up vector (0, 1, 0)
     * @return Normalized vector, or default up vector if original was too small
     */
    public Vector3D normalizeSafe() {
        double mag = this.magnitude();
        // Use small epsilon to avoid division by very small numbers
        return mag > 1e-6 ? this.multiply(1.0 / mag) : new Vector3D(0, 1, 0);
    }

    /**
     * Linear interpolation between this vector and another.
     * @param other Target vector
     * @param t Interpolation factor [0,1]
     * @return Interpolated vector
     */
    public Vector3D lerp(Vector3D other, double t) {
        return new Vector3D(
                this.x + (other.x - this.x) * t,
                this.y + (other.y - this.y) * t,
                this.z + (other.z - this.z) * t
        );
    }

    /**
     * Checks equality based on component values with a small epsilon tolerance.
     * Required for correct HashSet deduplication in Model3D.setTriangles().
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Vector3D)) return false;
        Vector3D other = (Vector3D) obj;
        double eps = 1e-10;
        return Math.abs(this.x - other.x) < eps &&
                Math.abs(this.y - other.y) < eps &&
                Math.abs(this.z - other.z) < eps;
    }

    /**
     * Generates a random point uniformly distributed inside a sphere of given radius,
     * centered at the origin. Uses rejection sampling to avoid the non-uniform density
     * you'd get from naively scaling random spherical coordinates.
     * Used for soft-shadow sampling — jittering a light's position within its physical size.
     * @param radius Radius of the sphere to sample within
     * @return Random point inside the sphere, or ZERO if radius <= 0
     */
    public static Vector3D randomInSphere(double radius) {
        if (radius <= 0) return ZERO();
        double x, y, z;
        do {
            x = 2 * Math.random() - 1;
            y = 2 * Math.random() - 1;
            z = 2 * Math.random() - 1;
        } while (x * x + y * y + z * z > 1.0);
        return new Vector3D(x * radius, y * radius, z * radius);
    }

    /**
     * Generates a cosine-weighted random direction in the hemisphere oriented around the given
     * normal. Used for ambient occlusion sampling — cosine weighting biases samples toward
     * directions that contribute most to a Lambertian ambient term, giving lower-noise AO for
     * the same sample count than uniform hemisphere sampling.
     * @param normal The normal defining the hemisphere's orientation (assumed normalized)
     * @return Random unit direction in the hemisphere around normal
     */
    public static Vector3D randomCosineHemisphere(Vector3D normal) {
        double u1 = Math.random();
        double u2 = Math.random();
        double r = Math.sqrt(u1);
        double theta = 2 * Math.PI * u2;

        double x = r * Math.cos(theta);
        double y = r * Math.sin(theta);
        double z = Math.sqrt(Math.max(0.0, 1.0 - u1));

        // Build an orthonormal basis around 'normal' (treated as local Z) to orient the sample
        Vector3D arbitrary = Math.abs(normal.getY()) < 0.99 ? new Vector3D(0, 1, 0) : new Vector3D(1, 0, 0);
        Vector3D tangent = normal.cross(arbitrary).normalize();
        Vector3D bitangent = normal.cross(tangent).normalize();

        return tangent.multiply(x).add(bitangent.multiply(y)).add(normal.multiply(z)).normalize();
    }

    /**
     * Hash code consistent with equals — uses rounded values to handle epsilon tolerance.
     */
    @Override
    public int hashCode() {
        // Round to 9 decimal places to match epsilon tolerance in equals
        long bx = Double.doubleToLongBits(Math.round(x * 1e9) / 1e9);
        long by = Double.doubleToLongBits(Math.round(y * 1e9) / 1e9);
        long bz = Double.doubleToLongBits(Math.round(z * 1e9) / 1e9);
        int result = (int)(bx ^ (bx >>> 32));
        result = 31 * result + (int)(by ^ (by >>> 32));
        result = 31 * result + (int)(bz ^ (bz >>> 32));
        return result;
    }

    /**
     * String representation of the vector for debugging and display
     * @return Formatted string showing vector components with 2 decimal places
     */
    @Override
    public String toString() {
        return String.format("Vector3D(%.2f, %.2f, %.2f)", x, y, z);
    }
}