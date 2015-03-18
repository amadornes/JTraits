package com.amadornes.jtraits;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.amadornes.jtraits.info.FieldInfo;
import com.amadornes.jtraits.info.IInfo;
import com.amadornes.jtraits.info.MethodInfo;

public class ClassFactory<T> {

    @SafeVarargs
    public static <T> Class<? extends T> createClass(Class<? extends T> base, Class<? extends ITrait>... traits) {

        Class<? extends T> c = base;
        for (Class<? extends ITrait> t : traits)
            c = createClass(c, t);

        return c;
    }

    private static <T> Class<? extends T> createClass(Class<? extends T> base, Class<? extends ITrait> trait) {

        return new ClassFactory<T>(base, trait).create();
    }

    // Info about the base class
    public final Class<? extends T> baseClass;
    public final ClassNode baseNode;
    public final String baseName;
    public final String baseType;

    // Info about the trait
    public final Class<? extends ITrait> traitClass;
    public final ClassNode traitNode;
    public final String traitName;
    public final String traitType;

    // Info about the class we're creating
    public final ClassWriter newWriter;
    public final String newName;
    public final String newType;

    // Other data needed to create the class
    public final String[] interfaces;

    public final boolean hasTraitInterface;
    public final String superName;

    public final List<IInfo> info = new ArrayList<IInfo>();

    private ClassFactory(Class<? extends T> base, Class<? extends ITrait> trait) {

        // Info about the base class
        baseClass = base;
        baseNode = ASMHelper.getClassNode(baseClass);
        baseName = baseClass.getName();
        baseType = Type.getInternalName(baseClass);

        // Info about the trait
        traitClass = trait;
        traitNode = ASMHelper.getClassNode(traitClass);
        traitName = traitClass.getName();
        traitType = Type.getInternalName(traitClass);

        // Info about the class we're creating
        newWriter = new ClassWriter(0);
        newName = baseName + "$$" + traitType.split("/")[traitType.split("/").length - 1];
        newType = newName.replace(".", "/");

        // Other data needed to create the class
        this.interfaces = ASMHelper.gatherInterfaces(new Class<?>[] { baseClass, traitClass }, ITrait.class);

        this.hasTraitInterface = ASMHelper.hasTraitInterface(trait);
        this.superName = hasTraitInterface ? "super" : "_super";

        gatherInfo();
    }

    public void gatherInfo() {

        for (MethodNode m : baseNode.methods)
            info.add(new MethodInfo(this, m));

        for (MethodNode m : traitNode.methods) {
            if (m.name.equals("<init>"))
                continue;
            if ((m.access & Opcodes.ACC_INTERFACE) != 0)
                continue;
            MethodInfo inf = null;
            for (IInfo i : info) {
                if (i instanceof MethodInfo && ((MethodInfo) i).override(m)) {
                    inf = (MethodInfo) i;
                    break;
                }
            }
            if (inf == null)
                info.add(new MethodInfo(this, m, true));
        }

        for (FieldNode f : baseNode.fields)
            if ((f.access & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC)
                info.add(new FieldInfo(this, f));

        for (FieldNode f : traitNode.fields) {
            if ((f.access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC)
                continue;
            FieldInfo inf = null;
            for (IInfo i : info) {
                if (i instanceof FieldInfo && ((FieldInfo) i).override(f)) {
                    inf = (FieldInfo) i;
                    break;
                }
            }
            if (inf == null)
                info.add(new FieldInfo(this, f, true));
        }
    }

    @SuppressWarnings("unchecked")
    public Class<? extends T> create() {

        newWriter.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, newType, null, baseType, interfaces);

        for (IInfo i : info)
            i.visit(newWriter);

        CustomClassLoader.instance.addBytecode(newName, newWriter.toByteArray());
        try {
            Class<? extends T> c = (Class<? extends T>) CustomClassLoader.instance.findClass(newName);
            return c;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
