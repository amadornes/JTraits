package com.amadornes.jtraits;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

public class ASMHelper {

    public static ClassNode getClassNode(Class<?> clazz) {

        try {
            ClassNode cnode = new ClassNode();
            ClassReader reader = new ClassReader(clazz.getClassLoader().getResourceAsStream(clazz.getName().replace(".", "/") + ".class"));
            reader.accept(cnode, 0);

            return cnode;
        } catch (IOException ignore) {
            ignore.printStackTrace();
            return null;
        }
    }

    public static String[] gatherInterfaces(Class<?>[] classes, Class<?>... excluded) {

        List<String> l = new ArrayList<String>();

        for (Class<?> c : classes)
            for (String s : gatherInterfacesAsList(c))
                if (!l.contains(s))
                    l.add(s);

        for (Class<?> c : excluded)
            l.remove(Type.getInternalName(c));

        return l.toArray(new String[l.size()]);
    }

    private static List<String> gatherInterfacesAsList(Class<?> clazz) {

        List<String> l = new ArrayList<String>();

        for (Class<?> i : clazz.getInterfaces())
            l.add(Type.getInternalName(i));

        if (clazz != Object.class)
            for (String s : gatherInterfacesAsList(clazz.getSuperclass()))
                if (!l.contains(s))
                    l.add(s);

        return l;
    }

    public static boolean hasTraitInterface(Class<?> clazz) {

        if (clazz != Object.class) {
            if (clazz.getSuperclass() == JTrait.class)
                return false;
            if (!hasTraitInterface(clazz.getSuperclass()))
                return false;
        }

        return true;
    }

}
