package com.trashsoftware.gravity2.gui.widgets;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ContourData {

    private static final double MIN_LOG_FORCE = 1e-15; // Adjust based on the minimum significant force value
    private static final double MAX_LOG_FORCE = 1; // Adjust based on the maximum capped force value
    
    private static final double MIN_LOG_EFF_POT = 1e1;
    private static final double MAX_LOG_EFF_POT = 1e30;

    private final double level;
    private final ColorRGBA color;
    private final List<Mesh> paths;

    public ContourData(double level, ColorRGBA color) {
        this.level = level;
        this.color = color;
        this.paths = new ArrayList<>();
    }

    public double getLevel() {
        return level;
    }

    public ColorRGBA getColor() {
        return color;
    }

//    public void addPath(Path path) {
//        paths.add(path);
//    }

    public void draw(Node node,
                     Function<Double, Double> xTransform,
                     Function<Double, Double> yTransform) {
        
//        for (Path path : paths) {
//            gc.beginPath();
//            for (PathElement element : path.getElements()) {
//                if (element instanceof MoveTo moveTo) {
//                    gc.moveTo(xTransform.apply(moveTo.getX()), yTransform.apply(moveTo.getY()));
//                } else if (element instanceof LineTo lineTo) {
////                    System.out.println(xTransform.apply(lineTo.getX()) + ", " + yTransform.apply(lineTo.getY()));
//                    gc.lineTo(xTransform.apply(lineTo.getX()), yTransform.apply(lineTo.getY()));
//                }
//            }
//            gc.stroke();
//            
//        }
        if (!paths.isEmpty()) {
//            labelContour(gc, paths.getFirst(), xTransform, yTransform);
        }
    }

//    private void labelContour(GraphicsContext gc, Path path,
//                              Function<Double, Double> xTransform,
//                              Function<Double, Double> yTransform) {
//        if (path.getElements().isEmpty()) {
//            return;
//        }
//
//        // Find a suitable location for the label (e.g., middle of the first segment)
//        PathElement firstElement = path.getElements().get(0);
//        if (!(firstElement instanceof MoveTo)) {
//            return;
//        }
//        double labelX = 0;
//        double labelY = 0;
//        int count = 0;
//
//        for (PathElement element : path.getElements()) {
//            if (element instanceof LineTo) {
//                LineTo lineTo = (LineTo) element;
//                labelX += xTransform.apply(lineTo.getX());
//                labelY += yTransform.apply(lineTo.getY());
//                count++;
//            }
//        }
//
//        if (count > 0) {
//            labelX /= count;
//            labelY /= count;
//
//            gc.setFill(color);
//            gc.fillText(String.format("%5.2e", level), labelX, labelY);
//        }
//    }

    public static double[] allocateContourLevels2Power(int nLevels, double maxForce) {
        // Cap the max force to avoid infinity issues
        double cappedMaxForce = Math.min(maxForce, MAX_LOG_FORCE);
        
        double maxLog = Math.log(cappedMaxForce);

        // Allocate levels on a logarithmic scale
        double[] levels = new double[nLevels];

        for (int i = 0; i < nLevels; i++) {
            levels[i] = Math.pow(2, -i);
        }

        return levels;
    }

    public static double[] allocateContourLevels(int nLevels, double minForce, double maxForce) {
        // Cap the max force to avoid infinity issues
        double cappedMaxForce = Math.min(maxForce, MAX_LOG_FORCE);

        // Avoid issues with forces too close to zero
        double cappedMinForce = Math.max(minForce, MIN_LOG_FORCE);

        // Calculate log scale values
        double minLog = Math.log10(cappedMinForce);
        double maxLog = Math.log10(cappedMaxForce);

        // Allocate levels on a logarithmic scale
        double[] levels = new double[nLevels];
        double step = (maxLog - minLog) / (nLevels - 1);

        for (int i = 0; i < nLevels; i++) {
            levels[i] = Math.pow(10, minLog + i * step);
        }

        return levels;
    }

    public static double[] allocateEffPotContourLevels(int nLevels, double minEffPot, double maxEffPot) {
//        // Cap the max force to avoid infinity issues
//        double cappedMaxForce = Math.min(maxForce, MAX_LOG_EFF_POT);
//
//        // Avoid issues with forces too close to zero
//        double cappedMinForce = Math.max(minForce, MIN_LOG_EFF_POT);

        // Calculate log scale values
        double minLog = Math.log10(minEffPot);
        double maxLog = Math.log10(maxEffPot);

        // Allocate levels on a logarithmic scale
        double[] levels = new double[nLevels];
        double step = (maxLog - minLog) / (nLevels - 1);

        for (int i = 0; i < nLevels; i++) {
            levels[i] = Math.pow(10, minLog + i * step);
        }

        return levels;
    }
    
//    public static Color colorOfContour(double gravity) {
//        double minLog = Math.log10(MIN_LOG_FORCE);
//        double maxLog = Math.log10(MAX_LOG_FORCE);
//        double log = Math.log10(gravity);
//        return Color.hsb((log - minLog) / (maxLog - minLog) * 120, 1.0, 1.0);
//    }
}
