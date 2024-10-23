package com.trashsoftware.gravity2.physics.status;

import com.trashsoftware.gravity2.fxml.units.UnitsUtil;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.HieraticalSystem;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.physics.VectorOperations;
import com.trashsoftware.gravity2.presets.SystemPresets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Comet extends Status {
    
    private CometTailParams dustTail;
    private final Map<CelestialObject, CometTailParams> ionTails = new HashMap<>();
    
    public Comet(CelestialObject co) {
        super(co);
    }
    
    public double getVolatileFraction() {
        return 0.5;
    }
    
    public double sublimationRate(double surfaceTemp) {
        double pv = vaporPressure(surfaceTemp);
        return pv * Math.sqrt(CelestialObject.WATER_MOLE_MASS / (2 * Math.PI * CelestialObject.BOLTZMANN_CONSTANT * surfaceTemp));
    }
    
    public double massLossRate(double surfaceTemperature, double surfaceArea) {
        double sr = sublimationRate(surfaceTemperature);
        return sr * surfaceArea * getVolatileFraction() * 0.5;  // 0.25 is the active area rate
    }
    
    public double vaporPressure(double t) {
        // todo: assume its water ice
        return 610.78 * Math.exp(17.27 * (t - 273.15) / (t - 35.85));
    }
    
    public void updateTails(Simulator simulator, 
                            List<Star> stars, 
                            double massLossRate,
                            double timeSteps) {
        ionTails.clear();

        double[] cometVelToParent;
        HieraticalSystem parent = simulator.getHieraticalSystem(co.getHillMaster());
        if (parent == null) {
            cometVelToParent = new double[simulator.getDimension()];
        } else {
            cometVelToParent = VectorOperations.subtract(co.getVelocity(), parent.getVelocity());
        }
        double[] windSum = new double[simulator.getDimension()];
        
        for (Star star : stars) {
            double[] relPos = VectorOperations.subtract(co.getPosition(), star.co.getPosition());
//            double dt = VectorOperations.magnitude(relPos);
            double[] windUnitDir = VectorOperations.scale(VectorOperations.normalize(relPos), -1);
            
            double ionRate = massLossRate * getVolatileFraction();
            double density = Math.sqrt(ionRate);
            double windSpeed = star.getStellarWindSpeed(simulator.getG());
            
            double length = Math.sqrt(ionRate) * windSpeed * 1e3;
            System.out.println("Wind: " + UnitsUtil.adaptiveSpeed(windSpeed) + 
                    ", ion tail length: " + UnitsUtil.adaptiveDistance(length));
            double[] windVel = VectorOperations.scale(windUnitDir, windSpeed);
            windSum = VectorOperations.add(windSum, windVel);
            
            double[] ionTailVel = VectorOperations.add(windVel, cometVelToParent);
            
            length = Math.min(length, SystemPresets.AU);
            
            // ion tail
            CometTailParams ionTail = new CometTailParams(true, 
                    length, 
                    density,
                    ionTailVel,
                    null,
                    timeSteps);
            ionTails.put(star.co, ionTail);
        }

//        System.out.println(massLossRate);

        double dustRate = massLossRate * (1 - getVolatileFraction());
        double density1 = Math.sqrt(dustRate);
        double length1 = Math.sqrt(dustRate) * 1e8;

        System.out.println("dust tail length:" + UnitsUtil.adaptiveDistance(length1));
        
        double[] dustVel = VectorOperations.scale(windSum, 0.25);
        dustVel = VectorOperations.add(dustVel, VectorOperations.scale(cometVelToParent, 0.8));

        length1 = Math.min(length1, SystemPresets.AU);

        dustTail = new CometTailParams(false,
                length1,
                density1,
                dustVel,
                null,
                timeSteps);
    }

    public CometTailParams getDustTail() {
        return dustTail;
    }

    public Map<CelestialObject, CometTailParams> getIonTails() {
        return ionTails;
    }
}
