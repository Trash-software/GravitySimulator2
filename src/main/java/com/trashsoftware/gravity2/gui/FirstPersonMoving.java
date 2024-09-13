package com.trashsoftware.gravity2.gui;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.trashsoftware.gravity2.physics.CelestialObject;

public class FirstPersonMoving {

    protected ObjectModel objectModel;
    protected Node cameraNode = new Node("CameraNode");
    protected Node northNode = new Node("NorthNode");
    private double longitude = 180;
    private double latitude = 0;
    private double altitude;

    protected double compassAzimuth = 90;
    protected double lookAltitudeDeg = 0;

    FirstPersonMoving(ObjectModel objectModel, double altitude) {
        this.objectModel = objectModel;
        this.altitude = altitude;

        Vector3f localPos = objectModel.calculateSurfacePosition(latitude,
                longitude,
                altitude).toVector3f();
        this.cameraNode.setLocalTranslation(localPos);
        Vector3f sightPos = objectModel.calculateSurfacePosition(latitude + 1,
                longitude,
                altitude).toVector3f();
        this.northNode.setLocalTranslation(sightPos);
    }

    public CelestialObject getObject() {
        return objectModel.object;
    }

    public void azimuthChange(double changeAmountDeg) {
        this.compassAzimuth -= changeAmountDeg;
//        System.out.println("Azimuth change: " + changeAmountDeg + ", Azimuth: " + lookAzimuthDeg);
    }

    public void lookingAltitudeChange(double changeAlgDeg) {
        this.lookAltitudeDeg += changeAlgDeg;
        lookAltitudeDeg = Math.min(89, Math.max(-89, lookAltitudeDeg));
//        System.out.println("Alt: " + lookAltitudeDeg);
    }
    
    public void moveUp(double moveDistance) {
        altitude += moveDistance;
        updateNodePositions();
    }
    
    public void moveDown(double moveDistance) {
        altitude -= moveDistance;
        altitude = Math.max(1, altitude);
        updateNodePositions();
    }

    public void moveForward(double moveDistance) {
        double azimuthRad = Math.toRadians(compassAzimuth);
        
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);

        // Approximate the planet's radius at the current latitude using ellipsoid model
        double radiusAtLat = computeRadiusAtLatitude(latRad);

        // Calculate the angular distance (in radians) moved along the surface
        double angularDistance = moveDistance / radiusAtLat;
        // Update latitude based on movement along the great circle
        double newLatRad = Math.asin(Math.sin(latRad) * Math.cos(angularDistance) +
                Math.cos(latRad) * Math.sin(angularDistance) * Math.cos(azimuthRad));

        // Update longitude based on movement along the great circle
        double deltaLonRad = Math.atan2(Math.sin(azimuthRad) * Math.sin(angularDistance) * Math.cos(latRad),
                Math.cos(angularDistance) - Math.sin(latRad) * Math.sin(newLatRad));
        double newLonRad = lonRad + deltaLonRad;

        // Normalize longitude to the range [-180, 180]
        newLonRad = (newLonRad + Math.PI) % (2 * Math.PI) - Math.PI;

        longitude = Math.toDegrees(newLonRad);
        latitude = Math.toDegrees(newLatRad);
//        System.out.println(longitude + " " + latitude);
        
        compassAzimuth = computeNewAzimuth(latRad, lonRad, newLatRad, newLonRad);
        
        updateNodePositions();
    }
    
    private void updateNodePositions() {
        // Calculate the new position using updated latitude and longitude, keeping altitude constant
        Vector3f newPosition = objectModel.calculateSurfacePosition(latitude,
                longitude,
                altitude).toVector3f();

        // Set the new position of the cameraNode
        cameraNode.setLocalTranslation(newPosition);

        double northLatitude = Math.min(90, latitude + 1);

        Vector3f newNorth = objectModel.calculateSurfacePosition(northLatitude,
                longitude,
                altitude).toVector3f();

        northNode.setLocalTranslation(newNorth);
    }

    /**
     * Compute the forward azimuth (heading) between two points on the surface of a sphere (great circle).
     * @param lat1 - starting latitude in radians.
     * @param lon1 - starting longitude in radians.
     * @param lat2 - new latitude in radians after moving.
     * @param lon2 - new longitude in radians after moving.
     * @return the new heading (azimuth) in degrees.
     */
    private double computeNewAzimuth(double lat1, double lon1, double lat2, double lon2) {
        double deltaLon = lon2 - lon1;

        // Forward azimuth formula based on spherical trigonometry (law of cosines for spherical surfaces)
        double y = Math.sin(deltaLon) * Math.cos(lat1);
//        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        double x = -Math.cos(lat2) * Math.sin(lat1) + Math.sin(lat2) * Math.cos(lat1) * Math.cos(deltaLon);

        // Calculate the azimuth (heading) in radians
        double azimuthRad = Math.atan2(y, x);

        // Convert from radians to degrees and normalize the result to the range [0, 360]
        double azimuthDeg = Math.toDegrees(azimuthRad);
        return (azimuthDeg + 360) % 360;  // Normalize the heading
    }

    public Vector3d getCurrentLocalPos() {
        return objectModel.calculateSurfacePosition(latitude,
                longitude,
                altitude);
    }

    public void updateCamera(Camera cam) {
        float azimuthRad = (float) compassAzimuthToGame(compassAzimuth + 90) * FastMath.DEG_TO_RAD;  // node is north
        float altitudeRad = (float) lookAltitudeDeg * FastMath.DEG_TO_RAD;

        // The unit vector pointing to the sky
        Vector3f upVector = getUpVector();
        // The camera will always look at the targetNode's position
        Vector3f targetPosition = northNode.getWorldTranslation();

        // Create a local forward direction vector towards the target point
        Vector3f forwardDir = targetPosition.subtract(cameraNode.getWorldTranslation()).normalize();

        // Apply azimuth rotation (rotate around the up vector)
        Quaternion azimuthRotation = new Quaternion().fromAngleAxis(azimuthRad, upVector);
        forwardDir = azimuthRotation.mult(forwardDir);

        // Instead of rotating around the camera's left vector, use a safe method:
        // Calculate a right vector from the forward direction and up vector
        Vector3f rightVector = forwardDir.cross(upVector).normalizeLocal();

        // Apply altitude rotation (rotate around the right vector to avoid gimbal lock)
        Quaternion altitudeRotation = new Quaternion().fromAngleAxis(altitudeRad, rightVector);
        forwardDir = altitudeRotation.mult(forwardDir);

        // Position the camera at the CameraNode and look in the adjusted forward direction
        cam.setLocation(cameraNode.getWorldTranslation());
        cam.lookAtDirection(forwardDir, upVector); // Look in the adjusted forward direction
    }

    /**
     * Compute the radius at the current latitude for an ellipsoid.
     * @param latRad - the latitude in radians.
     * @return the radius at the given latitude.
     */
    private double computeRadiusAtLatitude(double latRad) {
        CelestialObject co = objectModel.object;
        // Ellipsoid approximation for radius at a given latitude
        double a = co.getEquatorialRadius();
        double b = co.getPolarRadius();

        double cosLat = Math.cos(latRad);
        double sinLat = Math.sin(latRad);

        // Formula for the radius of the ellipsoid at a given latitude
        double numerator = (a * a * cosLat) * (a * a * cosLat) + (b * b * sinLat) * (b * b * sinLat);
        double denominator = (a * cosLat) * (a * cosLat) + (b * sinLat) * (b * sinLat);

        return Math.sqrt(numerator / denominator);
    }

    public Vector3f getUpVector() {
        Vector3f centerPos = objectModel.rotatingNode.getWorldTranslation();
        return cameraNode.getWorldTranslation().subtract(centerPos).normalize();
    }

    public double getAltitude() {
        return altitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getGeologicalLongitude() {
        double lon = longitude + 180;
        if (lon > 180) {
            lon -= 360;
        }
        return lon;
    }

    // Converts from 3D coordinate system azimuth (0° = East, counterclockwise) to real-world compass azimuth (0° = North, clockwise)
    public static double gameAzimuthToCompass(double azimuth3D) {
        // Adjust the azimuth from East-based (3D) to North-based (Compass) and change direction
        double compassAzimuth = 90 - azimuth3D;

        // Ensure the result is in the range [0, 360)
        if (compassAzimuth < 0) {
            compassAzimuth += 360;
        }

        return compassAzimuth;
    }

    // Converts from real-world compass azimuth (0° = North, clockwise) to 3D coordinate system azimuth (0° = East, counterclockwise)
    public static double compassAzimuthToGame(double compassAzimuth) {
        // Adjust the azimuth from North-based (Compass) to East-based (3D) and change direction
        double azimuth3D = 90 - compassAzimuth;

        // Ensure the result is in the range [0, 360)
        if (azimuth3D < 0) {
            azimuth3D += 360;
        }

        return azimuth3D;
    }
}
