package com.trashsoftware.gravity2.gui;

import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import javafx.scene.paint.Color;

public class GuiUtils {

    public static String fxColorToHex(Color color) {
        int red = (int) (color.getRed() * 255);
        int green = (int) (color.getGreen() * 255);
        int blue = (int) (color.getBlue() * 255);

        // Format the color to a hex string
        return String.format("#%02X%02X%02X", red, green, blue); // RGB
    }

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

    public static ColorRGBA opaqueOf(ColorRGBA color, float opaque) {
        return new ColorRGBA(color.r, color.g, color.b, opaque);
    }

    public static ColorRGBA interpolate(ColorRGBA beginValue, ColorRGBA endValue, double t) {
        if (t <= 0.0) return beginValue;
        if (t >= 1.0) return endValue;
        float ft = (float) t;
        return new ColorRGBA(
                beginValue.r + (endValue.r - beginValue.r) * ft,
                beginValue.g + (endValue.g - beginValue.g) * ft,
                beginValue.b + (endValue.b - beginValue.b) * ft,
                beginValue.a + (endValue.a - beginValue.a) * ft
        );
    }

    public static ColorRGBA deriveColor(ColorRGBA color,
                                        float hueShift, float saturationFactor,
                                        float brightnessFactor, float opacityFactor) {

        double[] hsb = RGBtoHSB(color.r, color.g, color.b);

        /* Allow brightness increase of black color */
        double b = hsb[2];
        if (b == 0 && brightnessFactor > 1.0) {
            b = 0.05;
        }

        /* the tail "+ 360) % 360" solves shifts into negative numbers */
        double h = (((hsb[0] + hueShift) % 360) + 360) % 360;
        double s = Math.max(Math.min(hsb[1] * saturationFactor, 1.0), 0.0);
        b = Math.max(Math.min(b * brightnessFactor, 1.0), 0.0);
        double a = Math.max(Math.min(color.a * opacityFactor, 1.0), 0.0);
        return hsb(h, s, b, a);
    }

    public static ColorRGBA hsb(double hue, double saturation, double brightness, double opacity) {
        checkSB(saturation, brightness);
        double[] rgb = HSBtoRGB(hue, saturation, brightness);
        ColorRGBA result = new ColorRGBA((float) rgb[0], (float) rgb[1], (float) rgb[2], (float) opacity);
        return result;
    }

    private static void checkSB(double saturation, double brightness) {
        if (saturation < 0.0 || saturation > 1.0) {
            throw new IllegalArgumentException("Color.hsb's saturation parameter (" + saturation + ") expects values 0.0-1.0");
        }
        if (brightness < 0.0 || brightness > 1.0) {
            throw new IllegalArgumentException("Color.hsb's brightness parameter (" + brightness + ") expects values 0.0-1.0");
        }
    }

    public static double[] RGBtoHSB(double r, double g, double b) {
        double hue, saturation, brightness;
        double[] hsbvals = new double[3];
        double cmax = (r > g) ? r : g;
        if (b > cmax) cmax = b;
        double cmin = (r < g) ? r : g;
        if (b < cmin) cmin = b;

        brightness = cmax;
        if (cmax != 0)
            saturation = (cmax - cmin) / cmax;
        else
            saturation = 0;

        if (saturation == 0) {
            hue = 0;
        } else {
            double redc = (cmax - r) / (cmax - cmin);
            double greenc = (cmax - g) / (cmax - cmin);
            double bluec = (cmax - b) / (cmax - cmin);
            if (r == cmax)
                hue = bluec - greenc;
            else if (g == cmax)
                hue = 2.0 + redc - bluec;
            else
                hue = 4.0 + greenc - redc;
            hue = hue / 6.0;
            if (hue < 0)
                hue = hue + 1.0;
        }
        hsbvals[0] = hue * 360;
        hsbvals[1] = saturation;
        hsbvals[2] = brightness;
        return hsbvals;
    }

    public static double[] HSBtoRGB(double hue, double saturation, double brightness) {
        // normalize the hue
        double normalizedHue = ((hue % 360) + 360) % 360;
        hue = normalizedHue / 360;

        double r = 0, g = 0, b = 0;
        if (saturation == 0) {
            r = g = b = brightness;
        } else {
            double h = (hue - Math.floor(hue)) * 6.0;
            double f = h - Math.floor(h);
            double p = brightness * (1.0 - saturation);
            double q = brightness * (1.0 - saturation * f);
            double t = brightness * (1.0 - (saturation * (1.0 - f)));
            switch ((int) h) {
                case 0:
                    r = brightness;
                    g = t;
                    b = p;
                    break;
                case 1:
                    r = q;
                    g = brightness;
                    b = p;
                    break;
                case 2:
                    r = p;
                    g = brightness;
                    b = t;
                    break;
                case 3:
                    r = p;
                    g = q;
                    b = brightness;
                    break;
                case 4:
                    r = t;
                    g = p;
                    b = brightness;
                    break;
                case 5:
                    r = brightness;
                    g = p;
                    b = q;
                    break;
            }
        }
        return new double[]{r, g, b};
    }

    public static Vector3f fromDoubleArray(double[] doubles) {
        if (doubles.length != 3) throw new IllegalArgumentException();
        return new Vector3f((float) doubles[0], (float) doubles[1], (float) doubles[2]);
    }

    public static double[] toDoubleArray(Vector3f vector3f) {
        return new double[]{vector3f.x, vector3f.y, vector3f.z};
    }
    
    public static String temperatureToRGBString(double temperature) {
        int[] rgb = temperatureToRGB((int) temperature);
        return String.format("#%02X%02X%02X", rgb[0], rgb[1], rgb[2]); // RGB
    }

    /**
     * Converts a given temperature (in Kelvin) to RGB values.
     * @param temperature Temperature in Kelvin
     * @return An array of integers representing the RGB values [R, G, B]
     */
    public static int[] temperatureToRGB(int temperature) {
        // Clamp the temperature between 1000K and 40000K
        temperature = Math.max(1000, Math.min(temperature, 40000));

        // Convert temperature to RGB values
        double t = temperature / 100.0;
        int r, g, b;

        // Calculate red
        if (t <= 66) {
            r = 255;
        } else {
            r = (int) (329.698727446 * Math.pow(t - 60, -0.1332047592));
            r = clamp(r, 0, 255);
        }

        // Calculate green with constraints to avoid unrealistic green colors
        if (t <= 66) {
            g = (int) (99.4708025861 * Math.log(t) - 161.1195681661);
            g = clamp(g, 0, 255);
        } else {
            // Ensure green component smoothly reduces at higher temperatures, mimicking the transition
            g = (int) (288.1221695283 * Math.pow(t - 60, -0.0755148492) * 0.7);
            g = clamp(g, 0, 255);
        }

        // Calculate blue
        if (t >= 66) {
            b = 255;
        } else if (t <= 19) {
            b = 0;
        } else {
            b = (int) (138.5177312231 * Math.log(t - 10) - 305.0447927307);
            b = clamp(b, 0, 255);
        }

        return new int[]{r, g, b};
    }

    /**
     * Clamps a value between a minimum and maximum.
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static Color temperatureToColorHSB(double temperature) {
        // Normalize temperature to a reasonable stellar range (1000K to 40000K)
        temperature = Math.max(1000, Math.min(40000, temperature));

        // Calculate the hue based on temperature
        float hue;
        if (temperature <= 4000) {
            // Cool stars (reddish): 0° to 30° on the hue scale
            hue = 0f + (float) ((temperature - 1000) / 3000.0) * 30f;
        } else if (temperature <= 7000) {
            // Mid-range stars (yellow-white): 30° to 60° on the hue scale
            hue = 30f + (float) ((temperature - 4000) / 3000.0) * 30f;
        } else {
            // Hot stars (white to blue): 60° to 240° on the hue scale
            hue = 60f + (float) ((temperature - 7000) / 33000.0) * 180f;
        }

        // Adjusted saturation for Sun-like stars
        float saturation = getSaturation(temperature);

        // Brightness is kept at max for simplicity
        float brightness = 1.0f;

        return Color.hsb(hue, saturation, brightness);
    }

    // Function for smooth saturation based on temperature
    public static float getSaturation(double temperature) {
        // Adjusted saturation for Sun-like stars
        float saturation;
        if (temperature <= 3000) {
            saturation = 1.0f;
        } else if (temperature <= 4000) {
            // Cool stars are highly saturated
            saturation = 0.4f + (float) (4000 - temperature) / 1000.0f * 0.6f;
        } else if (temperature <= 7000) {
            // Lower saturation for stars like the Sun (5770K)
            saturation = 0.2f + (float) (7000 - temperature) / 3000.0f * 0.2f;  // range 0.2 to 0.4
        } else {
            // Hot stars (white to blue) are less saturated
            saturation = 0.2f + (float) (40000 - temperature) / 33000.0f * 0.2f;  // range 0.2 to 0.3
        }
        return saturation;
    }

    public static ColorRGBA fxColorToJmeColor(Color fxColor) {
        return new ColorRGBA(
                (float) fxColor.getRed(),
                (float) fxColor.getGreen(),
                (float) fxColor.getBlue(),
                (float) fxColor.getOpacity()
        );
    }

}

