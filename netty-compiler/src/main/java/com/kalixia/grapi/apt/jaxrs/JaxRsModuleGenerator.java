package com.kalixia.grapi.apt.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.grapi.ApiRequest;
import com.kalixia.grapi.ApiResponse;
import com.kalixia.grapi.MDCLogging;
import com.kalixia.grapi.codecs.jaxrs.GeneratedJaxRsMethodHandler;
import com.kalixia.grapi.codecs.jaxrs.JaxRsPipeline;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.tools.Diagnostic.Kind;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyStaticImports"})
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
        Writer writer = null;
        try {
            TypeSpec.Builder moduleHandler = TypeSpec.classBuilder(MODULE_HANDLER)
                    .addJavadoc("Netty handler dispatching request to appropriate JAX-RS resource.\n")
                    .addAnnotation(AnnotationSpec.builder(Generated.class)
                            .addMember("value", "$S", StaticAnalysisCompiler.GENERATOR_NAME)
                            .build())
                    .addAnnotation(ChannelHandler.Sharable.class)
                    .addModifiers(PUBLIC, FINAL)
                    .superclass(ParameterizedTypeName.get(MessageToMessageDecoder.class, ApiRequest.class))
                    .addSuperinterface(JaxRsPipeline.class);

            // add fields

            // private final List<? extends GeneratedJaxRsMethodHandler> handlers;
            moduleHandler.addField(FieldSpec.builder(
                    ParameterizedTypeName.get(List.class, GeneratedJaxRsMethodHandler.class),
                    "handlers", PRIVATE, FINAL).build());
            // private static final ByteBuf ERROR_WRONG_URL = Unpooled.copiedBuffer("Wrong URL", CharsetUtil.UTF_8)
            moduleHandler.addField(FieldSpec.builder(ByteBuf.class, "ERROR_WRONG_URL", PRIVATE, FINAL)
                    .initializer("$T.copiedBuffer($S, $T.$L)", Unpooled.class, "Wrong URL", CharsetUtil.class, "UTF_8")
                    .build());
            // private static final ByteBuf ERROR_INTERNAL_ERROR = Unpooled.copiedBuffer("Unexpected error", CharsetUtil.UTF_8)
            moduleHandler.addField(FieldSpec.builder(ByteBuf.class, "ERROR_INTERNAL_ERROR", PRIVATE, FINAL)
                    .initializer("$T.copiedBuffer($S, $T.$L)", Unpooled.class, "Unexpected error", CharsetUtil.class, "UTF_8")
                    .build());
            // private static final Logger LOGGER = LoggerFactory.getLogger(com.kalixia.grapi.GeneratedJaxRsModuleHandler.class)
            moduleHandler.addField(FieldSpec.builder(Logger.class, "LOGGER", PRIVATE, FINAL)
                    .initializer("$T.getLogger($T.class)", LoggerFactory.class, ClassName.get(destPackage, "GeneratedJaxRsModuleHandler"))
                    .build());

            MethodSpec isKeepAliveMethod = generateIsKeepAliveMethod();

            moduleHandler.addMethod(generateConstructor(generatedHandlers));
            moduleHandler.addMethod(generateDecodeMethod(isKeepAliveMethod));
            moduleHandler.addMethod(isKeepAliveMethod);
            moduleHandler.addMethod(generateExceptionCaughtMethod());

            JavaFile javaFile = JavaFile.builder(destPackage, moduleHandler.build()).build();
            JavaFileObject sourceFile = filer.createSourceFile(destPackage + '.' + MODULE_HANDLER);
            writer = sourceFile.openWriter();
            javaFile.writeTo(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    messager.printMessage(Kind.MANDATORY_WARNING, "Grapi: generated Netty JAX-RS call dispatcher handler");
                } catch (IOException e) {
                    messager.printMessage(Kind.ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private MethodSpec generateConstructor(SortedSet<String> generatedHandlers) throws IOException {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);

        builder
            .addParameter(ObjectMapper.class, "objectMapper")
            .addParameter(Validator.class, "validator");

        if (useDagger) {
            builder.addAnnotation(Inject.class);
        }

        Iterator<String> iterator = generatedHandlers.iterator();
        StringBuilder paramsBuilder = new StringBuilder();
        for (int i = 1; i <= generatedHandlers.size(); i++) {
            String generatedHandler = iterator.next();
            String handlerParamName = format("handler%d", i);
            builder.addParameter(ClassName.get("", generatedHandler), handlerParamName);
            if (useDagger) {
                paramsBuilder.append(handlerParamName);
            } else {
                paramsBuilder.append(format("new %s(objectMapper, validator)", iterator.next()));
            }
            if (i < generatedHandlers.size()) {
                paramsBuilder.append(",\n");
            }
        }
        builder.addStatement("this.$L = $T.asList($L)", "handlers", Arrays.class, paramsBuilder.toString());

        return builder.build();
    }

    private MethodSpec generateDecodeMethod(MethodSpec isKeepAliveMethod) throws IOException {
        return MethodSpec.methodBuilder("decode")
                .addModifiers(PROTECTED)
                .addAnnotation(Override.class)
                .addParameter(ChannelHandlerContext.class, "ctx")
                .addParameter(ApiRequest.class, "request")
                .addParameter(ParameterizedTypeName.get(List.class, Object.class), "out")
                .addStatement("$T.put($T.MDC_REQUEST_ID, $L.id().toString())", MDC.class, MDCLogging.class, "request")
                .beginControlFlow("for ($T $L : handlers)", GeneratedJaxRsMethodHandler.class, "handler")
                    .beginControlFlow("if ($L.matches(request))", "handler")
                        .beginControlFlow("try")
                            .addStatement("$T response = $L.handle(request, ctx)", ApiResponse.class, "handler")
                            .addStatement("$T future = ctx.writeAndFlush(response)", ChannelFuture.class)
                            .beginControlFlow("if (!$N(request))", isKeepAliveMethod)
                                .addStatement("future.addListener($T.CLOSE)", ChannelFutureListener.class)
                            .endControlFlow()
                            .addStatement("return")
                        .nextControlFlow("catch (Exception e) ")
                            .addStatement("LOGGER.error($S, e)", "Can't invoke JAX-RS resource")
                            .addStatement("$T response = new $T(request.id(), $T.INTERNAL_SERVER_ERROR, ERROR_INTERNAL_ERROR, $T.TEXT_PLAIN)",
                                    ApiResponse.class, ApiResponse.class, HttpResponseStatus.class, MediaType.class)
                            .addStatement("$T future = ctx.writeAndFlush(response)", ChannelFuture.class)
                            .beginControlFlow("if (!$N(request))", isKeepAliveMethod)
                                .addStatement("future.addListener($T.CLOSE)", ChannelFutureListener.class)
                            .endControlFlow()
                            .addStatement("return")
                        .endControlFlow()
                    .endControlFlow()
                .endControlFlow()
//                .addJavadoc("no matching handler found -- issue a 404 error")
                .addStatement("LOGGER.info($S, request.uri(), request.method())", "Could not locate a JAX-RS resource for path '{}' and method {}")
                .addStatement("$T response = new $T(request.id(), $T.NOT_FOUND, ERROR_WRONG_URL, $T.TEXT_PLAIN)",
                        ApiResponse.class, ApiResponse.class, HttpResponseStatus.class, MediaType.class)
                .addStatement("$T future = ctx.writeAndFlush(response)", ChannelFuture.class)
                .beginControlFlow("if (!$N(request))", isKeepAliveMethod)
                    .addStatement("future.addListener($T.CLOSE)", ChannelFutureListener.class)
                .endControlFlow()
                .build();

    }

    private MethodSpec generateIsKeepAliveMethod() throws IOException {
        return MethodSpec.methodBuilder("isKeepAlive")
                .addModifiers(PRIVATE)
                .addParameter(ApiRequest.class, "request")
                .returns(boolean.class)
                .addStatement("$T connection = $L.headerParameter($T.CONNECTION.toString())",
                        String.class, "request", HttpHeaderNames.class)
                .beginControlFlow("if ($T.CLOSE.toString().equalsIgnoreCase(connection))", HttpHeaderValues.class)
                .addStatement("return false")
                .endControlFlow()
                .addStatement("return !$T.CLOSE.toString().equalsIgnoreCase(connection)", HttpHeaderValues.class)
                .build();
    }

    private MethodSpec generateExceptionCaughtMethod() throws IOException {
        return MethodSpec.methodBuilder("exceptionCaught")
                .addModifiers(PUBLIC)
                .addAnnotation(Override.class)
                .addParameter(ChannelHandlerContext.class, "ctx")
                .addParameter(Throwable.class, "cause")
                .addException(Exception.class)
                .addStatement("LOGGER.error($S, cause)", "Unexpected decoder exception")
                .addStatement("super.exceptionCaught($L, cause)", "ctx")
                .build();
    }

}
