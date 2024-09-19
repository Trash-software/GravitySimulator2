package com.trashsoftware.gravity2.presets;

import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;

public abstract class Preset {
    
    public final String name;
    public final int nObjects;
    
    protected Preset(String name, int nObjects) {
        this.name = name;
        this.nObjects = nObjects;
    }

    /**
     * Creates the presets on the simulator.
     * 
     * @return the preferred scale
     */
    public abstract double instantiate(Simulator simulator);
    
    public static Preset SOLAR_SYSTEM = new Preset("SolarSystem", SystemPresets.sun.getNumChildrenIncludeSelf()) {
        @Override
        public double instantiate(Simulator simulator) {
            SystemPresets.makeSystem(simulator, SystemPresets.sun, 1, 1e3, 1e3);

            SystemPresets.setTemperatureToSystem(simulator);
            return 0.005 * 1e-7;
        }
    };

    public static Preset SOLAR_SYSTEM_WITH_ASTEROIDS = new Preset("SolarSystemWithAsteroids", 
            SystemPresets.sun.getNumChildrenIncludeSelf() + 35 + 50 + 25 + 40) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = SOLAR_SYSTEM.instantiate(simulator);

            simulator.simulate(10, false);
            simulator.updateMasters();

            CelestialObject sun = simulator.findByName("Sun");

            // jupiter around
            CelestialObject jupiter = simulator.findByName("Jupiter");
            SystemPresets.addAsteroidsTo(simulator, sun, jupiter, 35, 5e9);

            // asteroid belt
            CelestialObject ceres = simulator.findByName("Ceres");
            SystemPresets.addAsteroidsTo(simulator, sun, ceres, 50, 5e10);

            // earth belt
            CelestialObject earth = simulator.findByName("Earth");
            SystemPresets.addAsteroidsTo(simulator, sun, earth, 25, 5e8);

            // kuiper belt
            CelestialObject kuiper = simulator.findByName("Makemake");
            SystemPresets.addAsteroidsTo(simulator, sun, kuiper, 40, 5e11);

            return scale;
        }
    };
    
    public static Preset TWO_SOLAR_SYSTEMS = new Preset("TWO_SOLAR_SYSTEMS", SystemPresets.sun.getNumChildrenIncludeSelf() * 2) {
        @Override
        public double instantiate(Simulator simulator) {
            double baseScale = SOLAR_SYSTEM.instantiate(simulator);
            simulator.shiftWholeSystem(new double[]{2e13, 0, 0});
            simulator.accelerateWholeSystem(new double[]{0.0, -1e3, 0.0});
            SOLAR_SYSTEM.instantiate(simulator);  // 2nd solar system

            return baseScale * 1e-1;
        }
    };

    public static final Preset[] DEFAULT_PRESETS = {
            SOLAR_SYSTEM, SOLAR_SYSTEM_WITH_ASTEROIDS, TWO_SOLAR_SYSTEMS
    };
}
