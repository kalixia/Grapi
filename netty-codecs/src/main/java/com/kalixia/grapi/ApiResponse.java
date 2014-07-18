package com.kalixia.grapi;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.glassfish.jersey.internal.util.collection.ImmutableMultivaluedMap;

import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.UUID;

public class ApiResponse extends ApiObject {
    private final HttpResponseStatus status;

    public ApiResponse(UUID id, HttpResponseStatus status, ByteBuf content, String contentType) {
        super(id, content, contentType, new ImmutableMultivaluedMap<>(new MultivaluedHashMap<String, String>()));
        this.status = status;
    }

    public ApiResponse(UUID id, HttpResponseStatus status, ByteBuf content, String contentType,
                       MultivaluedMap<String, String> headers) {
        super(id, content, contentType, headers);
        this.status = status;
    }

    public HttpResponseStatus status() {
        return status;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ApiResponse");
        sb.append("{id=").append(id());
        sb.append(", status=").append(status());
        sb.append(", headers=").append(headers());
        sb.append('}');
        return sb.toString();
    }

}
