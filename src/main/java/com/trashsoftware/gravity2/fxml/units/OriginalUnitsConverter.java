package com.trashsoftware.gravity2.fxml.units;

import java.util.ResourceBundle;

public class OriginalUnitsConverter implements UnitsConverter {
    @Override
    public String time(double seconds) {
        return String.format("%6.3e s", seconds);
    }

    @Override
    public String dateTime(double seconds, ResourceBundle strings) {
        return "";
    }

    @Override
    public String mass(double kg) {
        return String.format("%6.3e kg", kg);
    }

    @Override
    public String distance(double m) {
        return String.format("%6.3e m", m);
    }

    @Override
    public String area(double m2) {
        return String.format("%6.3e m²", m2);
    }

    @Override
    public String volume(double m3) {
        return String.format("%6.3e m³", m3);
    }

    @Override
    public String speed(double mPerS) {
        return String.format("%6.3e m/s", mPerS);
    }

    @Override
    public String acceleration(double ms2) {
        return String.format("%6.3e m/s²", ms2);
    }

    @Override
    public String energy(double joules) {
        return String.format("%6.3e J", joules);
    }

    @Override
    public String temperature(double k) {
        return String.format("%6.3eK", k);
    }

    @Override
    public String angleDegreeDecimal(double deg) {
        return String.format("%.2f°", deg);
    }

    @Override
    public String angleDegreeMinuteSecond(double deg) {
        return String.format("%.4f°", deg);
    }
}
