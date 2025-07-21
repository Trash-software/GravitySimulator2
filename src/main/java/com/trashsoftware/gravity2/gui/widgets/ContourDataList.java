package com.trashsoftware.gravity2.gui.widgets;

import com.trashsoftware.gravity2.physics.EffectivePotential;

import java.util.ArrayList;

public class ContourDataList extends ArrayList<ContourData> {
    
    private EffectivePotential ep;

    public ContourDataList() {
        this(null);
    }
    
    public ContourDataList(EffectivePotential ep) {
        super();
        
        this.ep = ep;
    }

    public EffectivePotential getEp() {
        return ep;
    }

    public void setEp(EffectivePotential ep) {
        this.ep = ep;
    }
}
