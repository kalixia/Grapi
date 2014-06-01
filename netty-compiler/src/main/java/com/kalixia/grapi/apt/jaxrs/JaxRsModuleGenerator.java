package com.kalixia.grapi.apt.jaxrs;

import com.kalixia.grapi.ApiRequest;
import com.kalixia.grapi.ApiResponse;
import com.kalixia.grapi.MDCLogging;
import com.kalixia.grapi.codecs.jaxrs.GeneratedJaxRsMethodHandler;
import com.kalixia.grapi.codecs.jaxrs.JaxRsPipeline;
import com.squareup.javawriter.JavaWriter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.tools.JavaFileObject;
import javax.validation.Validator;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind;

public class JaxRsModuleGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    public static final String MODULE_HANDLER = "GeneratedJaxRsModuleHandler";

    public JaxRsModuleGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey("dagger") && "true".equals(options.get("dagger"));
    }

    public void generateModuleClass(String destPackage, SortedSet<String> generatedHandlers) {
        Writer handlerWriter = null;
        try {
            // TODO: only uppercase the first character
            String handlerClassName = destPackage + '.' + MODULE_HANDLER;
            JavaFileObject handlerFile = filer.createSourceFile(handlerClassName);
            handlerWriter = handlerFile.openWriter();
            JavaWriter writer = new JavaWriter(handlerWriter);
            writer
                    .emitPackage(destPackage)
                            // add imports
                    .emitImports(ApiRequest.class)
                    .emitImports(ApiResponse.class)
                    .emitImports(MDCLogging.class)
                    .emitImports(JaxRsPipeline.class)
                    .emitImports(GeneratedJaxRsMethodHandler.class)
                    .emitImports(ByteBuf.class)
                    .emitImports(Unpooled.class)
                    .emitImports(ChannelFuture.class)
                    .emitImports(ChannelFutureListener.class)
                    .emitImports(ChannelHandlerContext.class)
                    .emitImports("io.netty.channel.ChannelHandler.Sharable")
                    .emitImports(MessageToMessageDecoder.class)
                    .emitImports(HttpHeaders.class)
                    .emitImports(HttpResponseStatus.class)
                    .emitImports("com.fasterxml.jackson.databind.ObjectMapper")
                    .emitImports(Logger.class)
                    .emitImports(LoggerFactory.class)
                    .emitImports(MDC.class)
                    .emitImports(Charset.class)
                    .emitImports(List.class)
                    .emitImports(Arrays.class)
                    .emitImports(MediaType.class)
                    .emitImports(Validator.class)
                    .emitImports(Generated.class)
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc("Netty handler dispatching request to appropriate JAX-RS resource.")
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
                    .emitAnnotation("Sharable")
                    .beginType(handlerClassName, "class", EnumSet.of(PUBLIC, FINAL), "MessageToMessageDecoder<ApiRequest>", "JaxRsPipeline")
                    // add set of handlers
                    .emitField("List<? extends GeneratedJaxRsMethodHandler>", "handlers", EnumSet.of(PRIVATE, FINAL))
                    .emitField("ByteBuf", "ERROR_WRONG_URL", EnumSet.of(PRIVATE, STATIC, FINAL), "Unpooled.copiedBuffer(\"Wrong URL\", Charset.forName(\"UTF-8\"))")
                    .emitField("ByteBuf", "ERROR_INTERNAL_ERROR", EnumSet.of(PRIVATE, STATIC, FINAL), "Unpooled.copiedBuffer(\"Unexpected error\", Charset.forName(\"UTF-8\"))")
                    .emitField("Logger", "LOGGER", EnumSet.of(PRIVATE, STATIC, FINAL), "LoggerFactory.getLogger(" + handlerClassName + ".class)")
            ;
            generateConstructor(writer, handlerClassName, generatedHandlers);
            generateDecodeMethod(writer);
            generateIsKeepAliveMethod(writer);
            generateExceptionCaughtMethod(writer);
            // end class
            writer.endType();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (handlerWriter != null) {
                try {
                    handlerWriter.close();
                    messager.printMessage(Kind.MANDATORY_WARNING, "Grapi: generated Netty JAX-RS call dispatcher handler");
                } catch (IOException e) {
                    messager.printMessage(Kind.ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private JavaWriter generateConstructor(JavaWriter writer, String handlerClassName, SortedSet<String> generatedHandlers)
            throws IOException {
        writer.emitEmptyLine();

        if (!useDagger) {
            writer.beginMethod(null, handlerClassName, EnumSet.of(PUBLIC),
                    "ObjectMapper", "objectMapper", "Validator", "validator");
        } else {
            writer.emitAnnotation(Inject.class.getName());
            List<String> args = new ArrayList<>();
            args.addAll(Arrays.asList("ObjectMapper", "objectMapper"));
            args.addAll(Arrays.asList("Validator", "validator"));
            Iterator<String> iterator = generatedHandlers.iterator();
            for (int i = 1; i <= generatedHandlers.size(); i++) {
                args.add(iterator.next());
                args.add(String.format("handler%d", i));
            }
            writer.beginMethod(null, handlerClassName, EnumSet.of(PUBLIC), args.toArray(new String[args.size()]));
        }

        StringBuilder builder = new StringBuilder();
        Iterator<String> iterator = generatedHandlers.iterator();
        for (int i = 0; i < generatedHandlers.size(); i++) {
            if (useDagger)
                builder.append(String.format("handler%d", i + 1));
            else
                builder.append(String.format("new %s(objectMapper, validator)", iterator.next()));
            if (i + 1 < generatedHandlers.size())
                builder.append(",\n");
        }

        return writer.
                emitStatement("this.handlers = Arrays.asList(\n" + builder.toString() + "\n)")
                .endMethod();
    }

    private JavaWriter generateDecodeMethod(JavaWriter writer)
            throws IOException {
        writer
                .emitEmptyLine()
                .emitAnnotation(Override.class)
                .beginMethod("void", "decode", EnumSet.of(PROTECTED),
                        "ChannelHandlerContext", "ctx", "ApiRequest", "request", "List<Object>", "out")
                    .emitStatement("MDC.put(MDCLogging.MDC_REQUEST_ID, request.id().toString())")
                    .beginControlFlow("for (GeneratedJaxRsMethodHandler handler : handlers)")
                        .beginControlFlow("if (handler.matches(request))")
                            .beginControlFlow("try")
                                .emitStatement("ApiResponse response = handler.handle(request, ctx)");
                                writeToContextAndHandleKeepAlive(writer)
                                .emitStatement("return")
                            .nextControlFlow("catch (Exception e)")
                                .emitStatement("LOGGER.error(\"Can't invoke JAX-RS resource\", e)")
                                .emitStatement("ApiResponse response = new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, ERROR_INTERNAL_ERROR, MediaType.TEXT_PLAIN)");
        writeToContextAndHandleKeepAlive(writer)
                                .emitStatement("return")
                            .endControlFlow()
                        .endControlFlow()
                    .endControlFlow()
                    .emitSingleLineComment("no matching handler found -- issue a 404 error")
                    .emitStatement("LOGGER.info(\"Could not locate a JAX-RS resource for path '{}' and method {}\", " +
                            "request.uri(), request.method());")
                    .emitEmptyLine()
                    .emitStatement("ApiResponse response = new ApiResponse(request.id(), HttpResponseStatus.NOT_FOUND, ERROR_WRONG_URL, MediaType.TEXT_PLAIN)");
        return writeToContextAndHandleKeepAlive(writer)
                .endMethod();
    }

    private JavaWriter generateIsKeepAliveMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .beginMethod("boolean", "isKeepAlive", EnumSet.of(PRIVATE), "ApiRequest", "request")
                    .emitStatement("String connection = request.headers().getFirst(HttpHeaders.Names.CONNECTION.toString())")
                    .beginControlFlow("if (HttpHeaders.Values.CLOSE.toString().equalsIgnoreCase(connection))")
                        .emitStatement("return false")
                    .endControlFlow()
                    .emitStatement("return !HttpHeaders.Values.CLOSE.toString().equalsIgnoreCase(connection)")
                .endMethod();
    }

    private JavaWriter generateExceptionCaughtMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation(Override.class)
                .beginMethod("void", "exceptionCaught", EnumSet.of(PUBLIC),
                        Arrays.asList(
                                "ChannelHandlerContext", "ctx",
                                "Throwable", "cause"),
                        Arrays.asList("Exception"))
                .emitStatement("LOGGER.error(\"Unexpected decoder exception\", cause)")
                .emitStatement("super.exceptionCaught(ctx, cause)")
                .endMethod();
    }

    private JavaWriter writeToContextAndHandleKeepAlive(JavaWriter writer) throws IOException {
        return writer
                .emitStatement("ChannelFuture future = ctx.writeAndFlush(response)")
                .beginControlFlow("if (!isKeepAlive(request))")
                .emitStatement("future.addListener(ChannelFutureListener.CLOSE)")
                .endControlFlow();
    }

}
