package com.trashsoftware.gravity2.gui;

import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Sphere;
import com.jme3.shadow.PointLightShadowFilter;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.fxml.units.UnitsConverter;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.DustObject;
import com.trashsoftware.gravity2.physics.OrbitalElements;
import com.trashsoftware.gravity2.physics.RealObject;
import com.trashsoftware.gravity2.physics.status.Comet;
import com.trashsoftware.gravity2.physics.status.CometTailParams;

import java.util.HashMap;
import java.util.Map;

public abstract class ObjectModel {
    public static final int COLOR_GRADIENTS = 16;
    protected static Mesh blank = new Mesh();

    static {
        blank.setBuffer(VertexBuffer.Type.Position, 3,
                BufferUtils.createFloatBuffer(new Vector3f(0, 0, 0)));
    }

    protected final RealObject object;
    protected final ColorRGBA color;
    protected final ColorRGBA opaqueColor;
    protected final ColorRGBA darkerColor;
    protected final JmeApp jmeApp;
    protected ObjectNode objectNode = new ObjectNode();
    protected Node rotatingNode;
    protected Geometry model;
    protected Geometry hillSphereModel;
    protected Geometry rocheLimitModel;
    protected BitmapText labelText;
    protected Node labelNode;

    protected Geometry path;

    protected Geometry orbit;
    protected final Map<String, Node> orbitInfoNodes = new HashMap<>();
    protected final Map<String, BitmapText> orbitInfoTexts = new HashMap<>();
    protected Node orbitNode;

    protected Geometry secondaryOrbit;
    protected Node secondaryOrbitNode;

    protected Geometry trace;
    protected Geometry axis;
    private boolean showLabel = true;
    private boolean showApPe = false;
    protected boolean renderLight = true;
    private boolean showHillSphere = false;
    protected boolean showRocheLimit = false;  // just leave it here

    protected Node barycenterMark;

    //    protected PointLightShadowRenderer plsr;
//    protected BloomFilter bloom;
    protected PointLightShadowFilter plsf;
//    protected FilterPostProcessor fpp;

    ParticleEmitter cometDustTail;
    Map<CelestialObject, ParticleEmitter> cometIonTails;

    protected Quaternion tiltRotation;
    protected double initialRadius;
    protected double displayingEmitLightColorTemp;

    protected ObjectModel(RealObject object, JmeApp jmeApp) {
        this.jmeApp = jmeApp;
        this.object = object;
        this.color = GuiUtils.stringToColor(object.getColorCode());
        this.opaqueColor = GuiUtils.opaqueOf(this.color, 0.5f);
        this.darkerColor = color.clone();
        darkerColor.interpolateLocal(jmeApp.backgroundColor, 0.9f);

        initialRadius = object.getMajorRadius();

        // Create the text label
        labelText = new BitmapText(jmeApp.font);
        labelText.setText(object.getNameShowing());
        labelText.setColor(ColorRGBA.White);

        // Attach the text to a Node positioned above the object
        labelNode = new Node("ObjectNameLabel");
        labelNode.attachChild(labelText);
//        labelText.setLocalTranslation(0, 2.5f, 0); // Center the text above the object

        // Add a BillboardControl to the labelNode to make the text always face the camera
        BillboardControl billboardControl = new BillboardControl();
        labelNode.addControl(billboardControl);
        labelNode.setLocalScale(0.1f);

        rotatingNode = new Node("Rotating");
        objectNode.attachChild(rotatingNode);
        objectNode.attachChild(labelNode);

        System.out.println(object.getId() + " " + color);

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
        orbitNode = new Node("OrbitNode");
        orbitNode.attachChild(orbit);
        createApPeText();

        // Create a geometry, apply the mesh, and set the material
        secondaryOrbit = new Geometry("OrbitSecondary", blank);
        Material matLine21 = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matLine21.setColor("Color", darkerColor);
        secondaryOrbit.setMaterial(matLine21);
        secondaryOrbitNode = new Node("OrbitNodeSecondary");
        secondaryOrbitNode.attachChild(secondaryOrbit);

        axis = new Geometry("Axis", blank);
        Material matLine4 = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        matLine4.setColor("Color", color);
        axis.setMaterial(matLine4);
        rotatingNode.attachChild(axis);

        // Create a geometry, apply the mesh, and set the material
        trace = new Geometry("Trace", blank);
        Material matLine3 = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
//        matLine3.setColor("Color", darkerColor);
        matLine3.setBoolean("VertexColor", true); // Enable vertex colors
        trace.setMaterial(matLine3);
    }

    public static ObjectModel create(RealObject object, JmeApp jmeApp) {
        if (object instanceof CelestialObject co) {
            return new SolidModel(co, jmeApp);
        } else if (object instanceof DustObject duo) {
            return new DustModel(duo, jmeApp);
        } else {
            throw new IllegalArgumentException("Unknown object type: " + object.getClass());
        }
    }

    private void initCometTails(Comet comet) {
        cometDustTail = new ParticleEmitter("DustTail-" + object.getId(),
                ParticleMesh.Type.Triangle, 300);
        Material dustTailMat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
        dustTailMat.setTexture("Texture",
                jmeApp.getAssetManager().loadTexture("com/trashsoftware/gravity2/effects/smoketrail_mirror.png"));
        cometDustTail.setMaterial(dustTailMat);
        cometDustTail.setImagesX(1);
        cometDustTail.setImagesY(3);

        cometDustTail.setStartColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 1.0f));
        cometDustTail.setEndColor(new ColorRGBA(0.5f, 0.5f, 0.5f, 0.0f));
        cometDustTail.getParticleInfluencer().setVelocityVariation(0.1f);
        cometDustTail.setFacingVelocity(true);

        objectNode.attachChild(cometDustTail);

        cometIonTails = new HashMap<>();
        for (var entry : comet.getIonTails().entrySet()) {
//            CometTailParams ctp = entry.getValue();

            ParticleEmitter ionTail = new ParticleEmitter("IonTail-" + object.getId() + "+" + entry.getKey().getId(),
                    ParticleMesh.Type.Triangle, 200);
            Material ionTailMat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Particle.j3md");
            ionTailMat.setTexture("Texture",
                    jmeApp.getAssetManager().loadTexture("com/trashsoftware/gravity2/effects/smoketrail_mirror.png"));
            ionTail.setMaterial(ionTailMat);
            ionTail.setImagesX(1);
            ionTail.setImagesY(3);

            // Set the particle properties for the gas tail (bluish color, small size)
            ionTail.setStartColor(new ColorRGBA(0.3f, 0.5f, 1.0f, 1.0f)); // Bluish ion tail
            ionTail.setEndColor(new ColorRGBA(0.3f, 0.5f, 1.0f, 0.0f));   // Fades out
            ionTail.setFacingVelocity(true);
            ionTail.setRotateSpeed(0);
//            ionTail.setStartSize(1.0f); // Tail particle size
//            ionTail.setEndSize(0.5f);
//            ionTail.setLowLife(1.0f);  // Short lifetime for fast ionized particles
//            ionTail.setHighLife(2.0f);
//            ionTail.setParticlesPerSec(50);
//            ionTail.setGravity(0, 0, 0); // No gravity influence on ion particles

            ionTail.getParticleInfluencer().setVelocityVariation(0.01f);

//            // Compute the direction of the tail (from comet to star, normalized)
//            Vector3f tailDirection = GuiUtils.fromDoubleArray(ctp.initDirection);
//            ionTail.getParticleInfluencer().setInitialVelocity(tailDirection.mult(-100f)); // Fast solar wind speed
//            ionTail.getParticleInfluencer().setVelocityVariation(0.2f);

            cometIonTails.put(entry.getKey(), ionTail);
            objectNode.attachChild(ionTail);
        }
    }

    private void adjustCometTails(Comet comet) {
        float scaleF = (float) jmeApp.getScale();
//        float speedF = (float) jmeApp.getSimulationSpeed();

        Vector3f focusMove = jmeApp.getLastFrameScreenMovement().toVector3f();

        CometTailParams dust = comet.getDustTail();
        float timeStepF = (float) dust.timeSteps;
//        float density1 = (float) (dust.tailDensity / 1e5);

        Vector3f initVel1 = GuiUtils.fromDoubleArray(dust.velocity)
                .mult(timeStepF * scaleF * -1);
        initVel1.subtractLocal(focusMove);
        initVel1.multLocal(jmeApp.getFrameRate());

        float velMag1 = initVel1.length();
        float dustTailLen = (float) (dust.tailLength * scaleF);
        float life1 = dustTailLen / velMag1;

        float particleRate1 = cometDustTail.getMaxNumParticles() / life1;
//        System.out.println(particleRate1 + " life: " + life1);
        particleRate1 = FastMath.clamp(particleRate1, 5, 50);

        float dustSize = (float) (comet.co.getAverageRadius() * scaleF * 50f);
        float dustSize2 = dustTailLen / 200f;
        dustSize = Math.max(dustSize, dustSize2);

        cometDustTail.setParticlesPerSec(particleRate1);
        cometDustTail.setLowLife(life1 * 0.5f);
        cometDustTail.setHighLife(life1);
        cometDustTail.setStartSize(dustSize);
        cometDustTail.setEndSize(dustSize * 20);
        cometDustTail.getParticleInfluencer().setInitialVelocity(initVel1);

        for (var entry : comet.getIonTails().entrySet()) {
            CometTailParams ctp = entry.getValue();
            ParticleEmitter ionTail = cometIonTails.get(entry.getKey());

//            float density = (float) (ctp.tailDensity / 3e5);

            Vector3f initVel = GuiUtils.fromDoubleArray(ctp.velocity)
                    .mult(timeStepF * scaleF * -1);
            initVel.subtractLocal(focusMove);
            initVel.multLocal(jmeApp.getFrameRate());

            float velMag = initVel.length();
            float ionTailLen = (float) (ctp.tailLength * scaleF);
            float life = ionTailLen / velMag;

            float particleRate = ionTail.getMaxNumParticles() / life;
            particleRate = FastMath.clamp(particleRate, 5, 50);

            float ionSize = (float) (comet.co.getAverageRadius() * scaleF * 10f);
            float ionSize2 = ionTailLen / 500f;
            ionSize = Math.max(ionSize, ionSize2);

//            ionTail.setNumParticles((int) density);
            ionTail.setParticlesPerSec(particleRate);
            ionTail.setLowLife(life * 0.5f);
            ionTail.setHighLife(life);
            ionTail.setStartSize(ionSize);
            ionTail.setEndSize(ionSize);
//            ionTail.set

            // Compute the direction of the tail (from comet to star, normalized)
            ionTail.getParticleInfluencer().setInitialVelocity(initVel); // Fast solar wind speed

//            System.out.println(initVel + " " + density + " " + life);
//            System.out.println("Dust tail len: " + UnitsUtil.adaptiveDistance(comet.getDustTail().tailLength) + 
//                    ", ion tail len: " + UnitsUtil.adaptiveDistance(ctp.tailLength) + 
//                    ", ion tail speed: " + UnitsUtil.adaptiveSpeed(VectorOperations.magnitude(ctp.velocity)) + 
//                    ", timeStep: " + ctp.timeSteps);

        }
    }

    protected void removeCometTails() {
        if (cometDustTail != null) {
            objectNode.detachChild(cometDustTail);
        }
        if (cometIonTails != null) {
            for (ParticleEmitter pe : cometIonTails.values()) {
                objectNode.detachChild(pe);
            }
        }
    }

    protected void updateCometTail(Comet comet) {
        if (cometDustTail == null) {
            initCometTails(comet);
        }
        adjustCometTails(comet);
    }

    public void clearEffects() {
        if (cometDustTail != null) {
            cometDustTail.killAllParticles();
        }
        if (cometIonTails != null) {
            for (ParticleEmitter pe : cometIonTails.values()) {
                pe.killAllParticles();
            }
        }
    }

    private void createApPeText() {
        for (String s : new String[]{"AP", "PE", "AN", "DN"}) {
            BitmapText text = new BitmapText(jmeApp.font);
            text.setColor(color);

            Node node = new Node(s + "_Label");
            node.attachChild(text);
//        labelText.setLocalTranslation(0, 2.5f, 0); // Center the text above the object

            BillboardControl bc = new BillboardControl();
            node.addControl(bc);
            node.setLocalScale(0.06f);

            orbitInfoTexts.put(s, text);
            orbitInfoNodes.put(s, node);
        }
    }

    Geometry createTransparentSphere(String name, ColorRGBA baseColor, float opacity) {
        // Create a sphere mesh
//        Sphere sphereMesh = new Sphere(32, 64, (float) object.getEquatorialRadius());  // 32 segments, radius 1
        Sphere sphereMesh = new Sphere(32, 64, (float) object.getAverageRadius());  // 32 segments, radius 1
        Geometry sphere = new Geometry(name, sphereMesh);

        // Create an unshaded material for the sphere
        Material mat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");

        // Set a semi-transparent color (RGBA format, where A is alpha/transparency)
        mat.setColor("Color", GuiUtils.opaqueOf(baseColor, opacity));  // Red with 50% transparency

        // Enable transparency
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);

        // Apply the material to the sphere geometry
        sphere.setMaterial(mat);

        // Ensure transparency is rendered correctly
        sphere.setQueueBucket(RenderQueue.Bucket.Transparent);

//        sphere.setLocalScale(0.01f);  // initially invisible

        return sphere;
    }

    protected abstract void updateModelScale(double scale);

    protected abstract void updateRelatedPosAndScale(double scale, Vector3f xyz);

    public void updateModelPosition(double scale) {
        updateModelScale(scale);
        float shift = (float) (scale * object.getMajorRadius());

        labelNode.setLocalTranslation(shift, shift, 0f);

        double[] position = object.getPosition();
        Vector3f xyz = new Vector3f(
                jmeApp.paneX(position[0]),
                jmeApp.paneY(position[1]),
                jmeApp.paneZ(position[2])
        );

        objectNode.setLocalTranslation(xyz);

        if (showHillSphere) {
            adjustHillSphereScale((float) scale);
        }
        updateRelatedPosAndScale(scale, xyz);
//        System.out.println(object.getName() + " " + xyz);
    }

    public abstract void notifyObjectChanged();

    public void setShowApPe(boolean showApPe) {
        // todo
        boolean wasShow = this.showApPe;
        this.showApPe = showApPe;
        if (wasShow != showApPe) {
            if (showApPe) {
                for (String s : orbitInfoNodes.keySet()) {
                    jmeApp.getRootNode().attachChild(orbitInfoNodes.get(s));
                }
            } else {
                for (String s : orbitInfoNodes.keySet()) {
                    jmeApp.getRootNode().detachChild(orbitInfoNodes.get(s));
                }
            }
        }
    }

    public void setShowLabel(boolean showLabel) {
        boolean wasShowLabel = this.showLabel;
        this.showLabel = showLabel;
//        System.out.println("Toggled " + object.getName() + " " + wasShowLabel + " " + showLabel);
        if (wasShowLabel != showLabel) {
            if (showLabel) {
                objectNode.attachChild(labelNode);
                rotatingNode.attachChild(axis);
            } else {
                objectNode.detachChild(labelNode);
                rotatingNode.detachChild(axis);
            }
        }
    }

    public void setShowHillSphere(boolean show) {
        boolean wasShow = showHillSphere;
        showHillSphere = show;
        if (wasShow != show) {
            if (show) {
                if (hillSphereModel == null) {
                    hillSphereModel = createTransparentSphere("Hill sphere " + object.getId(),
                            color,
                            0.1f);
                }
                objectNode.attachChild(hillSphereModel);
            } else {
                objectNode.detachChild(hillSphereModel);
            }
        }
    }

    public void setShowRocheLimit(boolean show) {
        boolean wasShow = showRocheLimit;
        showRocheLimit = show;
        if (wasShow != show) {
            if (show) {
                if (rocheLimitModel == null) {
                    rocheLimitModel = createTransparentSphere("Roche sphere " + object.getId(),
                            color,
                            0.2f);
                }
                objectNode.attachChild(rocheLimitModel);
            } else {
                objectNode.detachChild(rocheLimitModel);
            }
        }
    }

    public void setRenderLight(boolean renderLight) {
        boolean wasRenderLight = this.renderLight;
        this.renderLight = renderLight;
//        System.out.println("Toggled " + object.getName() + " " + wasShowLabel + " " + showLabel);
        if (wasRenderLight != renderLight) {
            updateLightSource();
        }
    }

    protected abstract void updateLightSource();

    private void adjustHillSphereScale(float baseScale) {
        if (hillSphereModel != null && object.getHillMaster() != null) {
            float ratio = (float) (object.getHillRadius() / object.getMajorRadius());
            hillSphereModel.setLocalScale(ratio * baseScale);
//            System.out.println(hillSphereModel.getWorldTranslation() + " " + hillSphereModel.getWorldScale());
        }
    }

    public void showEllipticOrbit(
            double[] barycenter,
            OrbitalElements oe,
            int samples,
            double childMassPercent,
            boolean isPrimary
    ) {
        Mesh mesh;
        Node node;
        if (isPrimary) {
            mesh = orbit.getMesh();
            if (mesh == null || mesh == ObjectModel.blank) {
                mesh = new Mesh();
                mesh.setMode(Mesh.Mode.LineStrip);
                orbit.setMesh(mesh);
            }
            node = orbitNode;
        } else {
            mesh = secondaryOrbit.getMesh();
            if (mesh == null || mesh == ObjectModel.blank) {
                mesh = new Mesh();
                mesh.setMode(Mesh.Mode.LineStrip);
                secondaryOrbit.setMesh(mesh);
            }
            node = secondaryOrbitNode;
        }

        Vector3f bc = jmeApp.panePosition(barycenter);

        float a = (float) (oe.semiMajorAxis * jmeApp.scale * (1 - childMassPercent));
        float e = (float) oe.eccentricity;
        float omega = (float) (FastMath.DEG_TO_RAD * (oe.argumentOfPeriapsis));
        float omegaBig = (float) (FastMath.DEG_TO_RAD * (oe.ascendingNode));
        float i = (float) (FastMath.DEG_TO_RAD * oe.inclination);

        Vector3f[] vertices = new Vector3f[samples + 1];
        for (int j = 0; j < samples; j++) {
            float theta = j * 2 * FastMath.PI / samples;
            float r = a * (1 - e * e) / (1 + e * FastMath.cos(theta));
            float x = r * FastMath.cos(theta);
            float y = r * FastMath.sin(theta);
            float z = 0;

            // Convert to 3D space using orbital elements
            vertices[j] = new Vector3f(x, y, z);
        }
        // manually create a line loop
        vertices[vertices.length - 1] = vertices[0].clone();

        // Set up the index buffer for a line loop
        short[] indices = new short[samples + 1];
        for (int j = 0; j < samples; j++) {
            indices[j] = (short) j;
        }
        indices[indices.length - 1] = indices[0];

        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createShortBuffer(indices));
//        mesh.setMode(Mesh.Mode.LineLoop); // Create a closed loop
        mesh.updateBound();
        mesh.updateCounts();

        // Create the quaternions for each rotation
        Quaternion rotateZ1 = new Quaternion();
        rotateZ1.fromAngleAxis(omega, Vector3f.UNIT_Z);  // Rotate by argument of periapsis (Z-axis)
        Quaternion rotateX = new Quaternion();
        rotateX.fromAngleAxis(i, Vector3f.UNIT_X);       // Rotate by inclination (X-axis)
        Quaternion rotateZ2 = new Quaternion();
        rotateZ2.fromAngleAxis(omegaBig, Vector3f.UNIT_Z);  // Rotate by longitude of ascending node (Z-axis)

        // Combine the rotations in the same order: Z -> X -> Z
        Quaternion combinedRotation = new Quaternion();
        combinedRotation.multLocal(rotateZ2).multLocal(rotateX).multLocal(rotateZ1);

        // Apply the combined rotation to the node
        node.setLocalRotation(combinedRotation);
        node.setLocalTranslation(bc);

        if (isPrimary && showApPe) {
            double aph = oe.semiMajorAxis * (1 + oe.eccentricity);
            double per = oe.semiMajorAxis * (1 - oe.eccentricity);

            UnitsConverter uc = jmeApp.getFxApp().getUnitConverter();

            Node apNode = orbitInfoNodes.get("AP");
            Node peNode = orbitInfoNodes.get("PE");

            Vector3f apPosition = new Vector3f(-a * (1 + e), 0, 0);  // Apogee (AP)
            Vector3f pePosition = new Vector3f(a * (1 - e), 0, 0);   // Perigee (PE)

            apPosition = combinedRotation.mult(apPosition);
            pePosition = combinedRotation.mult(pePosition);

            apNode.setLocalTranslation(apPosition.add(bc));
            orbitInfoTexts.get("AP").setText("AP\n" + uc.distance(aph));
            peNode.setLocalTranslation(pePosition.add(bc));
            orbitInfoTexts.get("PE").setText("PE\n" + uc.distance(per));

//            Vector3f anPosition = new Vector3f(0, -a * (1 - e * e), 0);
//            Vector3f dnPosition = new Vector3f(0, a * (1 - e * e), 0);

//            anPosition = rotateZ2.mult(anPosition);
//            dnPosition = rotateZ2.mult(dnPosition);

//            // todo: AN, DN should be relative, but the oe is calculated in absolute coordinates
//            // todo: really???
//            orbitInfoNodes.get("AN").setLocalTranslation(anPosition.add(bc));
//            orbitInfoTexts.get("AN").setText("AN\n" + UnitsUtil.shortFmt.format(oe.inclination));
//
//            orbitInfoNodes.get("DN").setLocalTranslation(dnPosition.add(bc));
//            orbitInfoTexts.get("DN").setText("DN\n" + UnitsUtil.shortFmt.format(oe.ascendingNode));
        }
    }

    public void showHyperbolicOrbit(
            double[] barycenter,
            OrbitalElements oe,
            int samples,
            double childMassPercent,
            boolean isPrimary) {

        Mesh mesh;
        Node node;
        if (isPrimary) {
            mesh = orbit.getMesh();
            if (mesh == null || mesh == ObjectModel.blank) {
                mesh = new Mesh();
                mesh.setMode(Mesh.Mode.LineStrip);
                orbit.setMesh(mesh);
            }
            node = orbitNode;
        } else {
            mesh = secondaryOrbit.getMesh();
            if (mesh == null || mesh == ObjectModel.blank) {
                mesh = new Mesh();
                mesh.setMode(Mesh.Mode.LineStrip);
                secondaryOrbit.setMesh(mesh);
            }
            node = secondaryOrbitNode;
        }

        Vector3f bc = jmeApp.panePosition(barycenter);

        float a = (float) (oe.semiMajorAxis * jmeApp.scale * (1 - childMassPercent));  // Semi-major axis
        float e = (float) oe.eccentricity;                   // Eccentricity
        float b = a * FastMath.sqrt(e * e - 1);
        float omega = (float) (FastMath.DEG_TO_RAD * (oe.argumentOfPeriapsis));   // Argument of periapsis
        float omegaBig = (float) (FastMath.DEG_TO_RAD * (oe.ascendingNode));      // Longitude of ascending node
        float i = (float) (FastMath.DEG_TO_RAD * oe.inclination);                 // Inclination

        Vector3f[] vertices = new Vector3f[samples + 1];
//        float hyperbolicLimit = 5.0f;  // Controls how far the orbit goes outward

        for (int j = 0; j < samples; j++) {
            float theta = -FastMath.PI + 2 * FastMath.PI * j / (samples - 1);  // Vary theta for hyperbolic orbit

            float x = a * ((float) Math.cosh(theta) - e);
            float y = b * (float) Math.sinh(theta);
            float z = 0;

            // Convert to 3D space using orbital elements
            Vector3f point = new Vector3f(x, y, z);

            vertices[j] = point;
        }

        // Set up the index buffer
        short[] indices = new short[samples];
        for (int j = 0; j < samples; j++) {
            indices[j] = (short) j;
        }

        // Set mesh data
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createShortBuffer(indices));
        mesh.setMode(Mesh.Mode.LineStrip);  // Open curve for hyperbolic orbits
        mesh.updateBound();
        mesh.updateCounts();

        // Create the quaternions for each rotation
        Quaternion rotateZ1 = new Quaternion();
        rotateZ1.fromAngleAxis(omega, Vector3f.UNIT_Z);  // Rotate by argument of periapsis (Z-axis)
        Quaternion rotateX = new Quaternion();
        rotateX.fromAngleAxis(i, Vector3f.UNIT_X);       // Rotate by inclination (X-axis)
        Quaternion rotateZ2 = new Quaternion();
        rotateZ2.fromAngleAxis(omegaBig, Vector3f.UNIT_Z);  // Rotate by longitude of ascending node (Z-axis)

        // Combine the rotations in the same order: Z -> X -> Z
        Quaternion combinedRotation = new Quaternion();
        combinedRotation.multLocal(rotateZ2).multLocal(rotateX).multLocal(rotateZ1);

        // Apply the combined rotation to the node
        node.setLocalRotation(combinedRotation);
        node.setLocalTranslation(bc);

        if (isPrimary && showApPe) {
//            double aph = oe.semiMajorAxis * (1 + oe.eccentricity);
            double per = oe.semiMajorAxis * (1 - oe.eccentricity);

            UnitsConverter uc = jmeApp.getFxApp().getUnitConverter();

            Node peNode = orbitInfoNodes.get("PE");

            Vector3f apPosition = new Vector3f(-a * (1 + e), 0, 0);  // Apogee (AP)
            Vector3f pePosition = new Vector3f(a * (1 - e), 0, 0);   // Perigee (PE)

            pePosition = combinedRotation.mult(pePosition);

            peNode.setLocalTranslation(pePosition.add(bc));
            orbitInfoTexts.get("PE").setText("PE\n" + uc.distance(per));
        }
    }

    public ColorRGBA getColor() {
        return color;
    }
}
