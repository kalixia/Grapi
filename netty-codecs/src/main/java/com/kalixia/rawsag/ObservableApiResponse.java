package com.kalixia.rawsag;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Observable;

import javax.ws.rs.core.MultivaluedMap;
import java.util.UUID;

public class ObservableApiResponse<T> extends ApiResponse {
    private final Observable<T> observable;

    public ObservableApiResponse(UUID id, HttpResponseStatus status, Observable<T> observable, String contentType,
                                 MultivaluedMap<String, String> headers) {
        super(id, status, Unpooled.EMPTY_BUFFER, contentType, headers);
        this.observable = observable;
    }

    public Observable<T> observable() {
        return observable;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ObservableApiResponse");
        sb.append("{id=").append(id());
        sb.append(", status=").append(status());
        sb.append(", observable=").append(observable());
        sb.append('}');
        return sb.toString();
    }
}
