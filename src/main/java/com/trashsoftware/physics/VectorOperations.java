package com.trashsoftware.physics;

public class VectorOperations {
    public static double[] add(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    public static void addInPlace(double[] a, double[] b) {
        if (b.length > a.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        for (int i = 0; i < b.length; i++) {
            a[i] += b[i];
        }
    }

    public static double[] subtract(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] - b[i];
        }
        return result;
    }

    public static double[] scale(double[] v, double scalar) {
        double[] result = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            result[i] = v[i] * scalar;
        }
        return result;
    }

    public static double[] normalize(double[] vector) {
        double mag = magnitude(vector);
        
        double[] res = new double[vector.length];
        for (int i = 0; i < vector.length; i++) {
            res[i] = vector[i] / mag;
        }
        return res;
    }

    public static double distance(double[] posA, double[] posB) {
        if (posA.length != posB.length) {
            throw new IllegalArgumentException("Two position bust be in same dimension");
        }
        double sqrSum = 0;
        for (int d = 0; d < posA.length; d++) {
            double diff = posA[d] - posB[d];
            sqrSum += diff * diff;  // distance must be squared, no matter how
        }
        return Math.sqrt(sqrSum);
    }

    public static double magnitude(double[] v) {
        double sum = 0;
        for (double val : v) {
            sum += val * val;
        }
        return Math.sqrt(sum);
    }

    public static double[] crossProduct(double[] a, double[] b) {
        if (a.length < b.length) throw new ArithmeticException("Cannot cross product");
        
        if (a.length == 1) {
            return new double[]{a[0] * b[0]};
        } else if (a.length == 2) {
            if (b.length == 1) {
                return new double[]{a[0] * b[0] + a[1] * b[0]};
            } else if (b.length == 2) {
                return new double[]{a[0] * b[1] - a[1] * b[0]};
            }
        } else if (a.length == 3) {
            if (b.length == 3) {
                return new double[]{
                        a[1] * b[2] - a[2] * b[1],
                        a[2] * b[0] - a[0] * b[2],
                        a[0] * b[1] - a[1] * b[0]
                };
            }
        }

        throw new ArithmeticException("Cannot cross product");
    }

    public static double dotProduct(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i] * b[i];
        }
        return sum;
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
    
    public static double[] minMax(double[][] matrix) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] array : matrix) {
            for (double d : array) {
                if (d < min) min = d;
                if (d > max) max = d;
            }
        }
        return new double[]{min, max};
    }

    public static double[] positiveMinMax(double[][] matrix) {
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double[] array : matrix) {
            for (double d : array) {
                if (d != 0.0 && d < min) min = d;
                if (d > max) max = d;
            }
        }
        return new double[]{min, max};
    }
    
    public static double[] rotateVector2d(double[] vec, double deg) {
        // Convert degrees to radians
        double rad = Math.toRadians(deg);

        // Calculate the components of the rotated vector
        double cosTheta = Math.cos(rad);
        double sinTheta = Math.sin(rad);

        // Perform the rotation
        double xNew = vec[0] * cosTheta - vec[1] * sinTheta;
        double yNew = vec[0] * sinTheta + vec[1] * cosTheta;

        // Return the rotated vector
        return new double[]{xNew, yNew};
    }
    
    public static double[] reverseVector(double[] vec) {
        double[] res = new double[vec.length];
        for (int i = 0; i < vec.length; i++) {
            res[i] = -vec[i];
        }
        return res;
    }
}

