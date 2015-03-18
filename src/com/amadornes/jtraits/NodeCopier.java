package com.amadornes.jtraits;

import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;

import com.google.common.collect.Maps;

public class NodeCopier {

    private Map<LabelNode, LabelNode> labelMap = Maps.newHashMap();

    public NodeCopier(InsnList sourceList) {

        for (AbstractInsnNode instruction = sourceList.getFirst(); instruction != null; instruction = instruction.getNext())
            if (instruction instanceof LabelNode)
                labelMap.put(((LabelNode) instruction), new LabelNode());
    }

    public void copyTo(AbstractInsnNode node, InsnList destination) {

        if (node == null)
            return;

        if (destination == null)
            return;

        destination.add(node.clone(labelMap));
    }
}
