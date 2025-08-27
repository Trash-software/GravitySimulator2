package com.trashsoftware.gravity2.physics.bhTree;

import com.trashsoftware.gravity2.physics.CelestialObject;

import java.util.Arrays;
import java.util.List;

public class BoundingBox {
    double[] center; // center of cube
    double size;     // length of the cube’s edge

    public BoundingBox(double[] center, double size) {
        this.center = center;
        this.size = size;
    }

    // Compute a bounding box that contains all the objects
    public static BoundingBox computeBoundingBox(List<CelestialObject> objects) {
        double[] min = new double[3];
        double[] max = new double[3];
        Arrays.fill(min, Double.POSITIVE_INFINITY);
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        for (CelestialObject obj : objects) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], obj.getPosition()[i]);
                max[i] = Math.max(max[i], obj.getPosition()[i]);
            }
        }

        double[] center = new double[3];
        double maxRange = 0.0;
        for (int i = 0; i < 3; i++) {
            center[i] = (min[i] + max[i]) / 2.0;
            maxRange = Math.max(maxRange, max[i] - min[i]);
        }

        return new BoundingBox(center, maxRange * 1.1); // add margin
    }

    // Check if a position is within this box
    boolean contains(double[] pos) {
        for (int i = 0; i < 3; i++) {
            if (Math.abs(pos[i] - center[i]) > size / 2) return false;
        }
        return true;
    }

    // Get child bounding box
    BoundingBox getOctant(int index) {
        double offset = size / 4;
        double[] newCenter = new double[3];
        for (int i = 0; i < 3; i++) {
            int bit = (index >> i) & 1;
            newCenter[i] = center[i] + (bit == 0 ? -offset : offset);
        }
        return new BoundingBox(newCenter, size / 2);
    }
}
