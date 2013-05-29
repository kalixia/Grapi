package com.kalixia.rawsag.codecs.jaxrs.converters;

import javax.ws.rs.ext.ParamConverter;
import java.util.List;

/**
 * A converter eases conversion from/to {@link String}.
 *
 * If data passed through {@link #fromString(String)} is invalid, the converter should throw
 * a {@link IllegalArgumentException}.
 */
public interface Converter<T> extends ParamConverter<T> {
    List<Class<T>> acceptClasses();
}
