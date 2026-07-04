package Raytracer;

/**
 * Camera class represents a virtual camera in 3D space for ray tracing.
 * Handles camera positioning, orientation, and ray generation for each pixel.
 * Uses a perspective projection model with configurable field of view.
 */
public class Camera {
    // Camera position in 3D world coordinates
    private Vector3D position;

    // Direction vector the camera is facing (normalized)
    private Vector3D forward;

    // Rightward direction vector (perpendicular to forward, normalized)
    private Vector3D right;

    // Upward direction vector (normalized)
    private Vector3D up;

    // Field of view angle in radians (converted from degrees in constructor)
    private double fov;

    // Aspect ratio of the image (width/height)
    private double aspectRatio;

    // Near clipping plane distance (closest objects rendered)
    private double near;

    // Far clipping plane distance (farthest objects rendered)
    private double far;

    /**
     * Constructor creates a camera with specified parameters.
     * @param position Camera's position in 3D space
     * @param lookAt Point the camera is looking towards
     * @param up Up direction vector (typically (0,1,0) for Y-up)
     * @param fov Field of view in degrees
     * @param aspectRatio Image aspect ratio (width/height)
     * @param near Near clipping plane distance
     * @param far Far clipping plane distance
     */
    public Camera(Vector3D position, Vector3D lookAt, Vector3D up, double fov, double aspectRatio, double near, double far) {
        // Store camera position
        this.position = position;

        // Calculate forward direction by subtracting position from lookAt target
        this.forward = lookAt.subtract(position).normalize();

        // Calculate right direction using cross product (forward × up)
        this.right = this.forward.cross(up).normalize();

        // Recalculate up direction to ensure orthogonality (right × forward)
        this.up = this.right.cross(this.forward).normalize();

        // Convert field of view from degrees to radians for calculations
        this.fov = Math.toRadians(fov);

        // Store image properties
        this.aspectRatio = aspectRatio;
        this.near = near;
        this.far = far;
    }

    /**
     * Generates a ray for a given pixel coordinate in normalized device coordinates.
     * Converts 2D screen coordinates to 3D ray direction through perspective projection.
     * @param x Normalized x coordinate [-1,1] (left to right)
     * @param y Normalized y coordinate [-1,1] (top to bottom)
     * @return Ray object starting from camera position in calculated direction
     */
    public Ray generateRay(double x, double y) {
        // Calculate scale factor based on field of view
        // tan(fov/2) gives the half-width of the view plane at unit distance
        double scale = Math.tan(fov / 2);

        // Convert normalized coordinates to view plane coordinates
        // Scale x by aspect ratio to account for rectangular images
        double pixelX = (2 * x - 1) * aspectRatio * scale;

        // Flip y-axis for correct image orientation (screen Y goes down, world Y goes up)
        double pixelY = (1 - 2 * y) * scale;

        // Compute ray direction by combining camera basis vectors
        // Start with forward direction, then add scaled right and up components
        Vector3D rayDirection = forward
                .add(right.multiply(pixelX))    // Add horizontal offset
                .add(up.multiply(pixelY))       // Add vertical offset
                .normalize();                   // Normalize to unit length

        // Create and return ray from camera position in calculated direction
        return new Ray(position, rayDirection);
    }

    /**
     * Gets the camera's position in 3D space.
     * @return Camera position as Vector3D
     */
    public Vector3D getPosition() {
        return position;
    }

    /**
     * Gets the camera's forward direction vector.
     * @return Normalized forward direction as Vector3D
     */
    public Vector3D getForward() {
        return forward;
    }

    /**
     * Gets the near clipping plane distance.
     * @return Near plane distance as double
     */
    public double getNear() {
        return near;
    }

    /**
     * Gets the far clipping plane distance.
     * @return Far plane distance as double
     */
    public double getFar() {
        return far;
    }
}