package com.kalixia.grapi;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;

import javax.ws.rs.core.MultivaluedMap;
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
    private final MultivaluedMap<String, String> formParameters;
    private final MultivaluedMap<String, String> queryParameters;
    private final MultivaluedMap<String, String> headers;
    private final MultivaluedMap<String, String> cookies;
    private final String clientAddress;

    public ApiRequest(UUID id, String uri, HttpMethod method, ByteBuf content, String contentType,
                      MultivaluedMap<String, String> headers,
                      MultivaluedMap<String, String> formParameters,
                      MultivaluedMap<String, String> queryParameters,
                      MultivaluedMap<String, String> cookies,
                      String clientAddress) {
        super(id, content, contentType, headers);
        this.uri = uri;
        this.method = method;
        this.headers = headers;
        this.formParameters = formParameters;
        this.queryParameters = queryParameters;
        this.cookies = cookies;
        this.clientAddress = clientAddress;
    }

    public String uri() {
        return uri;
    }

    public HttpMethod method() {
        return method;
    }

    public String formParameter(String parameter) {
        return formParameters.getFirst(parameter);
    }

    public String queryParameter(String parameter) {
        return queryParameters.getFirst(parameter);
    }

    public String headerParameter(String parameter) {
        return headers.getFirst(parameter);
    }

    public String cookieParameter(String parameter) {
        return cookies.getFirst(parameter);
    }

    public String clientAddress() {
        return clientAddress;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder(70);
        sb.append("ApiRequest");
        sb.append("{id=").append(id());
        sb.append(", path='").append(uri()).append('\'');
        sb.append(", method=").append(method());
        sb.append(", headers=").append(method());
        sb.append(", clientAddress=").append(clientAddress());
        sb.append('}');
        return sb.toString();
    }
}
