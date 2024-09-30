package com.trashsoftware.gravity2.gui;

import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.utils.OrbitPlane;

public class SpawningObject {
    
    protected JmeApp jmeApp;
    protected ObjectModel model;
    protected CelestialObject object;
    protected double orbitSpeed;
    
    protected CelestialObject spawnRelative;
    protected OrbitPlane orbitPlane;
    protected double axisTilt;
    private double[] planeNormal = new double[]{0, 0, 1};
    
    protected TextLine primaryLine;
    protected TextLine secondaryLine;
    
    SpawningObject(JmeApp jmeApp, ObjectModel model, double orbitSpeed, 
                   OrbitPlane orbitPlane, double axisTilt) {
        this.jmeApp = jmeApp;
        this.model = model;
        this.object = model.object;
        this.orbitSpeed = orbitSpeed;
        this.axisTilt = axisTilt;
        this.orbitPlane = orbitPlane;

        primaryLine = new TextLine(ColorRGBA.Yellow);
        secondaryLine = new TextLine(ColorRGBA.DarkGray);
    }
    
    public void updatePlane(CelestialObject reference) {
        switch (orbitPlane) {
            case XY -> this.planeNormal = new double[]{0, 0, 1};
            case EQUATORIAL -> this.planeNormal = reference.getRotationAxis();
            case ECLIPTIC -> this.planeNormal = reference.getEclipticPlaneNormal();
        }
    }

    public double[] getPlaneNormal() {
        return planeNormal;
    }

    public CelestialObject getSpawnRelative() {
        if (spawnRelative != null) {
            return spawnRelative;
        } else {
            return object.getHillMaster();
        }
    }
    
    public class TextLine extends Node {
        
        protected Geometry geometry;
        protected Material material;
        protected Mesh mesh;
        protected BitmapText labelText;
        protected Node labelNode;
        
        TextLine(ColorRGBA color) {
            super("TextLine");

            material = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
            material.setColor("Color", color);
            geometry = new Geometry("ConnectionLine");
            geometry.setMaterial(material);
            geometry.setMesh(ObjectModel.blank);

            // Create the text label
            labelText = new BitmapText(jmeApp.font);
            labelText.setText("--");
            labelText.setColor(ColorRGBA.White);

            // Attach the text to a Node positioned above the object
            labelNode = new Node("SpawnDistanceLabel");
            labelNode.attachChild(labelText);
//          labelText.setLocalTranslation(0, 2.5f, 0); // Center the text above the object

            // Add a BillboardControl to the labelNode to make the text always face the camera
            BillboardControl billboardControl = new BillboardControl();
            labelNode.addControl(billboardControl);
            labelNode.setLocalScale(0.1f);
            
            attachChild(geometry);
            attachChild(labelNode);
        }
        
        void show(Vector3f start, Vector3f end, String text) {
            if (mesh == null) {
                mesh = new Mesh();
            }
            geometry.setMesh(mesh);
            
            Vector3f[] vertices = new Vector3f[]{start, end};
            // Set the vertices in the mesh
            mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
            mesh.setMode(Mesh.Mode.Lines); // Render as lines
            mesh.updateBound();
            mesh.updateCounts();
            
            Vector3f midPoint = start.add(end).mult(0.5f);
            labelText.setText(text);
            labelText.setLocalTranslation(midPoint.x - labelText.getLineWidth() / 2, midPoint.y, midPoint.z);
        }
        
        void hide() {
            geometry.setMesh(ObjectModel.blank);
        }
    }
}
