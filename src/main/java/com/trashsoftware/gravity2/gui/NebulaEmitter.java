package com.trashsoftware.gravity2.gui;

import com.jme3.asset.AssetManager;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.trashsoftware.gravity2.physics.DustObject;
import com.trashsoftware.gravity2.utils.Util;

public class NebulaEmitter {

    private EmitterSphereShape shape;
    private final ParticleEmitter emitter;
    //        private final float radius;
    private final int particleCount;

    public NebulaEmitter(
            AssetManager assetManager,
            String name,
            Vector3f position,
            float initRadius,
            int particleCount,
            String texturePath,
            int imagesX,
            int imagesY
    ) {
//        this.radius = radius;
        this.particleCount = particleCount;

        emitter = new ParticleEmitter(
                name,
                ParticleMesh.Type.Triangle,
                particleCount
        );

        emitter.setLocalTranslation(position);

        // Important: particles move together with the emitter
        emitter.setInWorldSpace(false);

        // Emit from inside a sphere
        // fixme: 半径无法后期改变，没有修好！
        shape = new EmitterSphereShape(Vector3f.ZERO, initRadius * 0.75f);
        emitter.setShape(shape);

        // Sprite sheet settings, e.g. 2x2 texture grid
        emitter.setImagesX(imagesX);
        emitter.setImagesY(imagesY);
        emitter.setSelectRandomImage(true);

        // Long-lasting soft particles
        emitter.setLowLife(18f);
        emitter.setHighLife(30f);

        // Keep approximately fixed visible particle count.
        // At steady state: visible particles ≈ particlesPerSec * averageLife
        float averageLife = (18f + 30f) * 0.5f;
        emitter.setParticlesPerSec(particleCount / averageLife);

        // No gravity for space nebula
        emitter.setGravity(0, 0, 0);

        // Slow drifting motion
        emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0.03f, 0));
        emitter.getParticleInfluencer().setVelocityVariation(0.4f);

        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Particle.j3md");
        mat.setTexture("Texture", assetManager.loadTexture(texturePath));

        emitter.setMaterial(mat);

        // Transparent rendering
        emitter.setQueueBucket(RenderQueue.Bucket.Transparent);

        // Fill the nebula immediately instead of waiting for particles to appear slowly
        emitter.emitAllParticles();
    }

    public ParticleEmitter getEmitter() {
        return emitter;
    }

    public void setPosition(Vector3f position) {
        emitter.setLocalTranslation(position);
    }

    public void move(Vector3f delta) {
        emitter.move(delta);
    }

    public void remove() {
        emitter.removeFromParent();
    }

//        public float getRadius() {
//            return radius;
//        }

    public int getParticleCount() {
        return particleCount;
    }

    public void setAlpha(float alpha) {
        emitter.setStartColor(new ColorRGBA(0.5f, 0.5f, 0.5f, alpha));
        emitter.setEndColor(new ColorRGBA(0.5f, 0.5f, 0.5f, alpha));
    }

    public void updateNebula(double radius, double density) {
        shape.setRadius((float) radius * 0.75f);
        emitter.setShape(shape);
        emitter.setStartSize((float) (radius * 0.45));
        emitter.setEndSize((float) (radius * 0.9));

        double alpha = Util.linearMapping(DustObject.MINIMUM_DENSITY, DustObject.MAXIMUM_DENSITY,
                0.01, 0.25,
                density);
        setAlpha((float) alpha);

        emitter.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 0.03f, 0));
        emitter.getParticleInfluencer().setVelocityVariation(0.4f);
    }
}
