package com.trashsoftware.gravity2.physics;

import java.util.*;

public class HieraticalSystem implements AbstractObject {
    public final CelestialObject master;
    private double[] barycenter;
    private double[] barycenterV;
    private double systemMass;

    protected HieraticalSystem parent;
    private Set<HieraticalSystem> children;
    private int level;
    
    transient boolean visited;
    transient SystemStats curStats;

    HieraticalSystem(CelestialObject master) {
        this.master = master;
    }
    
    public boolean isRoot() {
        return parent == null;
    }

    void addChild(HieraticalSystem child) {
        if (children == null) children = new HashSet<>();
        children.add(child);
    }

    void updateRecursive(int depth) {
        // safety protection
        if (visited) return;
        visited = true;
        curStats = null;
        
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
        return children == null || children.isEmpty();  // children shouldn't be empty if update correctly
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
    
    public double bindingEnergyOf(AbstractObject child, Simulator simulator) {
        // assume "child" is a child of this system and is not the only of the system
        double remMass = systemMass - child.getMass();
        assert remMass > 0;
        double[] remBarycenter = barycenter.clone();
        double[] childPos = child.getPosition();

        int dim = master.position.length;
        // Compute the barycenterAB using the formula
        for (int i = 0; i < dim; i++) {
            remBarycenter[i] = (systemMass * barycenter[i] - child.getMass() * childPos[i]) / remMass;
        }
//        System.out.println(Arrays.toString(barycenter) + " " + Arrays.toString(remBarycenter));
        
        double[] relV = VectorOperations.subtract(child.getVelocity(), barycenterV);
        double kinetic = 0.5 * child.getMass() * Math.pow(VectorOperations.magnitude(relV), 2);
        double potential = Simulator.potentialEnergyBetween(remMass, child.getMass(), 
                VectorOperations.distance(childPos, remBarycenter), 
                simulator.getG(),
                simulator.getGravityDtPower());
//        System.out.println(kinetic + " " + potential);
        return kinetic + potential;
    }
    
    private Set<HieraticalSystem> computeSystemStats(Simulator simulator) {
        if (isObject()) {
            curStats = new SystemStats(1, getMass());
            return Set.of();
        } else {
            curStats = new SystemStats(1, master.getMass());
            Set<HieraticalSystem> thisUnhandled = new HashSet<>();
            for (HieraticalSystem child : children) {
                // recursive call
                Set<HieraticalSystem> childUnhandled = child.computeSystemStats(simulator);
                
                // check if child system escape
                double bindEnergy = bindingEnergyOf(child, simulator);
                if (bindEnergy < 0) {
                    if (child.curStats == null) {
                        // fixme: this is a bug
                        // fixme: maybe closed-loop recursion
                        System.err.println(child.master.getName() + "'s sub system is null");
                        curStats.nClosedObjectInSystem += 1;
                        curStats.circlingMass += child.getMass();
                    } else {
                        // child system not escape
                        // any orbiting children of child should also not escape
                        curStats.nClosedObjectInSystem += child.curStats.nClosedObjectInSystem;
                        curStats.circlingMass += child.curStats.circlingMass;
                    }
                } else {
                    // child escape
                    // note: if child escape, child's child escape from child but be caught by this,
                    // should be in "childUnhandled"
                    thisUnhandled.add(child);
                }
                
                // check if something escape from children will not escape this
                for (var cui = childUnhandled.iterator(); cui.hasNext();) {
                    HieraticalSystem cu = cui.next();
                    double bindEnergyChild = bindingEnergyOf(cu, simulator);
                    if (bindEnergyChild < 0) {
                        curStats.nClosedObjectInSystem += cu.curStats.nClosedObjectInSystem;
                        curStats.circlingMass += cu.curStats.circlingMass;
                        cui.remove();
                    }
                }
                thisUnhandled.addAll(childUnhandled);
            }
            return thisUnhandled;
        }
    }

    public SystemStats getCurStats(Simulator simulator) {
        if (curStats == null) {
            computeSystemStats(simulator);
        }
        return curStats;
    }

    public int nChildren() {
        return children == null ? 0 : children.size();
    }

    /**
     * @return the hieratical level of this, 0 is universe
     */
    public int getTrueLevel() {
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
    
    public static class SystemStats {
        private int nClosedObjectInSystem;  // recursive, include self
        private double circlingMass;
        
        SystemStats(int nClosedObjectInSystem, double circlingMass) {
            this.nClosedObjectInSystem = nClosedObjectInSystem;
            this.circlingMass = circlingMass;
        }

        public double getCirclingMass() {
            return circlingMass;
        }

        public int getNClosedObject() {
            return nClosedObjectInSystem;
        }
    }
}
