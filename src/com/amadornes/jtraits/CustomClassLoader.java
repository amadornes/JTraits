package com.amadornes.jtraits;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class CustomClassLoader extends ClassLoader {

    public static CustomClassLoader instance = new CustomClassLoader();

    private final Map<String, byte[]> bytecodes = new HashMap<String, byte[]>();
    private final Map<String, Class<?>> registered = new HashMap<String, Class<?>>();

    private CustomClassLoader() {

        super(CustomClassLoader.class.getClassLoader());
    }

    public void addBytecode(String name, byte[] bytecode) {

        bytecodes.put(name, bytecode);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {

        if (registered.containsKey(name))
            return registered.get(name);

        try {
            return super.findClass(name);
        } catch (Exception ex) {
        }

        byte[] bytecode = bytecodes.get(name);
        if (bytecode != null) {
            try {
                Class<?> c = defineClass(name, bytecode, 0, bytecode.length);
                registered.put(name, c);
                return c;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return super.findClass(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {

        for (String s : registered.keySet())
            if ((s.replace(".", "/") + ".class").equals(name))
                return new ByteArrayInputStream(bytecodes.get(s));

        return super.getResourceAsStream(name);
    }

}
