package com.trashsoftware.gravity2.utils;

import org.ejml.simple.SimpleMatrix;
import org.ejml.simple.SimpleSVD;

public class ConicCalculator {

    private double[] centroid;
    private double[] planeNormal;
    private double[] abcdef;
    private EllipseParams ellipseParams;

    public ConicCalculator(double[][] points3D) {
        centroid = computeCentroid(points3D);
        planeNormal = findPlaneNormal(points3D);
        // 2. Project the points onto the plane
        double[][] projectedPoints = new double[5][];
        for (int i = 0; i < points3D.length; i++) {
            projectedPoints[i] = projectPointOntoPlane(points3D[i], centroid, planeNormal);
        }

        // 3. Compute local 2D axes
        double[][] axes = computeLocalAxes(planeNormal);

        // 4. Convert the 3D projected points into 2D coordinates
        double[][] points2D = new double[points3D.length][];
        for (int i = 0; i < projectedPoints.length; i++) {
            points2D[i] = projectPointTo2D(projectedPoints[i], centroid, axes);
        }

        abcdef = fitEllipse(points2D);
        ellipseParams = calculateEllipseParams(abcdef[0], abcdef[1], abcdef[2], abcdef[3], abcdef[4], abcdef[5]);
    }

    private double[] computeCentroid(double[][] points) {
        double[] centroid = new double[3];
        for (double[] point : points) {
            centroid[0] += point[0];
            centroid[1] += point[1];
            centroid[2] += point[2];
        }
        centroid[0] /= points.length;
        centroid[1] /= points.length;
        centroid[2] /= points.length;
        return centroid;
    }

    private double[] findPlaneNormal(double[][] points) {
        SimpleMatrix centeredMatrix = new SimpleMatrix(points.length, 3);

        // Center the points by subtracting the centroid
        for (int i = 0; i < points.length; i++) {
            centeredMatrix.set(i, 0, points[i][0] - centroid[0]);
            centeredMatrix.set(i, 1, points[i][1] - centroid[1]);
            centeredMatrix.set(i, 2, points[i][2] - centroid[2]);
        }

        // Compute the covariance matrix
        SimpleMatrix covarianceMatrix = centeredMatrix.transpose().mult(centeredMatrix);

        // Perform Singular Value Decomposition (SVD) to get the eigenvalues and eigenvectors
        SimpleSVD<SimpleMatrix> svd = covarianceMatrix.svd();
        SimpleMatrix eigenvectors = svd.getV(); // The eigenvectors

        // The eigenvector corresponding to the smallest eigenvalue is the plane's normal
        double[] normal = new double[3];
        normal[0] = eigenvectors.get(0, 2);
        normal[1] = eigenvectors.get(1, 2);
        normal[2] = eigenvectors.get(2, 2);

        return normal; // This is the normal vector of the best-fit plane
    }

    private double[] projectPointOntoPlane(double[] point, double[] centroid, double[] normal) {
        double[] pointToCentroid = new double[3];
        for (int i = 0; i < 3; i++) {
            pointToCentroid[i] = point[i] - centroid[i];
        }

        // Dot product (P - C) · N
        double dotProduct = 0;
        for (int i = 0; i < 3; i++) {
            dotProduct += pointToCentroid[i] * normal[i];
        }

        // N · N (normal vector length squared)
        double normalLengthSquared = 0;
        for (int i = 0; i < 3; i++) {
            normalLengthSquared += normal[i] * normal[i];
        }

        // Projection formula
        double[] projection = new double[3];
        for (int i = 0; i < 3; i++) {
            projection[i] = point[i] - (dotProduct / normalLengthSquared) * normal[i];
        }

        return projection;
    }

    private double[][] computeLocalAxes(double[] normal) {
        // Find two vectors perpendicular to the normal to act as X and Y axes in the plane
        double[] xAxis = new double[]{1, 0, 0};
        if (Math.abs(normal[0]) > Math.abs(normal[1])) {
            xAxis = new double[]{0, 1, 0};
        }

        // Compute the cross-product to get an axis perpendicular to both the normal and xAxis
        double[] yAxis = new double[3];
        yAxis[0] = normal[1] * xAxis[2] - normal[2] * xAxis[1];
        yAxis[1] = normal[2] * xAxis[0] - normal[0] * xAxis[2];
        yAxis[2] = normal[0] * xAxis[1] - normal[1] * xAxis[0];

        // Now xAxis and yAxis are orthogonal and lie in the plane
        return new double[][]{xAxis, yAxis};
    }

    private double[] projectPointTo2D(double[] point3D, double[] centroid, double[][] axes) {
        double[] localPoint = new double[2];

        for (int i = 0; i < 3; i++) {
            localPoint[0] += (point3D[i] - centroid[i]) * axes[0][i]; // Project onto X axis
            localPoint[1] += (point3D[i] - centroid[i]) * axes[1][i]; // Project onto Y axis
        }

        return localPoint;
    }

    private double[] fitEllipse(double[][] points2d) {
        // Construct the design matrix X for the ellipse equation
        SimpleMatrix X = new SimpleMatrix(5, 6); // 5 rows, 6 columns for A, B, C, D, E, F

        for (int i = 0; i < 5; i++) {
            double x = points2d[i][0];
            double y = points2d[i][1];

            X.set(i, 0, x * x); // A coefficient
            X.set(i, 1, x * y); // B coefficient
            X.set(i, 2, y * y); // C coefficient
            X.set(i, 3, x);     // D coefficient
            X.set(i, 4, y);     // E coefficient
            X.set(i, 5, 1.0);   // F constant term
        }

        // Right-hand side is zero, but we use least squares to find the best fit
        SimpleMatrix Y = new SimpleMatrix(5, 1); // Zero vector for homogeneous solution

        // Solve using least-squares (X^T * X) * solution = X^T * Y
        SimpleMatrix Xt = X.transpose();
        SimpleMatrix XtX = Xt.mult(X);
        SimpleMatrix XtY = Xt.mult(Y);

        // Solve the system using least squares (pinv)
        SimpleMatrix solution = XtX.pseudoInverse().mult(XtY);

        // Return the ellipse coefficients [A, B, C, D, E, F]
        return solution.getDDRM().getData();
    }

    public double[] getPlaneNormal() {
        return planeNormal;
    }

    public double[] getAbcdef() {
        return abcdef;
    }

    public EllipseParams getEllipseParams() {
        return ellipseParams;
    }

    // Method to calculate ellipse parameters from A, B, C, D, E, F
    private EllipseParams calculateEllipseParams(double A, double B, double C, double D, double E, double F) {
        // Discriminant check to confirm if this is an ellipse
        double discriminant = B * B - 4 * A * C;
//        if (discriminant >= 0) {
//            return null;
////            throw new IllegalArgumentException("Not an ellipse, discriminant must be negative.");
//        }

        // Find the center of the ellipse (x0, y0)
        double x0 = (2 * C * D - B * E) / discriminant;
        double y0 = (2 * A * E - B * D) / discriminant;

        // Calculate rotation angle theta
        double theta = 0.5 * Math.atan2(B, A - C);

        // Rotation matrix for transforming coordinates
        SimpleMatrix R = new SimpleMatrix(2, 2, true,
                Math.cos(theta), -Math.sin(theta),
                Math.sin(theta), Math.cos(theta));

        // Form the quadratic form matrix M
        SimpleMatrix M = new SimpleMatrix(2, 2, true,
                A, B / 2,
                B / 2, C);

        // Rotate M to eliminate the cross term xy
        SimpleMatrix Mprime = R.transpose().mult(M).mult(R);

        // Extract the new coefficients A' and C' after rotation
        double A_prime = Mprime.get(0, 0);
        double C_prime = Mprime.get(1, 1);

        // Calculate semi-major and semi-minor axes
        double F_adjusted = F + A * x0 * x0 + B * x0 * y0 + C * y0 * y0 - D * x0 - E * y0;
        double a = Math.sqrt(-F_adjusted / A_prime);
        double b = Math.sqrt(-F_adjusted / C_prime);

        return new EllipseParams(x0, y0, theta, a, b);
    }

    public static void main(String[] args) {
        ConicCalculator cc = new ConicCalculator(new double[][]{});
    }

    // Class to store ellipse parameters
    public static class EllipseParams {
        public final double x0, y0, theta, a, b;

        public EllipseParams(double x0, double y0, double theta, double a, double b) {
            this.x0 = x0;
            this.y0 = y0;
            this.theta = theta;
            this.a = a;
            this.b = b;
        }
    }
}
