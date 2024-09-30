package com.trashsoftware.gravity2.utils;

import com.trashsoftware.gravity2.fxml.FxApp;

public enum OrbitPlane {
    XY("orbitPlaneXy"),
    ECLIPTIC("orbitPlaneEcliptic"),
    EQUATORIAL("orbitPlaneEquatorial");

    private final String key;

    OrbitPlane(String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return FxApp.getStrings().getString(key);
    }
}
