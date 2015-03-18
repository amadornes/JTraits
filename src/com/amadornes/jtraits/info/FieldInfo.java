package com.amadornes.jtraits.info;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.FieldNode;

import com.amadornes.jtraits.ClassFactory;

public class FieldInfo implements IInfo {

    private ClassFactory<?> factory;
    private FieldNode field;
    private boolean overriden = false;

    public FieldInfo(ClassFactory<?> factory, FieldNode field) {

        this.factory = factory;
        this.field = field;
    }

    public FieldInfo(ClassFactory<?> factory, FieldNode field, boolean overriden) {

        this(factory, field);
        this.overriden = overriden;
    }

    public FieldNode getField() {

        return field;
    }

    public boolean override(FieldNode field) {

        if (field.name.equals(this.field.name) && field.desc.equals(this.field.desc)) {
            overriden = true;
            this.field = field;
            return true;
        }
        return false;
    }

    @Override
    public void visit(ClassWriter writer) {

        String className = overriden ? factory.traitType : factory.baseType;

        writer.visitSource(className.split("/")[className.split("/").length - 1] + ".java", null);

        writer.newField(factory.newType, field.name, field.desc);
        writer.visitField(ACC_PUBLIC, field.name, field.desc, field.signature, field.value);

        writer.visitEnd();
    }
}
