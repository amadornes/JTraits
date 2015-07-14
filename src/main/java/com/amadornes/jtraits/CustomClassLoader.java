package com.amadornes.jtraits;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class CustomClassLoader {

    public static CustomClassLoader instance = new CustomClassLoader();

    private final Map<String, byte[]> bytecodes = new HashMap<String, byte[]>();
    private final Map<String, Mixin<?>> mixins = new HashMap<String, Mixin<?>>();

    private final ClassLoader classLoader = getClass().getClassLoader();
    private final Method m_defineClass;

    public CustomClassLoader() {

        try {
            m_defineClass = ClassLoader.class.getDeclaredMethod("defineClass", byte[].class, int.class, int.class);
            m_defineClass.setAccessible(true);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Class<?> addMixin(String name, byte[] bytecode, Mixin<?> mixin) {

        bytecodes.put(name, bytecode);
        mixins.put(name, mixin);
        try {
            return (Class<?>) m_defineClass.invoke(classLoader, bytecode, 0, bytecode.length);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Mixin<?> findMixin(String name) {

        if (mixins.containsKey(name))
            return mixins.get(name);

        return null;
    }

    public Mixin<?> findMixin(Class<?> clazz, Class<?> trait) {

        return findMixin(Mixin.getName(clazz, trait).replace("/", "."));
    }

    public InputStream getResourceAsStream(String name) {

        for (String s : bytecodes.keySet())
            if ((s.replace(".", "/") + ".class").equals(name))
                return new ByteArrayInputStream(bytecodes.get(s));

        return classLoader.getResourceAsStream(name);
    }

}
