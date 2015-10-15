package com.amadornes.jtraits;

import static org.objectweb.asm.Opcodes.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class Mixin<T> {

    public static final String getName(Class<?> clazz, Class<?> trait) {

        return Type.getInternalName(clazz) + "$$" + trait.getSimpleName();
    }

    private String parentType;
    private ClassNode parentNode;
    private Class<T> parentClass;

    private String traitType;
    private ClassNode traitNode;

    private String newType;
    private String castType;
    private Class<T> result;

    private String[] parents;

    public Mixin(Class<T> clazz, Class<?> trait) {

        parentType = Type.getInternalName(clazz);
        parentNode = ASMUtils.getClassNode(clazz);
        parentClass = clazz;

        traitType = Type.getInternalName(trait);
        traitNode = ASMUtils.getClassNode(trait);

        newType = getName(clazz, trait);
        castType = newType;

        parents = ASMUtils.recursivelyFindClasses(this);
    }

    @SuppressWarnings("unchecked")
    public Class<T> mixin() {

        if (result != null)
            return result;

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        writer.visit(V1_6, ACC_PUBLIC, newType, null, parentType, traitNode.interfaces.toArray(new String[traitNode.interfaces.size()]));
        writer.visitSource(traitType.substring(traitType.lastIndexOf("/") + 1) + ".java", null);

        transferParentFields(writer);
        transferTraitFields(writer);

        bridgeMethods(writer);

        return result = (Class<T>) ClassLoadingHelper.instance.addMixin(newType.replace('/', '.'), writer.toByteArray(), this);
    }

    private void transferParentFields(ClassWriter writer) {

        for (FieldNode f : parentNode.fields) {
            if (f.name.equals("_super"))
                continue;
            FieldVisitor v = writer.visitField(ACC_PUBLIC, f.name, f.desc, null, f.value);
            if (f.visibleAnnotations != null) {
                for (AnnotationNode a : f.visibleAnnotations) {
                    if (a.values == null)
                        continue;
                    AnnotationVisitor av = v.visitAnnotation(a.desc, true);
                    Iterator<Object> it = a.values.iterator();
                    while (it.hasNext()) {
                        String key = (String) it.next();
                        Object val = it.next();
                        try {
                            if (val instanceof Object[]
                                    && !(val instanceof byte[] || val instanceof boolean[] || val instanceof short[]
                                            || val instanceof char[] || val instanceof int[] || val instanceof long[]
                                            || val instanceof float[] || val instanceof double[])) {
                                av = av.visitArray(key);
                                int i = 0;
                                for (Object o : (Object[]) val) {
                                    av.visit("" + i, o);
                                    i++;
                                }
                            } else {
                                av.visit(key, val);
                            }
                        } catch (Exception ex) {
                            if (MixinFactory.debug)
                                new RuntimeException("Invalid key/value: " + key + " - " + val, ex).printStackTrace();
                        }
                    }
                }
            }
            v.visitEnd();
        }
    }

    private void transferTraitFields(ClassWriter writer) {

        for (FieldNode f : traitNode.fields) {
            if (f.name.equals("_super"))
                continue;
            FieldVisitor v = writer.visitField(ACC_PUBLIC, f.name, f.desc, null, f.value);
            if (f.visibleAnnotations != null) {
                for (AnnotationNode a : f.visibleAnnotations) {
                    AnnotationVisitor av = v.visitAnnotation(a.desc, true);
                    Iterator<Object> it = a.values.iterator();
                    while (it.hasNext())
                        av.visit((String) it.next(), it.next());
                }
            }
            v.visitEnd();
        }
    }

    private void bridgeMethods(ClassWriter writer) {

        for (MethodNode m : traitNode.methods) {
            MethodVisitor v = writer.visitMethod(ACC_PUBLIC | ACC_SYNTHETIC, m.name, m.desc, null, null);
            v.visitCode();

            ASMUtils.resetCopy(m.instructions);
            v.visitLabel(new Label());

            // Transfer parent node
            if (m.name.equals("<init>") || m.name.equals("<clinit>")) {
                v.visitVarInsn(ALOAD, 0);
                int index = 1;
                for (Type t : Type.getArgumentTypes(m.desc)) {
                    v.visitVarInsn(ALOAD, index);
                    index += t.getSize();
                }
                v.visitMethodInsn(INVOKESPECIAL, parentType, m.name, m.desc, false);
            }

            // Get matching method and transfer it if needed
            InsnList list = new InsnList();
            ListIterator<AbstractInsnNode> originalInsns = m.instructions.iterator();
            List<AbstractInsnNode> added = new ArrayList<AbstractInsnNode>();
            boolean supercall = false;
            while (originalInsns.hasNext()) {
                AbstractInsnNode node = originalInsns.next();
                AbstractInsnNode next = node.getNext(), prev = node.getPrevious();
                if (next != null && node instanceof VarInsnNode && ((VarInsnNode) node).var == 0 && next instanceof MethodInsnNode
                        && ((MethodInsnNode) next).name.equals("<init>"))
                    continue;
                if (prev != null && prev instanceof VarInsnNode && ((VarInsnNode) prev).var == 0 && node instanceof MethodInsnNode
                        && ((MethodInsnNode) node).name.equals("<init>"))
                    continue;

                int result = ASMUtils.addInstructionsWithSuperRedirections(node, added, supercall, this);
                if (result == 1)
                    supercall = true;
                else if (result == 2)
                    supercall = false;

                if (added.isEmpty()) {
                    ASMUtils.copyInsn(list, node);
                } else {
                    for (AbstractInsnNode n_ : added)
                        list.add(n_);
                    added.clear();
                }
            }
            list.accept(v);

            v.visitInsn(ASMUtils.getReturnCode(m.desc.substring(m.desc.lastIndexOf(")") + 1)));

            v.visitMaxs(m.maxStack + 1, m.maxLocals + 1);

            if (m.visibleAnnotations != null) {
                for (AnnotationNode a : m.visibleAnnotations) {
                    AnnotationVisitor av = v.visitAnnotation(a.desc, true);
                    Iterator<Object> it = a.values.iterator();
                    while (it.hasNext())
                        av.visit((String) it.next(), it.next());
                }
            }
            v.visitEnd();
        }
    }

    public String getParentType() {

        return parentType;
    }

    public ClassNode getParentNode() {

        return parentNode;
    }

    public Class<T> getParentClass() {

        return parentClass;
    }

    public String getTraitType() {

        return traitType;
    }

    public ClassNode getTraitNode() {

        return traitNode;
    }

    public String getNewType() {

        return newType;
    }

    public String getCastType() {

        return castType;
    }

    public String[] getParents() {

        return parents;
    }
}
