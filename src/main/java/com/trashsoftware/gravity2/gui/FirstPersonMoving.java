package com.trashsoftware.gravity2.gui;

import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;
import com.jme3.scene.Node;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Util;

public class FirstPersonMoving {

    protected ObjectModel objectModel;
    protected Node cameraNode = new Node("CameraNode");
    protected Node eastNode = new Node("EastNode");
    protected double longitude = 90;
    protected double latitude = 15;
    protected double altitude = 1e5f;

    protected double lookAzimuthDeg = 0;
    protected double lookAltitudeDeg = 0;

    FirstPersonMoving(ObjectModel objectModel) {
        this.objectModel = objectModel;

        Vector3f localPos = objectModel.calculateSurfacePosition(latitude,
                longitude,
                altitude).toVector3f();
        this.cameraNode.setLocalTranslation(localPos);
        Vector3f sightPos = objectModel.calculateSurfacePosition(latitude,
                longitude + 1,
                altitude).toVector3f();
        this.eastNode.setLocalTranslation(sightPos);
    }

    public CelestialObject getObject() {
        return objectModel.object;
    }

    public void azimuthChange(double changeAmountDeg) {
        this.lookAzimuthDeg += changeAmountDeg;
//        System.out.println("Azimuth change: " + changeAmountDeg + ", Azimuth: " + lookAzimuthDeg);
    }

    public void lookingAltitudeChange(double changeAlgDeg) {
        this.lookAltitudeDeg += changeAlgDeg;
        lookAltitudeDeg = Math.min(85, Math.max(-85, lookAltitudeDeg));
//        System.out.println("Alt change: " + changeAlgDeg + ", alt: " + lookAltitudeDeg);
    }

    public void moveForward(double moveDistance) {
        // Convert azimuth angle to radians for calculation
        double azimuthRad = Math.toRadians(lookAzimuthDeg);
        
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);
        
        // Calculate the change in latitude and longitude based on azimuth direction
        double deltaLat = moveDistance * Math.sin(azimuthRad); // Latitude change
        double deltaLon = moveDistance * Math.cos(azimuthRad) / Math.cos(latRad); // Longitude change, corrected for latitude

//        System.out.println("Delta: " + deltaLon + " " + deltaLat);
        
//         Update the latitude and longitude, convert back to degrees
        latRad = Util.clamp((latRad + deltaLat / getObject().getPolarRadius()), -Util.HALF_PI, Util.HALF_PI); // Ensure latitude stays within bounds
        lonRad = (lonRad + deltaLon / getObject().getEquatorialRadius()) % Util.TWO_PI; // Wrap longitude within 360 degrees
        
        longitude = Math.toDegrees(lonRad);
        latitude = Math.toDegrees(latRad);
//        System.out.println(longitude + " " + latitude);
        
        // Calculate the new position using updated latitude and longitude, keeping altitude constant
        Vector3f newPosition = objectModel.calculateSurfacePosition(latitude, 
                longitude, 
                altitude).toVector3f();

        // Set the new position of the cameraNode
        cameraNode.setLocalTranslation(newPosition);

        Vector3f newEast = objectModel.calculateSurfacePosition(latitude,
                longitude + 1,
                altitude).toVector3f();
        
        eastNode.setLocalTranslation(newEast);
    }

    public Vector3d getCurrentLocalPos() {
        return objectModel.calculateSurfacePosition(latitude,
                longitude,
                altitude);
    }

    public void updateCamera(Camera cam) {
        float azimuthAngle = (float) lookAzimuthDeg;
        float altitudeAngle = (float) lookAltitudeDeg;

        Vector3f upVector = getUpVector();
        // The camera will always look at the targetNode's position
        Vector3f targetPosition = eastNode.getWorldTranslation();

        // Create a local forward direction vector towards the target point
        Vector3f forwardDir = targetPosition.subtract(cameraNode.getWorldTranslation()).normalize();

        // Apply the azimuth rotation (rotate around the up vector)
        Quaternion azimuthRotation = new Quaternion().fromAngleAxis(azimuthAngle * FastMath.DEG_TO_RAD, upVector);
        forwardDir = azimuthRotation.mult(forwardDir); // Apply azimuth rotation

        // Apply the altitude (vertical) rotation (rotate around the camera's left vector)
        Quaternion altitudeRotation = new Quaternion().fromAngleAxis(altitudeAngle * FastMath.DEG_TO_RAD, cam.getLeft());
        forwardDir = altitudeRotation.mult(forwardDir); // Apply altitude rotation

        // Position the camera at the CameraNode and look in the adjusted forward direction
        cam.setLocation(cameraNode.getWorldTranslation());
        cam.lookAtDirection(forwardDir, upVector); // Look in the adjusted forward direction

    }

    public Vector3f getUpVector() {
        Vector3f centerPos = objectModel.rotatingNode.getWorldTranslation();
        return cameraNode.getWorldTranslation().subtract(centerPos).normalize();
    }
}
