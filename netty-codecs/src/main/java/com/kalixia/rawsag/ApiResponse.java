package com.kalixia.rawsag;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Observable;

import java.util.UUID;

public class ApiResponse extends ApiObject {
    private final HttpResponseStatus status;

    public ApiResponse(UUID id, HttpResponseStatus status, ByteBuf content, String contentType) {
        super(id, content, contentType);
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
        sb.append(", content=").append(content());
        sb.append('}');
        return sb.toString();
    }

}
