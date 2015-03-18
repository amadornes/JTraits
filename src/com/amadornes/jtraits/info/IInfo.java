package com.amadornes.jtraits.info;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public interface IInfo extends Opcodes {

    public void visit(ClassWriter writer);

}
