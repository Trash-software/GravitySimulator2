package com.trashsoftware.gravity2.gui;

import com.jme3.math.Vector3f;

public class Vector3d {

    public static final Vector3d ZERO = new Vector3d(0, 0, 0);
    public static final Vector3d UNIT_X = new Vector3d(1, 0, 0);
    public static final Vector3d UNIT_Y = new Vector3d(0, 1, 0);
    public static final Vector3d UNIT_Z = new Vector3d(0, 0, 1);
    public double x, y, z;

    public Vector3d() {

    }

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public static Vector3d fromArray(double[] array) {
        if (array.length != 3) {
            throw new IllegalArgumentException("Array length must be 3, got " + array.length);
        }
        return new Vector3d(array[0], array[1], array[2]);
    }
    
    public double[] toArray() {
        return new double[]{x, y, z};
    }

    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void set(double[] array) {
        set(array[0], array[1], array[2]);
    }
    
    public void set(Vector3d another) {
        set(another.x, another.y, another.z);
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

    public Vector3d subtractLocal(Vector3f vector3f) {
        x -= vector3f.x;
        y -= vector3f.y;
        z -= vector3f.z;
        return this;
    }

    public Vector3d subtract(Vector3d other) {
        return new Vector3d(
                x - other.x,
                y - other.y,
                z - other.z
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

    public static Vector3d fromVector3f(Vector3f v3f) {
        return new Vector3d(v3f.x, v3f.y, v3f.z);
    }

    @Override
    public String toString() {
        return "Vector3d{%f, %f, %f}".formatted(x, y, z);
    }
}
