package com.kalixia.rawsag.codecs.jaxrs.converters;

import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converters {
    private static Map<Class<?>, Converter> convertersMap;

    static {
        List<? extends Converter> converters = Arrays.asList(
            new UUIDConverter()
        );

        convertersMap = new HashMap<>();
        for (Converter converter : converters) {
            @SuppressWarnings("unchecked")
            List<Class> acceptedClasses = converter.acceptClasses();
            for (Class clazz : acceptedClasses) {
                convertersMap.put(clazz, converter);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromString(Class<T> clazz, String value) throws ConverterNotFoundException {
        Converter<T> converter = convertersMap.get(clazz);
        if (converter == null) {
            throw new ConverterNotFoundException(clazz);
        }
        return converter.fromString(value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T fromByteBuf(Class<T> clazz, ByteBuf buffer) throws ConverterNotFoundException {
        return fromString(clazz, buffer.toString());
    }

    @SuppressWarnings("unchecked")
    public static String toString(Object value) throws ConverterNotFoundException {
        Converter converter = convertersMap.get(value.getClass());
        if (converter == null) {
            throw new ConverterNotFoundException(value.getClass());
        }
        return converter.toString(value);
    }
}
