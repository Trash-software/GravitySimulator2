package com.trashsoftware.gravity2.fxml.units;

import java.util.Map;
import java.util.ResourceBundle;

public class AdaptiveUnitsConverter implements UnitsConverter {
    @Override
    public String time(double seconds) {
        return UnitsUtil.adaptiveTime(seconds);
    }

    @Override
    public String dateTime(double seconds, ResourceBundle strings) {
        return UnitsUtil.adaptiveDateTime(seconds, strings);
    }

    @Override
    public String mass(double kg) {
        return UnitsUtil.adaptiveMass(kg);
    }

    @Override
    public String distance(double m) {
        return UnitsUtil.adaptiveDistance(m);
    }

    @Override
    public String area(double m2) {
        return "";
    }

    @Override
    public String volume(double m3) {
        return UnitsUtil.adaptiveVolume(m3);
    }

    @Override
    public String speed(double mPerS) {
        return UnitsUtil.adaptiveSpeed(mPerS);
    }

    @Override
    public String acceleration(double ms2) {
        return UnitsUtil.adaptiveAcceleration(ms2);
    }

    @Override
    public String energy(double joules) {
        return UnitsUtil.adaptiveEnergy(joules);
    }

    @Override
    public String temperature(double k) {
        if (k < 1e4) {
            return UnitsUtil.stdFmt.format(k) + "K";
        } else if (k < 1e7) {
            return UnitsUtil.stdFmt.format(k / 1e3) + "k K";
        } else if (k < 1e10) {
            return UnitsUtil.stdFmt.format(k / 1e6) + "M K";
        } else {
            return UnitsUtil.sciFmt.format(k) + " K";
        }
    }

    @Override
    public String angleDegreeDecimal(double deg) {
        return UnitsUtil.stdFmt.format(deg) + "°";
    }

    @Override
    public String angleDegreeMinuteSecond(double deg) {
        int d = (int) deg;
        int seconds = (int) Math.round((deg - d) * 3600);
        int min = seconds / 60;
        int sec = seconds % 60;
//        return String.format("%d°%d′%d″", d, min, sec);
        return String.format("%d°%d'%d\"", d, min, sec);
    }
}
