package com.trashsoftware.gravity2.fxml.units;

import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.ResourceBundle;

public class UnitsUtil {

    private static final double MOON_VOLUME = 2.1958e10; // in km³
    private static final double EARTH_VOLUME = 1.08321e12; // in km³
    private static final double JUPITER_VOLUME = 1.43128e15; // in km³
    private static final double SUN_VOLUME = 1.412e18; // in km³

    private static final LocalDate EPOCH = Instant.EPOCH.atZone(ZoneOffset.UTC).toLocalDate();

    public static final DecimalFormat sciFmt = new DecimalFormat("0.###E0");
    public static final DecimalFormat stdFmt = new DecimalFormat("0.###");
    public static final DecimalFormat shortFmt = new DecimalFormat("0.#");

    public static String adaptiveMass(double kg) {
        if (kg < 1) {
            return stdFmt.format(kg * 1000) + " g";
        } else if (kg < 1000) {
            return stdFmt.format(kg) + " kg";
        } else if (kg < 1000_000) {
            return stdFmt.format(kg / 1000) + " t";
        }
        double moon = 7.342e22;
        double earth = 5.972e24;
        double jupiter = 1.899e27;
        double sun = 1.989e30;
        if (kg < moon * 0.01) {
            return sciFmt.format(kg) + " kg";
        } else if (kg < earth * 0.1) {
            return stdFmt.format(kg / moon) + " Moons";
        } else if (kg < jupiter * 0.1) {
            return stdFmt.format(kg / earth) + " Earths";
        } else if (kg < sun * 0.1) {
            return stdFmt.format(kg / jupiter) + " Jupiters";
        } else if (kg < sun * 1000) {
            return stdFmt.format(kg / sun) + " Suns";
        } else {
            return sciFmt.format(kg / sun) + " Suns";
        }
    }

    public static String adaptiveDistance(double m) {
        int sign = 1;
        if (m < 0) {
            sign = -1;
            m = -m;
        }
        
        String s = adaptiveDistancePositive(m);
        if (sign == -1) s = "-" + s;
        return s;
    }
    
    private static String adaptiveDistancePositive(double m) {
        if (m < 1) {
            return stdFmt.format(m * 1e3) + " mm";
        } else if (m < 1e3) {
            return shortFmt.format(m) + " m";
        } else if (m < 1e9) {
            return shortFmt.format(m / 1e3) + " km";
        }
//        else if (m < 1e12) {
//            return shortFmt.format(m / 1e6) + "k km";
//        } 
        else if (m < 1e12) {
            return shortFmt.format(m / 1e9) + "M km";
        }
        double km = m / 1000;
        double sun = 696340 * 2;
        double au = 149_598_262;
        double ly = 9_460_730_472_580.8;
        if (km < sun * 1.5) {
            return stdFmt.format(km / sun) + " Suns";
        } else if (km < au * 1000) {
            return stdFmt.format(km / au) + " AU";
        } else if (km < ly * 0.1) {
            return sciFmt.format(km / au) + " AU";
        } else if (km < ly * 1000) {
            return stdFmt.format(km / ly) + " ly";
        } else {
            return sciFmt.format(km / ly) + " ly";
        }
    }

    public static String adaptiveVolume(double volumeInCubicMeters) {
        double volumeInCubicKilometers = volumeInCubicMeters * 1e-9; // Convert m³ to km³

        if (volumeInCubicMeters < 1e-6) {
            return stdFmt.format(volumeInCubicMeters * 1e9) + " mm³";
        } else if (volumeInCubicMeters < 1e-3) {
            return stdFmt.format(volumeInCubicMeters * 1e6) + " cm³";
        } else if (volumeInCubicMeters < 1) {
            return stdFmt.format(volumeInCubicMeters * 1e3) + " dm³";
        } else if (volumeInCubicMeters < 1e7) {
            return stdFmt.format(volumeInCubicMeters) + " m³";
        } else if (volumeInCubicKilometers < MOON_VOLUME * 0.1) {
            return volumeInCubicKilometers < 1000
                    ? stdFmt.format(volumeInCubicKilometers) + " km³"
                    : sciFmt.format(volumeInCubicKilometers) + " km³";
        } else if (volumeInCubicKilometers < EARTH_VOLUME * 0.1) {
            double moons = volumeInCubicKilometers / MOON_VOLUME;
            return moons < 1000
                    ? stdFmt.format(moons) + " Moons"
                    : sciFmt.format(moons) + " Moons";
        } else if (volumeInCubicKilometers < JUPITER_VOLUME * 0.1) {
            double earths = volumeInCubicKilometers / EARTH_VOLUME;
            return earths < 1000
                    ? stdFmt.format(earths) + " Earths"
                    : sciFmt.format(earths) + " Earths";
        } else if (volumeInCubicKilometers < SUN_VOLUME * 0.1) {
            double jupiters = volumeInCubicKilometers / JUPITER_VOLUME;
            return jupiters < 1000
                    ? stdFmt.format(jupiters) + " Jupiters"
                    : sciFmt.format(jupiters) + " Jupiters";
        } else {
            double suns = volumeInCubicKilometers / SUN_VOLUME;
            return suns < 1000
                    ? stdFmt.format(suns) + " Suns"
                    : sciFmt.format(suns) + " Suns";
        }
    }

    public static String adaptiveSpeed(double ms) {
        String dt = adaptiveDistance(ms);
        return dt + "/s";
//        if (ms < 1e3) {
//            return stdFmt.format(ms) + " m/s";
//        } else if (ms < 1e6) {
//            return stdFmt.format(ms / 1e3) + " km/s";
//        } else {
//            return sciFmt.format(ms / 1e3) + " km/s";
//        }
    }

    public static String adaptiveAcceleration(double ms2) {
        if (ms2 < 1e-5) {
            return sciFmt.format(ms2) + " m/s²";
        } else if (ms2 < 1) {
            return stdFmt.format(ms2 * 1e3) + " mm/s²";
        } else if (ms2 < 1e3) {
            return stdFmt.format(ms2) + " m/s²";
        } else if (ms2 < 1e6) {
            return stdFmt.format(ms2 / 1e3) + " km/s²";
        } else {
            return sciFmt.format(ms2 / 1e3) + " km/s²";
        }
    }

    public static String adaptiveTime(double seconds) {
        if (seconds < 1) {
            return String.format("%.0fms", seconds * 1000);
        } else if (seconds < 60) {
            return String.format("%.1fs", seconds);
        } else if (seconds < 60 * 60) {
            int intSec = (int) seconds;
            return String.format("%dmin%ds", intSec / 60, intSec % 60);
        } else if (seconds < 60 * 60 * 24) {
            int intHours = (int) (seconds / 60);
            return String.format("%dh%dmin", intHours / 60, intHours % 60);
        } else if (seconds < 60 * 60 * 24 * 365) {
            return stdFmt.format(seconds / 60 / 60 / 24) + " days";
        } else {
            return stdFmt.format(seconds / 60 / 60 / 24 / 365) + " ys";
        }
    }

    public static String adaptiveDateTime(double seconds, ResourceBundle strings) {
        // Create the target date by adding the relative seconds to the epoch
        LocalDate targetDate = Instant.ofEpochSecond((long) seconds).atZone(ZoneOffset.UTC).toLocalDate();

        // Calculate the period between the epoch date and the target date
        Period period = Period.between(EPOCH, targetDate);

        // Calculate additional time components
        long remainingSeconds = (long) seconds % (24 * 3600); // Remaining seconds after days
        long hours = remainingSeconds / 3600;
        remainingSeconds %= 3600;
        long minutes = remainingSeconds / 60;
        long secs = remainingSeconds % 60;

        return String.format("%d %s %d %s %d %s, %02d:%02d",
                period.getYears(),
                strings.getString("timeYear"),
                period.getMonths(),
                strings.getString("timeMonth"),
                period.getDays(),
                strings.getString("timeDay"),
                hours, minutes);
    }

    public static String adaptiveEnergy(double joules) {
        double absJ = Math.abs(joules);
        if (absJ < 1e3) {
            return stdFmt.format(joules) + " J";
        } else if (absJ < 1e6) {
            return stdFmt.format(joules / 1e3) + " kJ";
        } else if (absJ < 1e9) {
            return stdFmt.format(joules / 1e6) + " MJ";
        } else if (absJ < 1e12) {
            return stdFmt.format(joules / 1e9) + " GJ";
        } else if (absJ < 1e15) {
            return stdFmt.format(joules / 1e12) + " TJ";
        } else if (absJ < 1e18) {
            return stdFmt.format(joules / 1e15) + " PJ";
        } else {
            return sciFmt.format(joules) + " J";
        }
    }
}
