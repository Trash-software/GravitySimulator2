package com.trashsoftware.gravity2.gui;

import com.jme3.light.AmbientLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Sphere;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.physics.CelestialObject;
import com.trashsoftware.gravity2.physics.status.Comet;
import com.trashsoftware.gravity2.physics.status.Star;
import com.trashsoftware.gravity2.physics.status.Status;

public class SolidModel extends ObjectModel {
    protected final CelestialObject object;

    final int samples;

    //    protected PointLight emissionLight;
    protected AmbientLight surfaceLight;

    protected Vector3f rotationAxis;

    protected FirstPersonMoving firstPersonMoving;

    protected LightSourceModel lightModel;

    public SolidModel(CelestialObject object, JmeApp jmeApp) {
        super(object, jmeApp);

        this.object = object;

        String texturePath = object.getTexturePath();
        if (texturePath == null) {
            samples = 32;
        } else {
            samples = 64;
        }

        Sphere sphere = new Sphere(samples, samples * 2, (float) object.getEquatorialRadius());
        sphere.setTextureMode(Sphere.TextureMode.Projected);

        model = new Geometry(object.getId(), sphere);
        // Create a material for the box
        Material mat = new Material(JmeApp.getInstance().getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");

        if (texturePath == null) {
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", color);
            mat.setColor("Ambient", color);
        } else {
            Texture texture = jmeApp.getAssetManager().loadTexture(texturePath);
            mat.setTexture("DiffuseMap", texture);
        }
        model.setMaterial(mat);

        rotatingNode.attachChild(model);

        updateLightSource();
        createAxisMesh();
    }

    @Override
    protected void updateModelScale(double scale) {
        double baseScale = object.getMajorRadius() / initialRadius;

        double radiusScale = scale * baseScale;
        if (object.getMajorRadius() * scale < 0.1) {
            radiusScale = 0.1 / object.getMajorRadius();
        }
        double ratio = object.getPolarRadius() / object.getEquatorialRadius();
        float eqScale = (float) radiusScale;
        float polarScale = (float) (radiusScale * ratio);
        rotatingNode.setLocalScale(eqScale, eqScale, polarScale);
    }

    @Override
    protected void updateRelatedPosAndScale(double scale, Vector3f xyz) {
        if (object.getAngularVelocity() != 0) {
            rotateModel();
        }
        if (super.renderLight && object.getStatus() instanceof Star star) {
            if (lightModel == null) {
                updateLightSource();
            }
            lightModel.emissionLight.setPosition(xyz);
            adjustPointLight(star);
//            System.out.println(object.getName() + " " + emissionLight.getPosition() + " " + emissionLight.getRadius());
        }
        if (object.getStatus() instanceof Comet comet) {
            updateCometTail(comet);
        } else {
            if (cometDustTail != null) {
                removeCometTails();
            }
        }
        if (showRocheLimit) {
            adjustRocheLimitScale((float) scale);
        }
        if (lightModel != null && lightModel.showHabitableZone) {
            adjustHabitableZoneScale((float) scale);
        }
    }

    protected void updateLightSource() {
        Material mat = model.getMaterial();
        Status status = object.getStatus();
//        boolean emitting = object.isEmittingLight();
        boolean changed = false;
        if (renderLight) {
            if (status instanceof Star star) {
                if (lightModel == null) {
                    lightModel = new LightSourceModel();
                    lightModel.addThisTo(jmeApp);

                    surfaceLight = new AmbientLight();
                    model.addLight(surfaceLight);

                    model.setShadowMode(RenderQueue.ShadowMode.Off);
                    adjustPointLight(star);
                    changed = true;
                }
            } else {
                changed = removeEmissionLight() || removeEffectLights();
                model.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
            }
        } else {
            changed = removeEmissionLight() || removeEffectLights();
            model.setShadowMode(RenderQueue.ShadowMode.CastAndReceive);
        }
        if (changed) {
            model.setMaterial(mat);
        }
    }

    protected boolean removeEmissionLight() {
        boolean changed = false;

        if (lightModel != null) {
            changed = lightModel.removeThisFrom(jmeApp, object.getId());
            lightModel = null;
        }

        if (surfaceLight != null) {
            model.removeLight(surfaceLight);
            surfaceLight = null;
            changed = true;
        }

        if (plsf != null) {
            try {
                jmeApp.filterPostProcessor.removeFilter(plsf);
            } catch (RuntimeException e) {
                System.err.println(object.getId() + " has rendering problem: plsf.");
                e.printStackTrace(System.err);
            }
            plsf = null;
            changed = true;
        }
        return changed;
    }

    private void updateEmissionColor(ColorRGBA lightColor) {
        lightModel.emissionLight.setColor(lightColor);
        model.getMaterial().setColor("GlowColor", lightColor);
        surfaceLight.setColor(lightColor);
    }

    private boolean removeEffectLights() {
        return lightModel != null && lightModel.removeEffectLights(jmeApp, object.getId());
    }

    private void adjustPointLight(Star star) {
        double scale = jmeApp.getScale();
        double luminosity = object.getLuminosity();

        if (object.getLightColorCode() != null) {
            ColorRGBA lightColor = GuiUtils.stringToColor(object.getLightColorCode());
            updateEmissionColor(lightColor);
        } else {
            double colorTemp = star.getEmissionColorTemperature();
            if (colorTemp != displayingEmitLightColorTemp) {
                displayingEmitLightColorTemp = colorTemp;
                ColorRGBA lightColor = GuiUtils.stringToColor(GuiUtils.temperatureToRGBString(colorTemp));
                updateEmissionColor(lightColor);
            }
        }

        double radius = Math.pow(scale, 2) * luminosity * 2e-2;
        lightModel.emissionLight.setRadius((float) radius);
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
        // set the visual rotation axis
        double[] axisD = object.getRotationAxis();
        Vector3f axis = new Vector3f((float) axisD[0], (float) axisD[1], (float) axisD[2]).normalizeLocal();

        if (!axis.equals(rotationAxis)) {
            notifyObjectChanged();
        }

        // Convert the current rotation degrees to radians
        float currentRotationRad = FastMath.DEG_TO_RAD * (float) object.getRotationAngle();

        // Create a quaternion representing the current rotation around the Earth's axis (Y-axis)
        Quaternion rotation = new Quaternion();
        rotation.fromAngleAxis(currentRotationRad, Vector3f.UNIT_Z);

        // Combine the tilt rotation (23.5 degrees) with the current rotation
        Quaternion combinedRotation = tiltRotation.mult(rotation);

        // Apply the combined rotation to the sphere geometry
        rotatingNode.setLocalRotation(combinedRotation);
    }

    private void createAxisMesh() {
        Mesh mesh = new Mesh();
        Vector3f[] vertices = new Vector3f[2];
        vertices[0] = new Vector3f(0, 0, 0);
        vertices[1] = new Vector3f(0, 0, (float) (object.getPolarRadius() * 1.5));

        mesh.setMode(Mesh.Mode.Lines);
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        // Set up indices to connect the vertices as line segments
        short[] indices = new short[]{0, 1};
        mesh.setBuffer(VertexBuffer.Type.Index, 2, BufferUtils.createShortBuffer(indices));

        mesh.updateBound();
        mesh.updateCounts();

        axis.setMesh(mesh);
    }

    /**
     * Notify this model that its owner model may have some internal change
     */
    @Override
    public void notifyObjectChanged() {
        // set the visual rotation axis
        double[] axisD = object.getRotationAxis();
        rotationAxis = new Vector3f((float) axisD[0], (float) axisD[1], (float) axisD[2]).normalizeLocal();

        tiltRotation = new Quaternion();
        tiltRotation.lookAt(rotationAxis, Vector3f.UNIT_Z);

        rotatingNode.setLocalRotation(tiltRotation);

        updateLightSource();
    }

    private void adjustRocheLimitScale(float baseScale) {
        if (rocheLimitModel != null) {
            float ratio = (float) (object.getApproxRocheLimit() / object.getEquatorialRadius());
//            System.out.println(object.getName() + ratio);
            rocheLimitModel.setLocalScale(ratio * baseScale);
        }
    }

    private void adjustHabitableZoneScale(float baseScale) {
        if (lightModel != null && lightModel.habitableZoneOuter != null) {
            if (object.getStatus() instanceof Star star) {
                double[] innerOuter = star.estimateHabitableZone();
                float innerRatio = (float) (innerOuter[0] / object.getEquatorialRadius());
                float outerRatio = (float) (innerOuter[1] / object.getEquatorialRadius());
                lightModel.habitableZoneInner.setLocalScale(innerRatio * baseScale);
                lightModel.habitableZoneOuter.setLocalScale(outerRatio * baseScale);
            }
        }
    }

    public void setShowHabitableZone(boolean show) {
        if (lightModel != null) {
            boolean wasShow = lightModel.showHabitableZone;
            lightModel.showHabitableZone = show;
            if (wasShow != show) {
                if (show) {
                    if (lightModel.habitableZoneOuter == null) {
                        lightModel.habitableZoneInner = createTransparentSphere("habitableZoneInner " + object.getId(),
                                ColorRGBA.Red,
                                0.1f);
                        lightModel.habitableZoneOuter = createTransparentSphere("habitableZoneOuter " + object.getId(),
                                ColorRGBA.Green,
                                0.1f);
                    }
                    objectNode.attachChild(lightModel.habitableZoneInner);
                    objectNode.attachChild(lightModel.habitableZoneOuter);
                } else {
                    objectNode.detachChild(lightModel.habitableZoneInner);
                    objectNode.detachChild(lightModel.habitableZoneOuter);
                }
            }
        }
    }
}
