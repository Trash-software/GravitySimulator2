package com.trashsoftware.gravity2.gui;

import com.jme3.font.BitmapText;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.post.FilterPostProcessor;
import com.jme3.post.filters.BloomFilter;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.shadow.PointLightShadowRenderer;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.OrbitalElements;

public class ObjectModel {
    public static final int COLOR_GRADIENTS = 16;
    protected final CelestialObject object;
    protected final ColorRGBA color;
    protected final ColorRGBA opaqueColor;
    protected final ColorRGBA darkerColor;
    protected final JmeApp jmeApp;
    protected ObjectNode objectNode = new ObjectNode();
    protected Node rotatingNode;
    protected Geometry model;
    protected BitmapText labelText;
    protected Node labelNode;
    protected Geometry path;
    protected Geometry orbit;
    protected Geometry pathGradient;
    private boolean showLabel = true;
    protected Mesh blank = new Mesh();
    protected PointLight emissionLight;
    protected AmbientLight surfaceLight;

    protected PointLightShadowRenderer plsr;
    protected PointLightShadowFilter plsf;
    protected FilterPostProcessor fpp;

    private float lastRotateAngle;

    public ObjectModel(CelestialObject object, JmeApp jmeApp) {
        this.jmeApp = jmeApp;
        this.object = object;
        this.color = GuiUtils.stringToColor(object.getColorCode());
        this.opaqueColor = GuiUtils.opaqueOf(this.color, 0.5f);
        this.darkerColor = color.clone();
        darkerColor.interpolateLocal(jmeApp.backgroundColor, 0.9f);
        
        blank.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(new Vector3f(0, 0, 0)));

        Sphere sphere = new Sphere(64, 64, (float) object.getEquatorialRadius());
        sphere.setTextureMode(Sphere.TextureMode.Projected);
        model = new Geometry(object.getName(), sphere);
        // Create a material for the box
        Material mat = new Material(JmeApp.getInstance().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");

        if (object.getTexture() == null) {
            mat.setColor("Diffuse", color);
        } else {
            mat.setTexture("DiffuseMap", object.getTexture());
        }
        if (object.isEmittingLight()) {
            mat.setFloat("Shininess", 128);
            mat.setColor("GlowColor", ColorRGBA.Yellow); // Glow effect color

            emissionLight = new PointLight();
            adjustPointLight();
            jmeApp.getRootNode().addLight(emissionLight);

            surfaceLight = new AmbientLight();
            surfaceLight.setColor(ColorRGBA.White);
            model.addLight(surfaceLight);

//            // Add shadow renderer
//            plsr = new PointLightShadowRenderer(jmeApp.getAssetManager(),
//                    1024);
//            plsr.setLight(emissionLight);
//            plsr.setShadowIntensity(0.9f); // Adjust the shadow intensity
//            plsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
//            jmeApp.getViewPort().addProcessor(plsr);

            // Add shadow filter for softer shadows
//            plsf = new PointLightShadowFilter(jmeApp.getAssetManager(), 1024);
//            plsf.setLight(emissionLight);
//            plsf.setEnabled(true);
            fpp = new FilterPostProcessor(jmeApp.getAssetManager());
//            fpp.addFilter(plsf);

            // Add bloom effect to enhance the star's glow
            BloomFilter bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
//            bloom.set
            bloom.setBloomIntensity(2.0f); // Adjust intensity for more or less glow
//            bloom.setBlurScale(10.0f);
            fpp.addFilter(bloom);

            jmeApp.getViewPort().addProcessor(fpp);
            model.setShadowMode(RenderQueue.ShadowMode.Off);
        } else {
//            model.setCullHint(Spatial.CullHint.Never);
            model.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        }
        model.setMaterial(mat);
//        model.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);

        // Create the text label
        labelText = new BitmapText(jmeApp.font);
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

        rotatingNode = new Node("Rotating");
        rotatingNode.attachChild(model);
        objectNode.attachChild(rotatingNode);
        objectNode.attachChild(labelNode);

        System.out.println(object.getName() + " " + color);

        // Create a geometry, apply the mesh, and set the material
        path = new Geometry("Path", blank);
        Material matLine = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matLine.setColor("Color", darkerColor);
        path.setMaterial(matLine);

        // Create a geometry, apply the mesh, and set the material
        orbit = new Geometry("Orbit", blank);
        Material matLine2 = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matLine2.setColor("Color", darkerColor);
        orbit.setMaterial(matLine2);

        // Create a geometry, apply the mesh, and set the material
        pathGradient = new Geometry("PathGradient", blank);
        Material matLine3 = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//        matLine3.setColor("Color", darkerColor);
        matLine3.setBoolean("VertexColor", true); // Enable vertex colors
        pathGradient.setMaterial(matLine3);
    }

    private void adjustPointLight() {
        double luminosity = object.getLuminosity();
        double radius = Math.pow(jmeApp.getScale(), 2) * luminosity * 2e-2;
//        System.out.println(radius);
        double scaledLuminosity = 1;
//        System.out.println(scaledLuminosity);
        emissionLight.setColor(ColorRGBA.White.mult((float) scaledLuminosity));
        emissionLight.setRadius((float) radius);

//        System.out.println(scaledLuminosity);
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

        rotatingNode.setLocalRotation(tiltQuaternion);
    }

    public void updateModelPosition(double scale) {
        double radiusScale = scale;
        if (object.getEquatorialRadius() * scale < 0.1) {
            radiusScale = 0.1 / object.getEquatorialRadius();
        }
        double ratio = object.getPolarRadius() / object.getEquatorialRadius();
        float eqScale = (float) radiusScale;
        float polarScale = (float) (radiusScale * ratio);
        rotatingNode.setLocalScale(eqScale, eqScale, polarScale);

        float shift = (float) (scale * object.getPolarRadius());

        labelNode.setLocalTranslation(shift, shift, 0f);

        double[] position = object.getPosition();
        Vector3f xyz = new Vector3f(
                jmeApp.paneX(position[0]),
                jmeApp.paneY(position[1]),
                jmeApp.paneZ(position[2])
        );

        objectNode.setLocalTranslation(xyz);

        if (object.getAngularVelocity() != 0) {
            rotateModel();
        }
        if (object.isEmittingLight()) {
            emissionLight.setPosition(xyz);
            adjustPointLight();
        }
    }

    // Method to calculate the position on the ellipsoid at a given latitude, longitude, and altitude
    public Vector3d calculateSurfacePosition(double latitude, double longitude, double altitude) {
        // Convert latitude and longitude to radians
        double lat = Math.toRadians(latitude);
        double lon = Math.toRadians(longitude);

        double equatorialRadius = object.getEquatorialRadius();
        double polarRadius = object.getPolarRadius();

        // Calculate the surface position on the ellipsoid
        double x = equatorialRadius * Math.cos(lat) * Math.cos(lon); // X-axis
        double z = polarRadius * Math.sin(lat);                          // Y-axis (polar)
        double y = equatorialRadius * Math.cos(lat) * Math.sin(lon); // Z-axis

        // Create the surface position vector
        Vector3d surfacePosition = new Vector3d(x, y, z);

//        return surfacePosition;

        // Calculate the surface normal (for altitude adjustment)
        Vector3d surfaceNormal = new Vector3d(
                x / (equatorialRadius * equatorialRadius),
                y / (polarRadius * polarRadius),
                z / (equatorialRadius * equatorialRadius)
        ).normalizeLocal();

        // Adjust the position by altitude (move along the normal)
        return surfacePosition.add(surfaceNormal.mult(altitude));
    }

    // Method to calculate the surface normal at the given latitude and longitude on an ellipsoid
    public Vector3f calculateSurfaceNormal(float latitude, float longitude) {
        float equatorialRadius = (float) (object.getEquatorialRadius());
        float polarRadius = (float) (object.getPolarRadius());

        // Convert latitude and longitude to radians
        float lat = FastMath.DEG_TO_RAD * latitude;
        float lon = FastMath.DEG_TO_RAD * longitude;

        // Calculate the surface normal on the ellipsoid
        float x = FastMath.cos(lat) * FastMath.cos(lon); // X-axis
        float y = FastMath.sin(lat);                    // Y-axis (polar)
        float z = FastMath.cos(lat) * FastMath.sin(lon); // Z-axis

        return new Vector3f(x / (equatorialRadius * equatorialRadius),
                y / (polarRadius * polarRadius),
                z / (equatorialRadius * equatorialRadius)).normalizeLocal();
    }

    private void rotateModel() {
        float rotateAngle = (float) object.getRotationAngle();
        Quaternion rotation = new Quaternion().fromAngleAxis(FastMath.DEG_TO_RAD * (rotateAngle - lastRotateAngle), Vector3f.UNIT_Z);
        rotatingNode.rotate(rotation);

        lastRotateAngle = rotateAngle;
    }

    public void setShowLabel(boolean showLabel) {
        boolean wasShowLabel = this.showLabel;
        this.showLabel = showLabel;
//        System.out.println("Toggled " + object.getName() + " " + wasShowLabel + " " + showLabel);
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
        float bcx = jmeApp.paneX(barycenter[0]);
        float bcy = jmeApp.paneY(barycenter[1]);
        float bcz = jmeApp.paneZ(barycenter[2]);
//        Vector3f bc = new Vector3f(bcx * scale, bcy * scale, bcz * scale);

        float a = (float) (oe.semiMajorAxis * jmeApp.scale);
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
