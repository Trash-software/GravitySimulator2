package com.trashsoftware.gravity2.utils;

import com.trashsoftware.gravity2.gui.GuiUtils;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.Random;

public class Util {
    
    public static final double TWO_PI = Math.PI * 2;
    public static final double HALF_PI = Math.PI / 2;

    public static String darker(String rgbaCode, double extent) {
        if (extent < 0 || extent > 1) {
            throw new IllegalArgumentException("Extent must be between 0 and 1");
        }

        // Remove the '#' from the beginning of the hex string
        String hex = rgbaCode.startsWith("#") ? rgbaCode.substring(1) : rgbaCode;

        // Check if it's an RGBA color (8 characters long)
        boolean isRgba = hex.length() == 8;

        // Parse the hex values for red, green, and blue
        int red = Integer.parseInt(hex.substring(0, 2), 16);
        int green = Integer.parseInt(hex.substring(2, 4), 16);
        int blue = Integer.parseInt(hex.substring(4, 6), 16);

        // Darken the RGB values by reducing each component by the extent factor
        red = (int) Math.max(0, red * (1 - extent));
        green = (int) Math.max(0, green * (1 - extent));
        blue = (int) Math.max(0, blue * (1 - extent));

        // If it's an RGBA string, preserve the alpha value
        String result;
        if (isRgba) {
            int alpha = Integer.parseInt(hex.substring(6, 8), 16);  // Get the alpha value
            result = String.format("#%02X%02X%02X%02X", red, green, blue, alpha);  // Keep alpha unchanged
        } else {
            result = String.format("#%02X%02X%02X", red, green, blue);  // Regular RGB
        }

        return result;
    }

    public static double clamp(double input, double min, double max) {
        return (input < min) ? min : Math.min(input, max);
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

    public static String randomCelestialColorCode() {
        Random random = new Random();
        double h = random.nextDouble(360);
        double s = random.nextDouble(0, 0.7);
        double b = random.nextDouble(0.01, 0.99);
        Color hsb = Color.hsb(h, s, b);
        return GuiUtils.colorToHex(GuiUtils.fxColorToJmeColor(hsb));
    }
}
