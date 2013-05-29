package com.kalixia.rawsag;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;

import java.util.UUID;

/**
 * Request to the API.
 *
 * Created from either the REST API or the WebSockets API.
 *
 * This class is intentionally immutable.
 */
public class ApiRequest extends ApiObject {
    private final String uri;
    private final HttpMethod method;
    private final String clientAddress;

    public ApiRequest(UUID id, String uri, HttpMethod method, ByteBuf content, String contentType, String clientAddress) {
        super(id, content, contentType);
        this.uri = uri;
        this.method = method;
        this.clientAddress = clientAddress;
    }

    public String uri() {
        return uri;
    }

    public HttpMethod method() {
        return method;
    }

    public String clientAddress() {
        return clientAddress;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ApiRequest");
        sb.append("{id=").append(id());
        sb.append(", path='").append(uri()).append('\'');
        sb.append(", method=").append(method());
        sb.append(", content=").append(content());
        sb.append(", clientAddress=").append(clientAddress());
        sb.append('}');
        return sb.toString();
    }
}
