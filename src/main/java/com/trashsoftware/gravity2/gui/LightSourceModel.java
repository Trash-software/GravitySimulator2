package com.trashsoftware.gravity2.gui;

import com.jme3.light.PointLight;
import com.jme3.post.filters.BloomFilter;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.shadow.EdgeFilteringMode;
import com.jme3.shadow.PointLightShadowRenderer;

public class LightSourceModel {

    protected PointLightShadowRenderer plsr;
    protected BloomFilter bloom;
    protected PointLight emissionLight;

    protected boolean showHabitableZone = false;
    protected Geometry habitableZoneInner;
    protected Geometry habitableZoneOuter;

    public void addThisTo(JmeApp jmeApp) {
        emissionLight = new PointLight();
        jmeApp.getRootNode().addLight(emissionLight);

        // Add shadow renderer
        plsr = new PointLightShadowRenderer(jmeApp.getAssetManager(),
                1024);
        plsr.setLight(emissionLight);
//                    plsr.setShadowIntensity(0.9f); // Adjust the shadow intensity
        plsr.setEdgeFilteringMode(EdgeFilteringMode.PCFPOISSON);
        jmeApp.getViewPort().addProcessor(plsr);

        // Add shadow filter for softer shadows
//                plsf = new PointLightShadowFilter(jmeApp.getAssetManager(), 1024);
//                plsf.setLight(emissionLight);
//                plsf.setEnabled(true);
//                jmeApp.filterPostProcessor.addFilter(plsf);

        // Add bloom effect to enhance the star's glow
        bloom = new BloomFilter(BloomFilter.GlowMode.Objects);
//                    bloom.setBloomIntensity(3f); // Adjust intensity for more or less glow
//                    bloom.setExposurePower(5f);
//                    bloom.setExposureCutOff(0.1f);
//                    bloom.setDownSamplingFactor(2f);
        jmeApp.filterPostProcessor.addFilter(bloom);
    }

    public boolean removeThisFrom(JmeApp jmeApp, String objId) {
        boolean changed = false;
        if (emissionLight != null) {
            jmeApp.getRootNode().removeLight(emissionLight);
            emissionLight = null;
            changed = true;
        }
        if (plsr != null) {
            jmeApp.getViewPort().removeProcessor(plsr);
            plsr = null;
            changed = true;
        }
        if (bloom != null) {
            try {
                jmeApp.filterPostProcessor.removeFilter(bloom);
            } catch (RuntimeException e) {
                System.err.println(objId + " has rendering problem: bloom.");
                e.printStackTrace(System.err);
            }
            bloom = null;
            changed = true;
        }

        return changed;
    }

    public boolean removeEffectLights(JmeApp jmeApp, String objId) {
        boolean changed = false;
        if (bloom != null) {
            try {
                jmeApp.filterPostProcessor.removeFilter(bloom);
            } catch (RuntimeException e) {
                System.err.println(objId + " has rendering problem: bloom.");
                e.printStackTrace(System.err);
            }
            bloom = null;
            changed = true;
        }
        return changed;
    }
}
