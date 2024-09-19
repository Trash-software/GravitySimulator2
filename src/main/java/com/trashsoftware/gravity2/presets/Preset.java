package com.trashsoftware.gravity2.presets;

import com.trashsoftware.gravity2.gui.GuiUtils;
import com.trashsoftware.gravity2.physics.BodyType;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.utils.Util;

import java.util.Random;

import static com.trashsoftware.gravity2.presets.SystemPresets.*;

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

    static double randomStarSystem(Simulator simulator, int n,
                                   double sizeScale, double starMass,
                                   double planetMass) {
        double a = 2e10 * sizeScale;
        double b = 2e10 * sizeScale;
        double c = 1e9 * sizeScale;

        double flatRatio = c / (a + b) * 2;

        double centroidMass = starMass;
        double starDensity = BodyType.massiveObjectDensity(centroidMass);
        double starRadius = CelestialObject.radiusOf(centroidMass, starDensity);

        String colorCode = GuiUtils.temperatureToRGBString(
                CelestialObject.approxColorTemperatureOfStar(
                        CelestialObject.approxLuminosityOfStar(centroidMass),
                        starRadius
                )
        );

        CelestialObject centroid = CelestialObject.create3d(
                "Star",
                centroidMass,
                starRadius,
                new double[3],
                new double[3],
                colorCode
        );

        centroid.forcedSetRotation(new double[]{0, 0, 1}, 1e-4);
        simulator.addObject(centroid);

        SystemPresets.ObjectInfo[] presets = new SystemPresets.ObjectInfo[]{
                mercury, venus, earth, mars,
                jupiter, saturn, uranus, neptune
        };

        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            double x, y;

            // Generate random point within a unit sphere using the rejection method
            do {
                x = 2 * rand.nextDouble() - 1;
                y = 2 * rand.nextDouble() - 1;
            } while (x * x + y * y > 1);

            // Scale the point to the ellipsoid
            String cc = Util.randomCelestialColorCode();
            x = x * a;
            y = y * b;
            double dtToCenter = Math.sqrt(x * x + y * y);
            double z = rand.nextDouble(-dtToCenter, dtToCenter) * flatRatio;

            CelestialObject co;
            if (i < presets.length) {
                co = SystemPresets.createObject(
                        simulator,
                        presets[i],
                        new double[]{x, y, z},
                        new double[3],
                        new double[]{0, 0, 1},
                        1,
                        1e3
                );
            } else {
                double mass = rand.nextDouble(planetMass * 0.1, planetMass * 10);
                double density = rand.nextDouble(500, 6000);
                double radius = CelestialObject.radiusOf(mass, density);

                co = CelestialObject.create3d(
                        "Planet" + i,
                        mass,
                        radius,
                        new double[]{x, y, z},
                        new double[3],
                        cc
                );
            }
            double[] velocity = simulator.computeVelocityOfN(centroid,
                    co,
                    rand.nextDouble(0.75, 1.25),
                    new double[]{0, 0, 1});
            co.setVelocity(velocity);

//            double[] axis = new double[]{rand.nextDouble(0.0, 0.25), 
//                    rand.nextDouble(0.0, 0.5), 
//                    rand.nextDouble(0.5, 1.0)};
//            axis = VectorOperations.normalize(axis);
            double[] axis = new double[]{0, 0, 1};
            double angVel = rand.nextDouble(1e-8, 1e-3);
//            if (rand.nextDouble() < 0.1) {
//                axis = VectorOperations.reverseVector(axis);
//            }
            co.forcedSetRotation(axis, angVel);
            simulator.addObject(co);
        }

        SystemPresets.setTemperatureToSystem(simulator);

        return 30 / (a + b + c);
    }

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

    public static Preset TWO_SOLAR_SYSTEMS = new Preset("TwoSolarSystems", SystemPresets.sun.getNumChildrenIncludeSelf() * 2) {
        @Override
        public double instantiate(Simulator simulator) {
            double baseScale = SOLAR_SYSTEM.instantiate(simulator);
            simulator.shiftWholeSystem(new double[]{2e13, 0, 0});
            simulator.accelerateWholeSystem(new double[]{0.0, -1e3, 0.0});
            SOLAR_SYSTEM.instantiate(simulator);  // 2nd solar system

            return baseScale * 1e-1;
        }
    };

    public static Preset SIMPLE_THREE_BODY = new Preset("SimpleThreeBody", 3) {
        @Override
        public double instantiate(Simulator simulator) {
            // Star 1
            double star1Mass = 1.989e30;               // Mass in kg (1 Solar mass)
            double star1Radius = 6.957e8;              // Radius in meters (Sun's radius)
            double[] star1Position = new double[]{     // Position in meters
                    0, // x-coordinate
                    0,         // y-coordinate
                    0          // z-coordinate
            };
            double[] star1Velocity = new double[]{     // Velocity in meters per second
                    0,          // x-component
                    0,    // y-component
                    0           // z-component
            };
            String star1Color = GuiUtils.temperatureToRGBString(
                    CelestialObject.approxColorTemperatureOfStar(
                            CelestialObject.approxLuminosityOfStar(star1Mass),
                            star1Radius
                    )
            );
            CelestialObject star1 = CelestialObject.create3d(
                    "Star1",
                    star1Mass,
                    star1Radius,
                    star1Position,
                    star1Velocity,
                    star1Color
            );
            simulator.addObject(star1);

            // Star 2
            double star2Mass = 9.945e29;               // Mass in kg (0.5 Solar masses)
            double star2Radius = 3.4785e8;             // Radius in meters (approx. 0.5 Sun's radius)
            double[] star2Position = new double[]{     // Position in meters
                    1e11,  // x-coordinate
                    1e2,         // y-coordinate
                    1e2          // z-coordinate
            };
            double[] star2Velocity = new double[]{     // Velocity in meters per second
                    1e2,           // x-component
                    3e4,           // y-component
                    1e2            // z-component
            };
            String star2Color = GuiUtils.temperatureToRGBString(
                    CelestialObject.approxColorTemperatureOfStar(
                            CelestialObject.approxLuminosityOfStar(star2Mass),
                            star2Radius
                    )
            );
            CelestialObject star2 = CelestialObject.create3d(
                    "Star2",
                    star2Mass,
                    star2Radius,
                    star2Position,
                    star2Velocity,
                    star2Color
            );
            simulator.addObject(star2);

            // Planet
            double[] planetPosition = new double[]{    // Position in meters
                    1e2,            // x-coordinate
                    -5e10,          // y-coordinate (5 AU from the center of mass)
                    1e2             // z-coordinate
            };
            double[] planetVelocity = new double[]{    // Velocity in meters per second
                    5e4,    // x-component
                    1e2,          // y-component
                    1e2           // z-component
            };
            CelestialObject earthObj = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.earth,
                    planetPosition,
                    planetVelocity,
                    1
            );
            simulator.addObject(earthObj);

            // Moon
            double moonOrbitalRadius = 1e8;        // Orbital radius in meters (distance from Earth to Moon)

            // Calculate the moon's orbital velocity around the planet
            double moonOrbitalVelocity = Math.sqrt(simulator.getG() * earthObj.getMass() / moonOrbitalRadius); // In m/s

            // Moon's position relative to the planet
            double[] moonPositionRelative = new double[]{
                    moonOrbitalRadius,  // x-coordinate
                    1e2,                  // y-coordinate
                    1e2                   // z-coordinate
            };

            // Moon's absolute position
            double[] moonPosition = new double[]{
                    planetPosition[0] + moonPositionRelative[0],
                    planetPosition[1] + moonPositionRelative[1],
                    planetPosition[2] + moonPositionRelative[2]
            };

            // Moon's velocity relative to the planet
            double[] moonVelocityRelative = new double[]{
                    1e1,                       // x-component
                    moonOrbitalVelocity,     // y-component
                    1e1                        // z-component
            };

            // Moon's absolute velocity
            double[] moonVelocity = new double[]{
                    planetVelocity[0] + moonVelocityRelative[0],
                    planetVelocity[1] + moonVelocityRelative[1],
                    planetVelocity[2] + moonVelocityRelative[2]
            };

            CelestialObject moonObj = SystemPresets.createObjectPreset(
                    simulator,
                    SystemPresets.moon,
                    moonPosition,
                    moonVelocity,
                    1
            );
            simulator.addObject(moonObj);

            SystemPresets.setTemperatureToSystem(simulator);

            return 1e-8;
        }
    };

    public static Preset RANDOM_STAR_SYSTEM = new Preset("StarSystem", 91) {
        @Override
        public double instantiate(Simulator simulator) {
            return randomStarSystem(simulator, 90, 1, 5e29, 1e27);
        }
    };
    
    public static Preset TWO_RANDOM_STAR_SYSTEM = new Preset("TwoStarSystem", 182) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = randomStarSystem(simulator, 90, 0.8, 3e29, 5e25);
            simulator.rotateWholeSystem(new double[]{0, 0.5, 0.5});
            simulator.accelerateWholeSystem(new double[]{0, 0, -2e4});
            simulator.shiftWholeSystem(new double[]{1e11, 1e10, 1e10});

            randomStarSystem(simulator, 90, 1.2, 2e30, 1e26);

            return scale;
        }
    };

    public static Preset TWO_RANDOM_CHAOS_SYSTEM = new Preset("TwoChaosSystem", 182) {
        @Override
        public double instantiate(Simulator simulator) {
            double scale = randomStarSystem(simulator, 90, 0.8, 3e29, 1e27);
            simulator.rotateWholeSystem(new double[]{0, 0.5, 0.5});
            simulator.accelerateWholeSystem(new double[]{0, 0, -2e4});
            simulator.shiftWholeSystem(new double[]{1e11, 1e10, 1e10});

            randomStarSystem(simulator, 90, 1.2, 2e30, 2e28);

            return scale;
        }
    };

    public static final Preset[] DEFAULT_PRESETS = {
            SOLAR_SYSTEM, SOLAR_SYSTEM_WITH_ASTEROIDS, TWO_SOLAR_SYSTEMS,
            SIMPLE_THREE_BODY,
            RANDOM_STAR_SYSTEM, TWO_RANDOM_STAR_SYSTEM, TWO_RANDOM_CHAOS_SYSTEM
    };
}
