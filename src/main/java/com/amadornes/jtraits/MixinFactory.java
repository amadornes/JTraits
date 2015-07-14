package com.amadornes.jtraits;

public class MixinFactory {

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public static <T> Class<? extends T> mixin(Class<T> clazz, Class<?>... traits) {

        Class<T> c = clazz;
        for (Class<?> t : traits) {
            Mixin<T> mixin = (Mixin<T>) CustomClassLoader.instance.findMixin(c, t);
            if (mixin == null)
                mixin = new Mixin<T>(c, t);
            c = mixin.mixin();
        }

        return c;
    }

}
