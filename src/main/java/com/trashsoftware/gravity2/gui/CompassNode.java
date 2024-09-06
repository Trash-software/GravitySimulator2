package com.trashsoftware.gravity2.gui;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.*;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Line;

public class CompassNode extends Node {
    
    protected JmeApp jmeApp;
    protected float compassRadius = 80;
    
    public CompassNode(JmeApp jmeApp) {
        super("CompassNode");
        this.jmeApp = jmeApp;
        
        createMeshes();
    }
    
    private void createMeshes() {
        drawCompassTicksAndNumbers();
    }

    private void drawCompassTicksAndNumbers() {
        int numDivisions = 36; // 360 degrees divided by 10 degrees for ticks
        float angleIncrement = FastMath.TWO_PI / numDivisions; // Increment for each tick mark (in radians)

        for (int i = 0; i < numDivisions; i++) {
            float angle = i * angleIncrement;
            boolean isMajorTick = (i % 3 == 0); // Every 9th tick is a major tick (N, E, S, W, etc.)

            // Calculate position for each tick on the compass
            float startX = FastMath.cos(angle) * (compassRadius - 20); // Start of the tick
            float startY = FastMath.sin(angle) * (compassRadius - 20);
            float endX = FastMath.cos(angle) * (compassRadius - 10); // End of the tick
            float endY = FastMath.sin(angle) * (compassRadius - 10);

            // Draw the tick (a line)
            drawTick(startX, startY, endX, endY, isMajorTick);

            // Draw the appropriate number or letter for each major tick
            if (isMajorTick) {
                drawCompassLabel(angle);
            }
        }
    }

    // Get the label for major ticks: N, E, S, W, or the specified numbers
    private String getCompassLabel(int degInt) {
        return switch (degInt) {
            case 0 -> "N"; // 0 degrees
            case 90 -> "E"; // 90 degrees
            case 180 -> "S"; // 180 degrees
            case 270 -> "W"; // 270 degrees
            default -> String.valueOf(degInt / 10); // No label for other minor ticks
        };
    }

    private void drawTick(float startX, float startY, float endX, float endY, boolean isMajorTick) {
        // Use Vector3f for 3D coordinates, with Z set to 0 for 2D rendering
        Line tickLine = new Line(new Vector3f(startX, startY, 0), new Vector3f(endX, endY, 0));
        Geometry tickGeom = new Geometry("Tick", tickLine);
        Material tickMat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        tickMat.setColor("Color", isMajorTick ? ColorRGBA.White : ColorRGBA.Gray);
        tickGeom.setMaterial(tickMat);
        attachChild(tickGeom);
    }

    private void drawCompassLabel(float radAngle) {
        double degCompassAngle = FirstPersonMoving.gameAzimuthToCompass(FastMath.RAD_TO_DEG * radAngle);
        String label = getCompassLabel((int) degCompassAngle);
        
        // Create a bitmap text for the compass number/label
        BitmapFont font = jmeApp.getAssetManager().loadFont("Interface/Fonts/Default.fnt");
        BitmapText text = new BitmapText(font);
        text.setText(label);
        text.setColor(ColorRGBA.White);

        // Calculate the position to place the number (slightly inside the compass edge)
        float textX = FastMath.cos(radAngle) * (compassRadius - 30);
        float textY = FastMath.sin(radAngle) * (compassRadius - 30);

        // Set the bounding box for the text
//        text.setBox(new com.jme3.font.Rectangle(-compassRadius, -compassRadius, 
//                compassRadius * 2, compassRadius * 2));
//        text.setAlignment(BitmapFont.Align.Center);
//        text.setVerticalAlignment(BitmapFont.VAlign.Center);
        
        text.setLocalTranslation(textX - text.getLineWidth() * text.getLocalScale().getX() / 2, 
                textY - text.getLineHeight() * text.getLocalScale().getY() / 2, 0);
        
        // Rotate the text to make it point outward
        text.setLocalRotation(new Quaternion().fromAngleAxis(radAngle - FastMath.HALF_PI, Vector3f.UNIT_Z));

        // Attach the text to the compass node
        attachChild(text);
    }
}
