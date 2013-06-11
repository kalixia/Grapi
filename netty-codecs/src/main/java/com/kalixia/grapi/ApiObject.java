package com.kalixia.grapi;

import io.netty.buffer.ByteBuf;
import javax.ws.rs.core.MultivaluedMap;
import java.util.UUID;

public class ApiObject {
    private final UUID id;
    private final ByteBuf content;
    private final String contentType;
    private final MultivaluedMap<String, String> headers;

    public ApiObject(UUID id, ByteBuf content, String contentType, MultivaluedMap<String, String> headers) {
        this.id = id;
        this.content = content;
        this.contentType = contentType;
        this.headers = headers;
    }

    public UUID id() {
        return id;
    }

    public ByteBuf content() {
        return content;
    }

    public String contentType() {
        return contentType;
    }

    public MultivaluedMap<String, String> headers() {
        return headers;
    }
}
