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

    public Vector3d add(Vector3d other) {
        return new Vector3d(
                x + other.x,
                y + other.y,
                z + other.z
        );
    }

    public Vector3d multLocal(double scale) {
        x *= scale;
        y *= scale;
        z *= scale;
        return this;
    }

    public Vector3d mult(double scale) {
        return new Vector3d(
                x * scale,
                y * scale,
                z * scale
        );
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

    /**
     * Normalizes the vector to length=1 and returns the (modified) current
     * instance. If the vector has length=0, it's unchanged.
     *
     * @return the (modified) current instance (for chaining)
     */
    public Vector3d normalizeLocal() {
        // NOTE: this implementation is more optimized
        // than the old jme normalize as this method
        // is commonly used.
        double length = x * x + y * y + z * z;
        if (length != 1f && length != 0f) {
            length = 1.0f / Math.sqrt(length);
            x *= length;
            y *= length;
            z *= length;
        }
        return this;
    }

    public Vector3f toVector3f() {
        return new Vector3f(
                (float) x,
                (float) y,
                (float) z
        );
    }

    @Override
    public String toString() {
        return "Vector3d{%f, %f, %f}".formatted(x, y, z);
    }
}
