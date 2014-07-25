package com.kalixia.grapi

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.codec.http.HttpResponseStatus

class HttpContentWithStatus {
    private final String content
    private final HttpResponseStatus status
    private final ObjectMapper objectMapper

    HttpContentWithStatus(String content, HttpResponseStatus status, ObjectMapper objectMapper) {
        this.status = status
        this.content = content
        this.objectMapper = objectMapper
    }

    /**
     * Parse the content of the response via Jackson
     * @param response
     * @param clazz
     * @return
     */
    def <T> T getJsonContent(Class<T> clazz) {
        return String.class.isAssignableFrom(clazz) ? content : objectMapper.readValue(content, clazz)
    }

    /**
     * Parse the content of the response via Jackson
     * @param response
     * @param clazz
     * @return
     */
    def <T> T getJsonContent(TypeReference<T> typeReference) {
        return objectMapper.readValue(content, typeReference)
    }

    String getContent() {
        return content
    }

    HttpResponseStatus getStatus() {
        return status
    }
}
