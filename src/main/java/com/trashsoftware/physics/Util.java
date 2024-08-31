package com.trashsoftware.physics;

import com.jme3.math.ColorRGBA;
import org.json.JSONArray;

import java.util.Arrays;
import java.util.Random;

public class Util {

    public static String colorToHex(ColorRGBA color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);
        int alpha = (int) (color.getAlpha() * 255);

        // Format the color to a hex string
        if (alpha < 255) {
            return String.format("#%02X%02X%02X%02X", red, green, blue, alpha); // RGBA
        } else {
            return String.format("#%02X%02X%02X", red, green, blue); // RGB
        }
    }

    public static ColorRGBA stringToColor(String colorHex) {
        if (colorHex.length() > 1 && colorHex.charAt(0) == '#') {
            String code = colorHex.substring(1);
            int red = 0, green = 0, blue = 0;
            int alpha = 255;

            if (code.length() >= 2) {
                red = Integer.parseInt(code.substring(0, 2), 16);
            }
            if (code.length() >= 4) {
                green = Integer.parseInt(code.substring(2, 4), 16);
            }
            if (code.length() >= 6) {
                blue = Integer.parseInt(code.substring(4, 6), 16);
            }
            if (code.length() == 8) {
                alpha = Integer.parseInt(code.substring(6, 8), 16);
            }
            return ColorRGBA.fromRGBA255(red, green, blue, alpha);
        }
        throw new RuntimeException("Cannot convert '" + colorHex + "' to color");
    }

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
