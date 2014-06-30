package com.kalixia.grapi.codecs.hystrix.metrics.eventstream;

import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixThreadPoolMetrics;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;
import rx.subscriptions.MultipleAssignmentSubscription;

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static com.kalixia.grapi.codecs.hystrix.metrics.eventstream.JsonMappers.toJson;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.PRAGMA;
import static io.netty.handler.codec.http.HttpHeaders.Values.NO_CACHE;

/**
 * Handler exposing Hystrix Metrics like the servlet available in <code>hystrix-metrics-event-stream</code> Servlet.
 *
 * @see com.netflix.hystrix.contrib.rxnetty.metricsstream.HystrixMetricsStreamHandler
 */
@ChannelHandler.Sharable
public class HystrixMetricsStreamHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    private final String urlMapping;
    private final long interval;
    private final HttpContent PING;
    private final ByteBuf BEGIN_DATA;
    private final ByteBuf END_DATA;
    private static final Logger logger = LoggerFactory.getLogger(HystrixMetricsStreamHandler.class);

    /**
     * Create a new handler emitting SSE events if a request is made on <code>urlMapping</code> URL.
     * @param urlMapping the path the handler will look for beginning to emit SSE events,
     *                   otherwise ignore HTTP messages
     * @param interval   interval between publication of events
     * @throws UnsupportedEncodingException
     */
    public HystrixMetricsStreamHandler(String urlMapping, long interval) throws UnsupportedEncodingException {
        this.urlMapping = urlMapping;
        this.interval = interval;
        PING = new DefaultHttpContent(Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("ping: \n".getBytes("UTF-8"))));
        BEGIN_DATA = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("data: ".getBytes("UTF-8")));
        END_DATA = Unpooled.unreleasableBuffer(Unpooled.wrappedBuffer("\n\n".getBytes("UTF-8")));
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, FullHttpRequest msg) throws Exception {
        if (!msg.getUri().startsWith(urlMapping)) {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
            return;
        }

        logger.debug("Handling Hystrix stream request...");
        final HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(CONTENT_TYPE, "text/event-stream;charset=UTF-8");
        response.headers().set(CACHE_CONTROL, "no-cache, no-store, max-age=0, must-revalidate");
        response.headers().add(PRAGMA, NO_CACHE);
        ctx.writeAndFlush(response);

        final Subject<Void, Void> subject = PublishSubject.create();
        final MultipleAssignmentSubscription subscription = new MultipleAssignmentSubscription();
        Subscription actionSubscription = Observable.timer(0, interval, TimeUnit.MILLISECONDS, Schedulers.computation())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long tick) {
                        if (!ctx.channel().isOpen()) {
                            subscription.unsubscribe();
                            logger.debug("Stopping Hystrix Turbine stream to connection");
                            return;
                        }
                        try {
                            Collection<HystrixCommandMetrics> hystrixCommandMetrics = HystrixCommandMetrics.getInstances();
                            Collection<HystrixThreadPoolMetrics> hystrixThreadPoolMetrics = HystrixThreadPoolMetrics.getInstances();
                            logger.debug("Found {} hystrix command metrics", hystrixCommandMetrics.size());
                            logger.debug("Found {} hystrix thread pool metrics", hystrixThreadPoolMetrics.size());
                            for (HystrixCommandMetrics commandMetrics : hystrixCommandMetrics) {
                                writeMetric(toJson(commandMetrics), ctx);
                            }
                            for (HystrixThreadPoolMetrics threadPoolMetrics : hystrixThreadPoolMetrics) {
                                writeMetric(toJson(threadPoolMetrics), ctx);
                            }
                            if (hystrixCommandMetrics.isEmpty() && hystrixThreadPoolMetrics.isEmpty()) {
                                ctx.writeAndFlush(PING.duplicate()).addListener(CLOSE_ON_FAILURE);
                            } else {
                                ctx.flush();
                            }
                        } catch (Exception e) {
                            logger.error("Unexpected error", e);
                            subject.onError(e);
                        }
                    }
                });
        subscription.set(actionSubscription);
    }

    private void writeMetric(String json, ChannelHandlerContext ctx) throws UnsupportedEncodingException {
        logger.debug("About to send Json data: \n{}", json);
        ctx.write(BEGIN_DATA.duplicate());
        ctx.write(ctx.alloc().buffer().writeBytes(json.getBytes("UTF-8"))).addListener(CLOSE_ON_FAILURE);
        ctx.write(END_DATA.duplicate());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
        logger.error("Unexpected Hystrix stream exception", cause);
    }
}
