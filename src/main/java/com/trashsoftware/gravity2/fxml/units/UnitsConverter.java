package com.trashsoftware.gravity2.fxml.units;

import java.util.ResourceBundle;

public interface UnitsConverter {
    String time(double seconds);
    
    String dateTime(double seconds, ResourceBundle strings);
    
    String mass(double kg);
    
    String distance(double m);
    
    String area(double m2);
    
    String volume(double m3);
    
    String speed(double mPerS);
    
    String acceleration(double ms2);
    
    String energy(double joules);
    
    String temperature(double k);
}
