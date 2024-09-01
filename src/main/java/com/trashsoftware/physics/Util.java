package com.trashsoftware.physics;

import com.jme3.math.ColorRGBA;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Random;

public class Util {

    public static double[] jsonArrayToDoubleArray(JSONArray jsonArray) {
        double[] res = new double[jsonArray.length()];
        for (int i = 0; i < res.length; i++) {
            res[i] = jsonArray.getDouble(i);
        }
        return res;
    }

    /**
     * Map a value from domain to range linearly
     */
    public static double linearMapping(double domainLow, double domainHigh,
                                       double rangeLow, double rangeHigh,
                                       double value) {
        if (value < domainLow) return rangeLow;
        if (value > domainHigh) return rangeHigh;
        double ratio = (value - domainLow) / (domainHigh - domainLow);
        return rangeLow + (rangeHigh - rangeLow) * ratio;
    }

    public static boolean containsNaN(double[] values) {
        return Arrays.stream(values).anyMatch(Double::isNaN);
    }

    public static boolean containsNaN(double[][] values) {
        return Arrays.stream(values).anyMatch(Util::containsNaN);
    }

//    public static StarModel cloneShape(StarModel original, double radius) {
//        StarModel newOne = new StarModel(radius);
//        Material material = original.getMaterial();
//        newOne.setMaterial(material);
////        for (Transform t : original.getTransforms()) {
////            if (t instanceof Rotate) {
////                newOne.getTransforms().add(t);
////            }
////        }
//
//        return newOne;
//    }

//    public static Sphere cloneShape(Sphere original, double radius) {
//        Sphere newOne = new Sphere(radius);
//        Material material = original.getMaterial();
//        newOne.setMaterial(material);
////        for (Transform t : original.getTransforms()) {
////            if (t instanceof Rotate) {
////                newOne.getTransforms().add(t);
////            }
////        }
//        
//        return newOne;
//    }

    public static String randomColorCode() {
        Random random = new Random();
        return String.format("#%02X%02X%02X",
                random.nextInt(256),
                random.nextInt(256),
                random.nextInt(256)); // RGB
    }
}
