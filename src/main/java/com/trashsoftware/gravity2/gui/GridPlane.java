package com.trashsoftware.gravity2.gui;

import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

public class GridPlane extends Node {
    
    JmeApp jmeApp;
    protected Geometry geometry;
    protected Mesh mesh;
    
    public GridPlane(JmeApp jmeApp) {
        super("GridPlane");
        
        this.jmeApp = jmeApp;
        
        Material mat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.Gray);
        geometry = new Geometry("GridGeom");
        geometry.setMaterial(mat);
        geometry.setMesh(ObjectModel.blank);
        
        attachChild(geometry);
    }
    
    public void hide() {
        geometry.setMesh(ObjectModel.blank);
    }
    
    public void showAt(Vector3f planeNormal, Vector3f pointOnPlane, 
                       int linesX, int linesY, float spacing) {
        if (mesh == null) {
            mesh = new Mesh();
        }
        geometry.setMesh(mesh);
        
        int totalLines = linesX + linesY + 2;
        Vector3f[] vertices = new Vector3f[totalLines * 2];
        
        int index = 0;
        // Generate grid along the X-axis (parallel to Z-axis)
        for (int i = -linesX / 2; i <= linesX / 2; i++) {
            vertices[index++] = new Vector3f(i * spacing,  (float) -linesY / 2 * spacing, 0);
            vertices[index++] = new Vector3f(i * spacing,  (float) linesY / 2 * spacing, 0);
        }

        // Generate grid along the Z-axis (parallel to X-axis)
        for (int i = -linesY / 2; i <= linesY / 2; i++) {
            vertices[index++] = new Vector3f((float) -linesX / 2 * spacing, i * spacing, 0);
            vertices[index++] = new Vector3f((float) linesX / 2 * spacing, i * spacing, 0);
        }

        // Set the vertices in the mesh
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setMode(Mesh.Mode.Lines); // Render as lines
        mesh.updateBound();
        mesh.updateCounts();

        // Rotate the grid to align with the plane's normal vector
        Quaternion rotation = new Quaternion();
        rotation.lookAt(planeNormal, Vector3f.UNIT_Z);  // Make the grid align with the plane's normal
        geometry.setLocalRotation(rotation);

        // Position the grid on the point of the plane
        geometry.setLocalTranslation(pointOnPlane);
    }
}
