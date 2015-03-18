package com.amadornes.jtraits.info;

import java.util.Iterator;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import com.amadornes.jtraits.ClassFactory;
import com.amadornes.jtraits.NodeCopier;

public class MethodInfo implements IInfo {

    private ClassFactory<?> factory;
    private MethodNode method;
    private boolean overriden = false;

    public MethodInfo(ClassFactory<?> factory, MethodNode method) {

        this.factory = factory;
        this.method = method;
    }

    public MethodInfo(ClassFactory<?> factory, MethodNode method, boolean overriden) {

        this(factory, method);
        this.overriden = overriden;
    }

    public boolean override(MethodNode method) {

        if (method.name.equals(this.method.name) && method.desc.equals(this.method.desc)) {
            overriden = true;
            this.method = method;
            return true;
        }
        return false;
    }

    @Override
    public void visit(ClassWriter writer) {

        String className = overriden ? factory.traitType : factory.baseType;

        writer.visitSource(className.split("/")[className.split("/").length - 1] + ".java", null);

        writer.newMethod(factory.newType, method.name, method.desc, false);
        if (method.name.equals("<init>")) {
            MethodVisitor v = writer.visitMethod(ACC_PUBLIC, method.name, method.desc, null, method.exceptions.toArray(new String[] {}));
            v.visitCode();
            {
                v.visitVarInsn(ALOAD, 0);
                v.visitMethodInsn(INVOKESPECIAL, factory.baseType, method.name, method.desc, false);
                v.visitInsn(RETURN);
                v.visitMaxs(method.maxStack + 1, method.maxLocals + 2);
            }
            v.visitEnd();
        } else {
            MethodVisitor v = writer.visitMethod(ACC_PUBLIC, method.name, method.desc, null, method.exceptions.toArray(new String[] {}));
            v.visitCode();
            {
                Iterator<AbstractInsnNode> l = method.instructions.iterator();
                NodeCopier c = new NodeCopier(method.instructions);
                InsnList l2 = new InsnList();

                int skip = 0;
                while (l.hasNext()) {
                    AbstractInsnNode n = l.next();
                    if (skip > 0) {
                        skip--;
                        continue;
                    }
                    Pair<AbstractInsnNode, Integer> data = getNode(n);
                    if (data != null) {
                        if (data.getKey() != null)
                            c.copyTo(data.getKey(), l2);
                        skip = data.getValue();
                    }
                }

                l2.accept(v);

                v.visitMaxs(method.maxStack + 1, method.maxLocals + 2);
            }
            v.visitEnd();
        }
    }

    private Pair<AbstractInsnNode, Integer> getNode(AbstractInsnNode node) {

        if (node instanceof FieldInsnNode) {
            FieldInsnNode f = (FieldInsnNode) node;
            if (overriden && f.name.equals(factory.superName) && f.getOpcode() == GETFIELD) {
                AbstractInsnNode next = f.getNext();
                if (next.getOpcode() == CHECKCAST)
                    return new ImmutablePair<AbstractInsnNode, Integer>(null, 1);
            }
            if (f.owner.equals(factory.baseType) || f.owner.equals(factory.traitType))
                return new ImmutablePair<AbstractInsnNode, Integer>(new FieldInsnNode(f.getOpcode(), factory.newType, f.name, f.desc), 0);
        }

        return new ImmutablePair<AbstractInsnNode, Integer>(node, 0);
    }
}
