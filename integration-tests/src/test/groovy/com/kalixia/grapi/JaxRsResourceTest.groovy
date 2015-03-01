package com.kalixia.grapi

import com.kalixia.grapi.tests.DataHolder
import com.kalixia.grapi.tests.TestModule
import dagger.ObjectGraph
import groovy.util.logging.Slf4j
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelPipeline
import io.netty.handler.codec.http.*
import io.netty.util.CharsetUtil
import io.reactivex.netty.RxNetty
import io.reactivex.netty.pipeline.PipelineConfigurator
import io.reactivex.netty.pipeline.PipelineConfigurators
import io.reactivex.netty.protocol.http.client.HttpClient
import io.reactivex.netty.protocol.http.client.HttpClientRequest
import io.reactivex.netty.protocol.http.client.HttpClientResponse
import io.reactivex.netty.protocol.http.server.HttpServer
import io.reactivex.netty.protocol.http.server.HttpServerRequest
import io.reactivex.netty.protocol.http.server.HttpServerResponse
import io.reactivex.netty.protocol.http.server.RequestHandler
import rx.Observable
import rx.functions.Action1
import rx.functions.Func1
import rx.functions.Func2
import spock.lang.Shared
import spock.lang.Specification

import static io.netty.handler.codec.http.HttpMethod.GET

@Slf4j("LOGGER")
abstract class JaxRsResourceTest extends Specification {
    @Shared
    Integer serverPort = 33333

    @Shared
    HttpServer<ApiRequest, ApiResponse> server;

    @Shared
    HttpClient httpClient = RxNetty.createHttpClient("localhost", serverPort)

    @Shared
    DataHolder dataHolder

    def setupSpec() {
        // setup dagger
        ObjectGraph objectGraph = ObjectGraph.create(new TestModule());
        dataHolder = objectGraph.get(DataHolder.class)
        // start Netty server serving 'HelloResource' JAX-RS resource generated code
        startServer(dataHolder)
    }

    def cleanupSpec() {
        // stop Netty server
        if (server != null)
            server.shutdown()
    }

    def request(String requestURL) {
        return request(requestURL, GET)
    }

    def request(String requestURL, HttpMethod method) {
        return buildHttpContentWithStatusFromObservable(httpClient.submit(HttpClientRequest.create(method, requestURL)))
    }

    def requestWithHeaders(String requestURL, HttpMethod method, Map<String, String> headers) {
        def request = HttpClientRequest.create(method, requestURL)
        headers.each { key, value -> request = request.withHeader(key, value) }
        return buildHttpContentWithStatusFromObservable(httpClient.submit(request))
    }

    def requestWithQueryParams(String requestURL, HttpMethod method, Map<String, String> queryParams) {
        QueryStringEncoder enc = new QueryStringEncoder(requestURL)
        queryParams.each { key, value -> enc.addParam(key, value )}
        def request = HttpClientRequest.create(method, enc.toString())
        return buildHttpContentWithStatusFromObservable(httpClient.submit(request))
    }

    def requestWithCookies(String requestURL, HttpMethod method, Map<String, String> cookies) {
        def request = HttpClientRequest.create(method, requestURL)
        cookies.each { key, value -> request = request.withCookie(new DefaultCookie(key, value)) }
        return buildHttpContentWithStatusFromObservable(httpClient.submit(request))
    }

    def requestWithJacksonBody(String requestURL, HttpMethod method, Object body) {
        LOGGER.debug("Requesting [$method] $requestURL...")
        def request = HttpClientRequest.create(method, requestURL)
        if (body != null) {
            request = request.withContent(json(body))
        }
        return buildHttpContentWithStatusFromObservable(httpClient.submit(request))
    }

    def String json(Object o) {
        return dataHolder.objectMapper.writeValueAsString(o)
    }

    def startServer(DataHolder dataHolder) {
        PipelineConfigurator<?, ?> pipelineConfigurator =
                PipelineConfigurators.<ByteBuf, ByteBuf> httpServerConfigurator()
        PipelineConfigurator<?, ?> grapiPipelineConfigurator =
                new PipelineConfigurator<HttpRequest, HttpResponse>() {
                    @Override
                    void configureNewPipeline(ChannelPipeline pipeline) {
//                        pipeline.addLast("shiro", new ShiroHandler(securityManager))
                        pipeline.addLast("api-protocol-switcher", dataHolder.apiProtocolSwitcher)
                        pipeline.addLast("jax-rs-jaxRsHandler", dataHolder.jaxRsHandler)
                    }
                }
        pipelineConfigurator = PipelineConfigurators.composeConfigurators(pipelineConfigurator, grapiPipelineConfigurator)
        server = RxNetty.newHttpServerBuilder(serverPort, new RequestHandler<ApiRequest, ApiResponse>() {
            @Override
            Observable<Void> handle(HttpServerRequest<ApiRequest> req, HttpServerResponse<ApiResponse> resp) {
                return resp.close(false)
            }
        }).pipelineConfigurator(pipelineConfigurator).build();
        server.start()
    }

    def buildHttpContentWithStatusFromObservable(Observable<HttpClientResponse<ByteBuf>> obs) {
        return obs
                .flatMap(
                    { HttpClientResponse<ByteBuf> response ->
                        response.getContent().defaultIfEmpty(Unpooled.EMPTY_BUFFER)
                    } as Func1<HttpClientResponse<ByteBuf>, Observable<ByteBuf>>,
                    { HttpClientResponse<ByteBuf> response, ByteBuf buffer ->
                        def status = response.getStatus()
                        def content = buffer.toString(CharsetUtil.UTF_8)
                        return new HttpContentWithStatus(content, status, dataHolder.objectMapper)
                    }
                    as Func2<HttpClientResponse<ByteBuf>, ByteBuf, HttpContentWithStatus>
                )
                .doOnError({ LOGGER.error "Unexpected error", it } as Action1)
                .toBlocking().last()
    }
}