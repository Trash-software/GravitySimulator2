package com.trashsoftware.gui;

import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Sphere;
import com.jme3.util.BufferUtils;
import com.trashsoftware.physics.CelestialObject;
import com.trashsoftware.physics.OrbitalElements;
import com.trashsoftware.physics.Util;

import java.util.function.Function;

public class ObjectModel {
    public static final int COLOR_GRADIENTS = 16;
    protected final CelestialObject object;
    protected final ColorRGBA color;
    protected final ColorRGBA opaqueColor;
    protected final ColorRGBA darkerColor;
//    protected final ColorRGBA[] gradientColor = new ColorRGBA[COLOR_GRADIENTS];
    protected final App app;
    protected ObjectNode objectNode;
    protected Geometry model;
    protected Node labelNode;
    protected Geometry path;
    protected Geometry orbit;
    protected Geometry pathGradient;
    private boolean showLabel = true;
    
    private float lastRotateAngle;

    public ObjectModel(CelestialObject object, App app) {
        this.app = app;
        this.object = object;
        this.color = GuiUtils.stringToColor(object.getColorCode());
        this.opaqueColor = GuiUtils.opaqueOf(this.color, 0.5f);
        this.darkerColor = color.clone();
        darkerColor.interpolateLocal(app.backgroundColor, 0.9f);
        
//        float begin = 0.25f;
//        for (int i = 0; i < COLOR_GRADIENTS; i++) {
//            float interpolate = (float) i / COLOR_GRADIENTS * (1 - begin) + begin;
//            ColorRGBA c = color.clone();
////            gradientColor[i] = c.interpolateLocal(app.backgroundColor, interpolate);
//        }

        Sphere sphere = new Sphere(64, 64, (float) object.getEquatorialRadius());
        sphere.setTextureMode(Sphere.TextureMode.Projected);
        model = new Geometry(object.getName(), sphere);
        // Create a material for the box
        Material mat = new Material(App.getInstance().getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        if (object.getTexture() == null) {
            mat.setColor("Color", color);
        } else {
            mat.setTexture("ColorMap", object.getTexture());
        }
        model.setMaterial(mat);

        // Create the text label
        BitmapText labelText = new BitmapText(app.font);
        labelText.setText(object.getName());
        labelText.setColor(ColorRGBA.White);

        // Attach the text to a Node positioned above the object
        labelNode = new Node("LabelNode");
        labelNode.attachChild(labelText);
//        labelText.setLocalTranslation(0, 2.5f, 0); // Center the text above the object

        // Add a BillboardControl to the labelNode to make the text always face the camera
        BillboardControl billboardControl = new BillboardControl();
        labelNode.addControl(billboardControl);
        labelNode.setLocalScale(0.1f);

        objectNode = new ObjectNode();
        objectNode.attachChild(model);
        objectNode.attachChild(labelNode);

        System.out.println(object.getName() + " " + color);
        
        // Create a geometry, apply the mesh, and set the material
        path = new Geometry("Path", new Mesh());
        Material matLine = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matLine.setColor("Color", darkerColor);
        path.setMaterial(matLine);

        // Create a geometry, apply the mesh, and set the material
        orbit = new Geometry("Orbit", new Mesh());
        Material matLine2 = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matLine2.setColor("Color", darkerColor);
        orbit.setMaterial(matLine2);

        // Create a geometry, apply the mesh, and set the material
        pathGradient = new Geometry("PathGradient", new Mesh());
        Material matLine3 = new Material(app.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//        matLine3.setColor("Color", darkerColor);
        matLine3.setBoolean("VertexColor", true); // Enable vertex colors
        pathGradient.setMaterial(matLine3);
    }

    /**
     * Notify this model that its owner model may have some internal change
     */
    public void notifyObjectChanged() {
        // set the visual rotation axis
        double[] axisD = object.getRotationAxis();
        Vector3f axis = new Vector3f((float) axisD[0], (float) axisD[1], (float) axisD[2]).normalizeLocal();
        
        Quaternion tiltQuaternion = new Quaternion();
        tiltQuaternion.lookAt(axis, Vector3f.UNIT_Z);
        
        model.setLocalRotation(tiltQuaternion);
    }

    public void updateModelPosition(Function<Double, Float> xMapper,
                                    Function<Double, Float> yMapper,
                                    Function<Double, Float> zMapper,
                                    double scale) {
        model.setLocalScale((float) scale);

        float shift = (float) (scale * object.getPolarRadius());

        labelNode.setLocalTranslation(shift, shift, 0f);

        double[] position = object.getPosition();
        float x = xMapper.apply(position[0]);
        float y = yMapper.apply(position[1]);
        float z = zMapper.apply(position[2]);

        objectNode.setLocalTranslation(x, y, z);
        
        if (object.getAngularVelocity() != 0) {
            rotateModel();
        }
    }
    
    private void rotateModel() {
        float rotateAngle = (float) object.getRotationAngle();
        Quaternion rotation = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (rotateAngle - lastRotateAngle), Vector3f.UNIT_Z);
        model.rotate(rotation);
        
        lastRotateAngle = rotateAngle;
    }
    
    public void setShowLabel(boolean showLabel) {
        boolean wasShowLabel = this.showLabel;
        this.showLabel = showLabel;
        if (wasShowLabel != showLabel) {
            if (showLabel) {
                objectNode.attachChild(labelNode);
            } else {
                objectNode.detachChild(labelNode);
            }
        }
    }
    
    public Mesh createOrbitMesh(double[] barycenter, 
                                OrbitalElements oe,
                                int samples) {
//        float bcx = (float) barycenter[0];
//        float bcy = (float) barycenter[1];
//        float bcz = (float) barycenter[2];
        float bcx = app.paneX(barycenter[0]);
        float bcy = app.paneY(barycenter[1]);
        float bcz = app.paneZ(barycenter[2]);
//        Vector3f bc = new Vector3f(bcx * scale, bcy * scale, bcz * scale);
        
        float a = (float) (oe.semiMajorAxis * app.scale);
        float e = (float) oe.eccentricity;
        float omega = (float) (FastMath.DEG_TO_RAD * (oe.argumentOfPeriapsis));
        float omegaBig = (float) (FastMath.DEG_TO_RAD * (oe.ascendingNode));
        float i = (float) (FastMath.DEG_TO_RAD * oe.inclination);

        Mesh mesh = new Mesh();

        Vector3f[] vertices = new Vector3f[samples];
        for (int j = 0; j < samples; j++) {
            float theta = j * 2 * FastMath.PI / samples;
            float r = a * (1 - e * e) / (1 + e * FastMath.cos(theta));
            float x = r * FastMath.cos(theta);
            float y = r * FastMath.sin(theta);
            float z = 0;

            // Convert to 3D space using orbital elements
            Vector3f point = new Vector3f(x, y, z);
            
            point = UiVectorUtils.rotateAroundZAxis(point, omega); // Rotate by argument of periapsis
            point = UiVectorUtils.rotateAroundXAxis(point, i);     // Rotate by inclination
            point = UiVectorUtils.rotateAroundZAxis(point, omegaBig); // Rotate by longitude of the ascending node
            
            point.setX(point.x + bcx);
            point.setY(point.y + bcy);
            point.setZ(point.z + bcz);
            
            vertices[j] = point;
        }

        // Set up the index buffer for a line loop
        short[] indices = new short[samples];
        for (int j = 0; j < samples; j++) {
            indices[j] = (short) j;
        }

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createShortBuffer(indices));
        mesh.setMode(Mesh.Mode.LineLoop); // Create a closed loop
        mesh.updateBound();
        mesh.updateCounts();

        return mesh;
    }

    public ColorRGBA getColor() {
        return color;
    }
}
