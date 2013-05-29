package com.kalixia.rawsag;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class ApiObject {
    private final UUID id;
    private final ByteBuf content;
    private final String contentType;

    public ApiObject(UUID id, ByteBuf content, String contentType) {
        this.id = id;
        this.content = content;
        this.contentType = contentType;
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
}
