package com.trashsoftware.gravity2.gui;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;

public class FirstPersonMoving {
    
    protected ObjectModel objectModel;
    protected Node cameraNode = new Node("CameraNode");
    protected Node sightPoint = new Node("LookAtPoint");
    protected double longitude = 90;
    protected double latitude = 15;
    protected double altitude = 1e5f;
    
    protected double lookDirectionDeg = 0;
    protected double lookAzimuthDeg = 0;
    
    FirstPersonMoving(ObjectModel objectModel) {
        this.objectModel = objectModel;

        Sphere box = new Sphere(32, 32, (float) (50 / objectModel.jmeApp.scale));
        Material boxMat = new Material(objectModel.jmeApp.getAssetManager(), 
                "Common/MatDefs/Misc/Unshaded.j3md");
        boxMat.setColor("Color", ColorRGBA.Yellow);
        Geometry boxGeo = new Geometry("Box", box);
        boxGeo.setMaterial(boxMat);
        
//        cameraNode.attachChild(boxGeo);
    }
    
    public void directionChange(double changeAmountDeg) {
        this.lookDirectionDeg += changeAmountDeg;
        System.out.println(this.lookDirectionDeg);
    }
    
    public void azimuthChange(double changeAmountDeg) {
        this.lookAzimuthDeg += changeAmountDeg;
    }
    
    public Vector3d getLookAtLonLatAltDelta() {
        // Convert degrees to radians for calculations
        double lookDirectionRad = Math.toRadians(lookDirectionDeg);
        double lookAzimuthRad = Math.toRadians(lookAzimuthDeg);
        
        double dx = Math.cos(lookDirectionRad);
        double dy = Math.sin(lookDirectionRad);
        double dz = Math.sin(lookAzimuthRad);
        
//        // Determine the Cartesian direction vector based on lookDirection and lookAzimuth
//        double dx = Math.cos(lookAzimuthRad) * Math.cos(lookDirectionRad);
//        double dy = Math.cos(lookAzimuthRad) * Math.sin(lookDirectionRad);
//        double dz = Math.sin(lookAzimuthRad);
        return new Vector3d(dx, dy, dz);
    }

    protected Vector3f getSightLocalPos3() {
        // Convert degrees to radians for calculations
        double lookDirectionRad = Math.toRadians(lookDirectionDeg);
        double lookAzimuthRad = Math.toRadians(lookAzimuthDeg);

        // Earth's radius (assuming for simplicity); for other celestial bodies, replace this with the correct radius
        double radius = altitude + 6371000.0;  // Altitude + Earth's radius in meters

        // Convert current longitude and latitude to radians
        double latRad = Math.toRadians(latitude);
        double lonRad = Math.toRadians(longitude);

        // Convert spherical coordinates (longitude, latitude, altitude) to Cartesian (x, y, z)
        double x = radius * Math.cos(latRad) * Math.cos(lonRad);
        double y = radius * Math.cos(latRad) * Math.sin(lonRad);
        double z = radius * Math.sin(latRad);

        // Determine the Cartesian direction vector based on lookDirection and lookAzimuth
        double dx = Math.cos(lookAzimuthRad) * Math.cos(lookDirectionRad);
        double dy = Math.cos(lookAzimuthRad) * Math.sin(lookDirectionRad);
        double dz = Math.sin(lookAzimuthRad);

        // Scale the direction vector by the desired distance (in this case, 1 unit)
        double distance = 1.0;
        x += dx * distance;
        y += dy * distance;
        z += dz * distance;

        // Convert back to spherical coordinates (longitude, latitude, altitude)
        double newRadius = Math.sqrt(x * x + y * y + z * z);  // Distance from the center
        double newLatitude = Math.asin(z / newRadius);        // Latitude in radians
        double newLongitude = Math.atan2(y, x);               // Longitude in radians
        double newAltitude = newRadius - 6371000.0;           // Subtract Earth's radius to get altitude

        // Convert latitude and longitude back to degrees
        newLatitude = Math.toDegrees(newLatitude);
        newLongitude = Math.toDegrees(newLongitude);

        // Return the computed look-at point in 3D space (longitude, latitude, altitude)
        return new Vector3f((float)newLongitude, (float)newLatitude, (float)newAltitude);
    }
    
    public Vector3d getCurrentLocalPos() {
        return objectModel.calculateSurfacePosition(latitude,
                longitude,
                altitude);
    }
    
    public Vector3f getSightLocalPos() {
//        return computeLookAtPosition();
        
        Vector3d delta = getLookAtLonLatAltDelta();
        double sightLat = latitude + delta.x;
        double sightLon = longitude + delta.y;
        double sightAlt = altitude + delta.z * altitude;
//        System.out.println(sightLat + " " + sightLon + " " + sightAlt);
        return objectModel.calculateSurfacePosition(sightLat,
                sightLon,
                sightAlt).toVector3f();
    }

//    public Vector3d getSightLocalPos() {
//        Vector3d pos = getCurrentLocalPos();
//        double lookDirectionRad = Math.toRadians(lookDirectionDeg);
//        double lookAzimuthRad = Math.toRadians(lookAzimuthDeg);
//
//        double dx = Math.cos(lookAzimuthRad) * Math.cos(lookDirectionRad) * 1e5;
//        double dy = Math.cos(lookAzimuthRad) * Math.sin(lookDirectionRad) * 1e5;
//        double dz = Math.sin(lookAzimuthRad) * 1e5;
//        
//        Vector3d delta = new Vector3d(dx, dy, dz);
//        return pos.add(delta);
//    }
}
