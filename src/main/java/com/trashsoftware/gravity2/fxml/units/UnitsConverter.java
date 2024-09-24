package com.trashsoftware.gravity2.fxml.units;

import java.util.ResourceBundle;

public interface UnitsConverter {
    String generalNumber(double x);
    
    String time(double seconds);
    
    String dateTime(double seconds, ResourceBundle strings);
    
    String mass(double kg);
    
    String distance(double m);
    
    String radius(double m);
    
    String area(double m2);
    
    String volume(double m3);
    
    String speed(double mPerS);
    
    String acceleration(double ms2);
    
    String energy(double joules);
    
    String temperature(double k);
    
    String luminosity(double watt);
    
    String angleDegreeDecimal(double deg);
    
    String angleDegreeMinuteSecond(double deg);
    
    default String longitude(double deg) {
        if (deg == 0) return angleDegreeMinuteSecond(deg);
        String suffix = " E";
        if (deg < 0) {
            deg = -deg;
            suffix = " W";
        }
        return angleDegreeMinuteSecond(deg) + suffix;
    }

    default String latitude(double deg) {
        if (deg == 0) return angleDegreeMinuteSecond(deg);
        String suffix = " N";
        if (deg < 0) {
            deg = -deg;
            suffix = " S";
        }
        return angleDegreeMinuteSecond(deg) + suffix;
    }
}
