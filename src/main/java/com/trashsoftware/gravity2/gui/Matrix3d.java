package com.trashsoftware.gravity2.gui;

public class Matrix3d {
    private double[][] m;

    public Matrix3d() {
        m = new double[3][3];
        identity();
    }

    // Initialize as identity matrix
    public void identity() {
        m[0][0] = 1; m[0][1] = 0; m[0][2] = 0;
        m[1][0] = 0; m[1][1] = 1; m[1][2] = 0;
        m[2][0] = 0; m[2][1] = 0; m[2][2] = 1;
    }

    // Create a rotation matrix from angle (in radians) around an axis
    public void fromAngleAxis(double angle, Vector3d axis) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double oneMinusCos = 1.0 - cos;

        double x = axis.x, y = axis.y, z = axis.z;

        m[0][0] = cos + x * x * oneMinusCos;
        m[0][1] = x * y * oneMinusCos - z * sin;
        m[0][2] = x * z * oneMinusCos + y * sin;

        m[1][0] = y * x * oneMinusCos + z * sin;
        m[1][1] = cos + y * y * oneMinusCos;
        m[1][2] = y * z * oneMinusCos - x * sin;

        m[2][0] = z * x * oneMinusCos - y * sin;
        m[2][1] = z * y * oneMinusCos + x * sin;
        m[2][2] = cos + z * z * oneMinusCos;
    }

    // Multiply this matrix with a vector
    public Vector3d mult(Vector3d vec) {
        double x = m[0][0] * vec.x + m[0][1] * vec.y + m[0][2] * vec.z;
        double y = m[1][0] * vec.x + m[1][1] * vec.y + m[1][2] * vec.z;
        double z = m[2][0] * vec.x + m[2][1] * vec.y + m[2][2] * vec.z;
        return new Vector3d(x, y, z);
    }

    @Override
    public String toString() {
        return "[[" + m[0][0] + ", " + m[0][1] + ", " + m[0][2] + "],\n" +
                " [" + m[1][0] + ", " + m[1][1] + ", " + m[1][2] + "],\n" +
                " [" + m[2][0] + ", " + m[2][1] + ", " + m[2][2] + "]]";
    }
}
