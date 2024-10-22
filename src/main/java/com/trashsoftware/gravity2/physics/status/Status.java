package com.trashsoftware.gravity2.physics.status;

import com.trashsoftware.gravity2.physics.CelestialObject;

public abstract class Status {
    
    public final CelestialObject co;
    
    protected Status(CelestialObject co) {
        this.co = co;
    }
}
