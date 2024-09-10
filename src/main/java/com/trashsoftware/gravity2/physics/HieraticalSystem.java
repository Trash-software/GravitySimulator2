package com.trashsoftware.gravity2.physics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HieraticalSystem implements AbstractObject {
    public final CelestialObject master;
    private double[] barycenter;
    private double[] barycenterV;
    private double systemMass;

    protected HieraticalSystem parent;
    private Set<HieraticalSystem> children;
    private int level;
    
    transient boolean visited;

    HieraticalSystem(CelestialObject master) {
        this.master = master;
    }

    void addChild(HieraticalSystem child) {
        if (children == null) children = new HashSet<>();
        children.add(child);
    }

    void updateRecursive(int depth) {
        // safety protection
        if (visited) return;
        visited = true;
        
        this.level = depth;
        
        if (children != null) {
            // remove those who no longer belongs to me or is destroyed
            children.removeIf(c -> c.parent != this || !c.master.isExist() || c.visited);
            if (children.isEmpty()) {
                // lonely again
                children = null;
                return;
            }
            
            int dim = master.position.length;
            barycenter = new double[dim];
            barycenterV = new double[dim];
            systemMass = master.mass;

            for (HieraticalSystem child : children) {
                child.updateRecursive(depth + 1);
                double[] childPos = child.getPosition();
                double[] childVel = child.getVelocity();
                double childMass = child.getMass();
                systemMass += childMass;
                for (int d = 0; d < dim; d++) {
                    barycenter[d] += childMass * childPos[d];
                    barycenterV[d] += childMass * childVel[d];
                }
            }
            for (int d = 0; d < dim; d++) {
                barycenter[d] += master.mass * master.position[d];
                barycenterV[d] += master.mass * master.velocity[d];
                barycenter[d] /= systemMass;
                barycenterV[d] /= systemMass;
            }
        }
    }

    public boolean isObject() {
        return children == null;
    }
    
    void sortByDistance(List<CelestialObject> hsList) {
        hsList.add(master);
        if (children != null) {
            List<HieraticalSystem> dtOrder = new ArrayList<>(children);
            dtOrder.sort((a, b) -> Double.compare(
                    VectorOperations.distance(a.getPosition(), getPosition()),
                    VectorOperations.distance(b.getPosition(), getPosition())
            ));
            for (HieraticalSystem sub : dtOrder) {
                sub.sortByDistance(hsList);
            }
        }
    }
    
    public List<HieraticalSystem> getChildrenSorted() {
        if (isObject()) return new ArrayList<>();
        var result = new ArrayList<>(children);
        result.sort((a, b) -> Double.compare(b.getMass(), a.getMass()));
        return result;
    }
    
    public int nChildren() {
        return children == null ? 0 : children.size();
    }

    public int getLevel() {
        return level;
    }

    @Override
    public HieraticalSystem getMaster() {
        return parent;
    }

    @Override
    public double getMass() {
        if (isObject()) return master.getMass();
        else return systemMass;
    }

    @Override
    public double[] getPosition() {
        if (isObject()) return master.getPosition();
        else return barycenter;
    }

    @Override
    public double[] getVelocity() {
        if (isObject()) return master.getVelocity();
        else return barycenterV;
    }

    @Override
    public String toString() {
        return "HieraticalSystem{" + master.getName() + "}";
    }
    
    public HieraticalSystem getDeepestHillMaster(double[] position) {
        // postorder traversal
        double dt = VectorOperations.distance(position, getPosition());
        if (dt < master.getHillRadius()) {
            if (!isObject()) {
                for (HieraticalSystem hs : children) {
                    HieraticalSystem childRes = hs.getDeepestHillMaster(position);
                    if (childRes != null) {
                        return childRes;
                    }
                }
            }
            return this;
        } else {
            return null;
        }
    }
}
