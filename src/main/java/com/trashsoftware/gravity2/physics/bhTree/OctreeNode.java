package com.trashsoftware.gravity2.physics.bhTree;

import com.trashsoftware.gravity2.physics.CelestialObject;

public class OctreeNode {
    BoundingBox box;
    CelestialObject body = null;
    OctreeNode[] children = new OctreeNode[8];
    boolean isLeaf = true;

    double totalMass = 0;
    double[] centerOfMass = new double[3];

    public OctreeNode(BoundingBox box) {
        this.box = box;
    }

    public void insert(CelestialObject obj) {
        if (!box.contains(obj.getPosition())) return;

        if (body == null && isLeaf) {
            body = obj;
            totalMass = obj.getMass();
            centerOfMass = obj.getPosition().clone();
        } else {
            if (isLeaf) {
                // Subdivide and reinsert existing body
                isLeaf = false;
                CelestialObject oldBody = body;
                body = null;
                insert(oldBody);
            }

            // Insert into correct child
            for (int i = 0; i < 8; i++) {
                BoundingBox childBox = box.getOctant(i);
                if (children[i] == null) {
                    children[i] = new OctreeNode(childBox);
                }
                if (children[i].box.contains(obj.getPosition())) {
                    children[i].insert(obj);
                    break;
                }
            }

            // Update center of mass
            totalMass += obj.getMass();
            for (int i = 0; i < 3; i++) {
                centerOfMass[i] = (centerOfMass[i] * (totalMass - obj.getMass()) + obj.getPosition()[i] * obj.getMass()) / totalMass;
            }
        }
    }

    public double[] computeForce(CelestialObject obj, double G) {
        double theta = 0.5; // opening angle threshold
        double[] force = new double[3];

        if (isLeaf && body == obj) return force;

        double dx = centerOfMass[0] - obj.getPosition()[0];
        double dy = centerOfMass[1] - obj.getPosition()[1];
        double dz = centerOfMass[2] - obj.getPosition()[2];
        double dist = Math.sqrt(dx*dx + dy*dy + dz*dz) + 1e-5;

        double s = box.size;
        if (isLeaf || (s / dist < theta)) {
            double f = G * obj.getMass() * totalMass / (dist * dist + 1e-5);
            force[0] += f * dx / dist;
            force[1] += f * dy / dist;
            force[2] += f * dz / dist;
        } else {
            for (OctreeNode child : children) {
                if (child != null) {
                    double[] f = child.computeForce(obj, G);
                    for (int i = 0; i < 3; i++) {
                        force[i] += f[i];
                    }
                }
            }
        }

        return force;
    }
}
