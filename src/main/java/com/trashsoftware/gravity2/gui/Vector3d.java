package com.trashsoftware.gravity2.gui;

import com.jme3.math.Vector3f;

public class Vector3d {
    
    public double x, y, z;
    
    public Vector3d() {
        
    }
    
    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Vector3d addLocal(Vector3f vector3f) {
        x += vector3f.x;
        y += vector3f.y;
        z += vector3f.z;
        return this;
    }

    public Vector3d multLocal(double scale) {
        x *= scale;
        y *= scale;
        z *= scale;
        return this;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    @Override
    public String toString() {
        return "Vector3d{%f, %f, %f}".formatted(x, y, z);
    }
}
