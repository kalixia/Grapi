package com.kalixia.rawsag.codecs.websockets;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

/**
 * Models the content expected from the WebSockets clients.
 */
class WebSocketRequest {
    @JsonProperty
    private final UUID id;

    @JsonProperty
    private final String path;

    @JsonProperty
    private final String method;

    @JsonProperty
    private final String entity;

    @JsonCreator
    public WebSocketRequest(@JsonProperty("id") UUID id,
                            @JsonProperty("path") String path, @JsonProperty("method") String method,
                            @JsonProperty("entity") String entity) {
        this.id = id;
        this.path = path;
        this.method = method;
        this.entity = entity;
    }

    public UUID getId() {
        return id;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getEntity() {
        return entity;
    }
}
