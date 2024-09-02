package com.trashsoftware.gravity2.gui;

import com.jme3.math.Matrix3f;
import com.jme3.math.Vector3f;

public class UiVectorUtils {
    public static Vector3f rotateAroundZAxis(Vector3f point, float angle) {
        Matrix3f rotationMatrix = new Matrix3f();
        rotationMatrix.fromAngleAxis(angle, Vector3f.UNIT_Z);
        return rotationMatrix.mult(point);
    }

    public static Vector3f rotateAroundXAxis(Vector3f point, float angle) {
        Matrix3f rotationMatrix = new Matrix3f();
        rotationMatrix.fromAngleAxis(angle, Vector3f.UNIT_X);
        return rotationMatrix.mult(point);
    }

    public static Vector3f rotateAroundYAxis(Vector3f point, float angle) {
        Matrix3f rotationMatrix = new Matrix3f();
        rotationMatrix.fromAngleAxis(angle, Vector3f.UNIT_Y);
        return rotationMatrix.mult(point);
    }
}
