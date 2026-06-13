package com.trashsoftware.gravity2.gui;

import com.jme3.effect.ParticleEmitter;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Quad;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.trashsoftware.gravity2.physics.DustObject;

public class DustModel extends ObjectModel {
    protected final DustObject object;
    protected final NebulaEmitter nebulaEmitter;
    protected ParticleEmitter nebulaParticleEmitter;

    public DustModel(DustObject object, JmeApp jmeApp) {
        super(object, jmeApp);

        this.object = object;

//        Node nebula = new Node("Nebula");

//        // Example: one dust layer
//        Sphere sphere = new Sphere(32, 64, (float) object.getAverageRadius());
//        Geometry dust = new Geometry(object.getId(), sphere);
//
//        Material mat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");
//        mat.setTexture("DiffuseMap", jmeApp.getAssetManager().loadTexture("com/trashsoftware/gravity2/effects/Smoke.png"));
//        mat.setBoolean("UseMaterialColors", true);
//        mat.setColor("Diffuse", new ColorRGBA(0.6f, 0.4f, 1.0f, 0.35f));
//        mat.setColor("Ambient", new ColorRGBA(0.2f, 0.15f, 0.4f, 0.35f));
//        mat.setColor("Specular", ColorRGBA.Black);
//        mat.setFloat("Shininess", 1f);
//
//        // Transparency
//        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
////        mat.getAdditionalRenderState().setDepthWrite(false); // often better for transparent clouds
//
//        dust.setMaterial(mat);
//
//        dust.setQueueBucket(RenderQueue.Bucket.Transparent);
//        dust.setLocalTranslation(0, 0, -30);
////        dust.lookAt(cam.getLocation(), Vector3f.UNIT_Y); // optional billboard behavior
//
////        nebula.attachChild(dust);
//        model = dust;
//        
//        rotatingNode.attachChild(model);

//        Material nebulaMat = createNebulaMaterial();
//
//        Node nebula = createNebula(
//                new Vector3f(0, 0, -80),
//                (float) object.getMajorRadius(),
//                20,
//                nebulaMat,
//                15,  // depend on texture png
//                1
//        );
//
//        rotatingNode.attachChild(nebula);
//
//        for (Spatial child : nebula.getChildren()) {
//            child.lookAt(jmeApp.getCamera().getLocation(), Vector3f.UNIT_Z);
//            child.rotate(0, 0, 0);
//        }

        nebulaEmitter = new NebulaEmitter(
                jmeApp.getAssetManager(),
                object.getId(),
                new Vector3f(0, 0, 0),
                (float) object.getMajorRadius(),
                20,
                "com/trashsoftware/gravity2/effects/Smoke.png",
                15,
                1
        );
        nebulaEmitter.updateNebula(object.getMajorRadius(), object.getDensity());

        nebulaParticleEmitter = nebulaEmitter.getEmitter();
        rotatingNode.attachChild(nebulaParticleEmitter);

        updateLightSource();
    }
    
    private Material createNebulaMaterial() {
        Material mat = new Material(jmeApp.getAssetManager(), "Common/MatDefs/Light/Lighting.j3md");

        Texture tex = jmeApp.getAssetManager().loadTexture("com/trashsoftware/gravity2/effects/Smoke.png");
        mat.setTexture("DiffuseMap", tex);

        mat.setBoolean("UseMaterialColors", true);
        mat.setColor("Diffuse", new ColorRGBA(0.8f, 0.8f, 1.0f, 0.35f));
        mat.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.4f, 0.35f));
        mat.setColor("Specular", ColorRGBA.Black);
        mat.setFloat("Shininess", 1f);
        
        mat.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        mat.getAdditionalRenderState().setDepthWrite(false);
        
        return mat;
    }

    private Quad createSpriteQuad(float size, int cols, int rows, int frameIndex) {
        Quad quad = new Quad(size, size);

        int col = frameIndex % cols;
        int row = frameIndex / cols;

        float u0 = (float) col / cols;
        float u1 = (float) (col + 1) / cols;

        // jME texture V coordinate usually goes bottom to top,
        // so flip the row.
        float v1 = 1.0f - (float) row / rows;
        float v0 = 1.0f - (float) (row + 1) / rows;

        Vector2f[] texCoords = new Vector2f[]{
                new Vector2f(u0, v0),
                new Vector2f(u1, v0),
                new Vector2f(u0, v1),
                new Vector2f(u1, v1)
        };

        quad.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));
        quad.updateBound();

        return quad;
    }

    private Vector3f randomPointInsideSphere(float radius) {
        Vector3f p;

        do {
            p = new Vector3f(
                    FastMath.nextRandomFloat() * 2f - 1f,
                    FastMath.nextRandomFloat() * 2f - 1f,
                    FastMath.nextRandomFloat() * 2f - 1f
            );
        } while (p.lengthSquared() > 1f);

        return p.multLocal(radius);
    }

    public Node createNebula(
            Vector3f center,
            float radius,
            int density,
            Material mat,
            int cols,
            int rows
    ) {
        Node nebula = new Node("Nebula");
        nebula.setLocalTranslation(center);

        for (int i = 0; i < density; i++) {
            Vector3f localPos = randomPointInsideSphere(radius);

            float layerSize = radius * FastMath.nextRandomFloat() * 0.8f + radius * 0.4f;

            int frame = FastMath.nextRandomInt(0, cols * rows - 1);
            Quad quad = createSpriteQuad(layerSize, cols, rows, frame);

            Geometry layer = new Geometry("NebulaLayer_" + i, quad);
            layer.setMaterial(mat);
            layer.setQueueBucket(RenderQueue.Bucket.Transparent);

            layer.setLocalTranslation(localPos);

            // Random initial rotation
            layer.rotate(
                    FastMath.nextRandomFloat() * FastMath.TWO_PI,
                    FastMath.nextRandomFloat() * FastMath.TWO_PI,
                    FastMath.nextRandomFloat() * FastMath.TWO_PI
            );

            nebula.attachChild(layer);
        }

        return nebula;
    }

    @Override
    public void notifyObjectChanged() {

    }

    @Override
    protected void updateLightSource() {

    }

    @Override
    protected void updateModelScale(double scale) {
        double baseScale = object.getMajorRadius() / initialRadius;

        double radiusScale = scale * baseScale;
        if (object.getMajorRadius() * scale < 0.1) {
            radiusScale = 0.1 / object.getMajorRadius();
        }
        nebulaEmitter.updateNebula(object.getMajorRadius(), object.getDensity());
        rotatingNode.setLocalScale((float) radiusScale);
    }

    @Override
    protected void updateRelatedPosAndScale(double scale, Vector3f xyz) {

    }
}
