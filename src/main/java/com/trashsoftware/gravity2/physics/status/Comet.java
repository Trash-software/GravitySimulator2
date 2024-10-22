package com.trashsoftware.gravity2.physics.status;

import com.trashsoftware.gravity2.fxml.units.UnitsUtil;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.HieraticalSystem;
import com.trashsoftware.gravity2.physics.Simulator;
import com.trashsoftware.gravity2.physics.VectorOperations;

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
        return pv * Math.sqrt(co.getMass() / (2 * Math.PI * CelestialObject.BOLTZMANN_CONSTANT * surfaceTemp));
    }
    
    public double massLossRate(double surfaceTemperature, double surfaceArea) {
        double sr = sublimationRate(surfaceTemperature);
        return sr * surfaceArea * getVolatileFraction();
    }
    
    public double vaporPressure(double t) {
        // todo: assume its water ice
        return 610.78 * Math.exp(17.27 * (t - 273.15) / t - 35.85);
    }
    
    public void updateTails(Simulator simulator, List<Star> stars, double massLossRate) {
        ionTails.clear();
        
        double[] windSum = new double[simulator.getDimension()];
        
        for (Star star : stars) {
            double[] relPos = VectorOperations.subtract(co.getPosition(), star.co.getPosition());
//            double dt = VectorOperations.magnitude(relPos);
            double[] windUnitDir = VectorOperations.scale(VectorOperations.normalize(relPos), -1);
            
            double ionRate = massLossRate * getVolatileFraction();
            double density = Math.sqrt(ionRate);
            double windSpeed = star.getStellarWindSpeed(simulator.getG());
            
            double length = Math.sqrt(ionRate) * windSpeed * 1e-2;
//            System.out.println("Wind: " + UnitsUtil.adaptiveSpeed(windSpeed) + 
//                    ", tail length: " + UnitsUtil.adaptiveDistance(length));
            double[] windVel = VectorOperations.scale(windUnitDir, windSpeed);
            
            windSum = VectorOperations.add(windSum, windVel);
            
            // ion tail
            CometTailParams ionTail = new CometTailParams(true, 
                    length, 
                    density,
                    windVel,
                    null);
            ionTails.put(star.co, ionTail);
        }

        double dustRate = massLossRate * (1 - getVolatileFraction());
        double density1 = Math.sqrt(dustRate);
        double length1 = Math.sqrt(dustRate) * 1e3;

//        double[] cometVelToParent;
//        HieraticalSystem parent = simulator.getHieraticalSystem(co.getHillMaster());
//        if (parent == null) {
//            cometVelToParent = new double[simulator.getDimension()];
//        } else {
//            cometVelToParent = VectorOperations.subtract(co.getVelocity(), parent.getVelocity());
//        }
        double[] dustVel = VectorOperations.scale(windSum, 0.25);

        dustTail = new CometTailParams(false,
                length1,
                density1,
                dustVel,
                null);
    }

    public CometTailParams getDustTail() {
        return dustTail;
    }

    public Map<CelestialObject, CometTailParams> getIonTails() {
        return ionTails;
    }
}
