package com.kalixia.grapi.codecs.shiro;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;

import static io.netty.handler.codec.http.HttpHeaderNames.AUTHORIZATION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

/**
 * <a href="http://shiro.apache.org">Shiro</a> handler exposing the {@link Subject}
 * to following handlers in the pipeline.
 *
 * To use it, add it in your pipeline after the {@link io.netty.handler.codec.http.HttpRequestDecoder}.
 */
@ChannelHandler.Sharable
@SuppressWarnings({"PMD.AvoidPrefixingMethodParameters", "PMD.DataflowAnomalyAnalysis"})
public class ShiroHandler extends ChannelDuplexHandler {
    private final SecurityManager securityManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(ShiroHandler.class);
    public static final String BEARER = "Bearer ";
    public static final AttributeKey<Subject> ATTR_SUBJECT = AttributeKey.valueOf("SHIRO_SUBJECT");

    public ShiroHandler(SecurityManager securityManager) {
        super();
        if (securityManager == null) {
            throw new NullPointerException("Shiro security manager can't be null");
        }
        this.securityManager = securityManager;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest httpMessage = (HttpRequest) msg;
            LOGGER.debug("Intercepting {} request on '{}'", httpMessage.method(), httpMessage.uri());
            SocketAddress socketAddress = ctx.channel().remoteAddress();
            String remoteHost;
            if (socketAddress instanceof InetSocketAddress) {
                remoteHost = ((InetSocketAddress) socketAddress).getHostString();
            } else {
                remoteHost = "";

            }
            if (httpMessage.headers().contains(AUTHORIZATION)) {
                Subject subject = new Subject.Builder(securityManager)
                        .host(remoteHost)
                        .buildSubject();
                String authorization = httpMessage.headers().get(AUTHORIZATION);
                if (authorization.startsWith(BEARER)) {
                    String bearer = authorization.substring(BEARER.length());
                    OAuth2Token token = new OAuth2Token(bearer);
                    try {
                        subject.login(token);
                        ctx.channel().attr(ATTR_SUBJECT).set(subject);
                    } catch (AuthenticationException e) {
                        LOGGER.error("Can't authenticate user with OAuth2 token '{}'", bearer);
                        String errorMessage = String.format("OAuth2 token '%s' is not valid.", bearer);
                        HttpResponse httpResponse = new DefaultFullHttpResponse(
                                HttpVersion.HTTP_1_1,
                                HttpResponseStatus.UNAUTHORIZED,
                                Unpooled.copiedBuffer(errorMessage,
                                        Charset.defaultCharset()));
                        httpResponse.headers().set(CONTENT_LENGTH, errorMessage.length());
                        httpResponse.headers().set(CONTENT_TYPE, MediaType.TEXT_PLAIN);
                        ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
                        return;
                    }
                }
            }
        } else {
            LOGGER.debug("Ignoring unsupported message {}", msg);
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (msg instanceof HttpMessage) {
            Subject subject = ctx.channel().attr(ATTR_SUBJECT).getAndRemove();
            if (subject != null) {
                subject.logout();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error("Unexpected error", cause);
        cause.printStackTrace();
        ctx.close();
    }
}
