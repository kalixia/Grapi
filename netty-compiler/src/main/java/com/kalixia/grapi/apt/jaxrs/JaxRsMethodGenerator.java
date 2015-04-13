package com.kalixia.grapi.apt.jaxrs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalixia.grapi.ApiRequest;
import com.kalixia.grapi.ApiResponse;
import com.kalixia.grapi.ObservableApiResponse;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsMethodInfo;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsParamInfo;
import com.kalixia.grapi.codecs.jaxrs.GeneratedJaxRsMethodHandler;
import com.kalixia.grapi.codecs.jaxrs.UriTemplateUtils;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.util.CharsetUtil;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Inject;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;
import static javax.tools.Diagnostic.Kind.ERROR;

@SuppressWarnings({"PMD.DataflowAnomalyAnalysis", "PMD.TooManyStaticImports"})
public class JaxRsMethodGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    private final boolean useMetrics;
    private final boolean useShiro;
    private final boolean useRxJava;
    private final JaxRsAnalyzer analyzer = new JaxRsAnalyzer();

    public JaxRsMethodGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey(Options.DAGGER.getValue())
                && "true".equals(options.get(Options.DAGGER.getValue()));
        this.useMetrics = options.containsKey(Options.METRICS.getValue())
                && "true".equals(options.get(Options.METRICS.getValue()));
        this.useShiro = options.containsKey(Options.SHIRO.getValue())
                        && "true".equals(options.get(Options.SHIRO.getValue()));
        this.useRxJava = options.containsKey(Options.RXJAVA.getValue())
                && "true".equals(options.get(Options.RXJAVA.getValue()));
    }

    public String generateHandlerClass(String resourceClassName, PackageElement resourcePackage,
                                        String uriTemplate, JaxRsMethodInfo method) {
        String packageName = resourcePackage.toString();
        String methodNameCamel = method.getMethodName()/*.toUpperCase()*/;
        String handlerClassName = format("%s_%s_Handler", resourceClassName, methodNameCamel);
        Writer writer = null;
        try {
            ClassName resourceClass = ClassName.get(packageName, resourceClassName);
            TypeSpec.Builder handler = TypeSpec.classBuilder(handlerClassName)
                    .addJavadoc("Netty handler for JAX-RS resource {@link $T#$L}.\n",
                            resourceClass, method.getMethodName())
                    .addAnnotation(AnnotationSpec.builder(Generated.class)
                            .addMember("value", "$S", StaticAnalysisCompiler.GENERATOR_NAME)
                            .build())
                    .addAnnotation(ChannelHandler.Sharable.class)
                    .addModifiers(PUBLIC, FINAL)
                    .addSuperinterface(GeneratedJaxRsMethodHandler.class);

            handler.addField(
                    FieldSpec.builder(resourceClass, "delegate")
                            .addJavadoc("Delegate for the JAX-RS resource\n")
                            .addModifiers(PRIVATE, FINAL)
                            .build())
                    .addField(FieldSpec.builder(ObjectMapper.class, "objectMapper")
                            .addModifiers(PRIVATE, FINAL)
                            .build())
                    .addField(FieldSpec.builder(Validator.class, "validator")
                            .addModifiers(PRIVATE, FINAL)
                            .build())
                    .addField(FieldSpec.builder(Method.class, "delegateMethod")
                            .addModifiers(PRIVATE, FINAL)
                            .build())
                    .addField(FieldSpec.builder(String.class, "URI_TEMPLATE")
                            .addModifiers(PRIVATE, STATIC, FINAL)
                            .initializer("$S", uriTemplate)
                            .build())
                    .addField(FieldSpec.builder(Logger.class, "LOGGER")
                            .addModifiers(PRIVATE, STATIC, FINAL)
                            .initializer("$T.getLogger($L.class)", LoggerFactory.class, handlerClassName)
                            .build());

            if (useMetrics) {
                handler.addField(FieldSpec.builder(Timer.class, "timer")
                        .addModifiers(PRIVATE, FINAL)
                        .build());
            }

            handler.addMethod(generateConstructor(resourceClass, method));
            handler.addMethod(generateMatchesMethod(method));
            handler.addMethod(generateHandleMethod(method));

            JavaFile javaFile = JavaFile.builder(packageName, handler.build()).build();
            JavaFileObject sourceFile = filer.createSourceFile(packageName + '.' + handlerClassName);
            writer = sourceFile.openWriter();
            javaFile.writeTo(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING, "Grapi: generated Netty JAX-RS call dispatcher handler");
                } catch (IOException e) {
                    messager.printMessage(ERROR, "Can't close generated source file");
                }
            }
        }
        return resourcePackage.toString() + '.' + handlerClassName;
    }

    private MethodSpec generateConstructor(ClassName resourceClass, JaxRsMethodInfo method) throws IOException {
        MethodSpec.Builder builder = MethodSpec.constructorBuilder()
                .addParameter(ObjectMapper.class, "objectMapper")
                .addParameter(Validator.class, "validator")
                .addStatement("this.objectMapper = objectMapper")
                .addStatement("this.validator = validator");
        if (useDagger) {
            builder.addAnnotation(Inject.class)
                    .addParameter(resourceClass, "delegate")
                    .addStatement("this.delegate = delegate");
        } else {
            builder.addStatement("this.delegate = new $T()", resourceClass);
        }
        if (useMetrics) {
            builder.addParameter(MetricRegistry.class, "registry");
        }

        // create reflection method used by the validation API
        builder.beginControlFlow("try");
        if (method.hasParameters()) {
            StringBuilder paramsBuilder = new StringBuilder();
            Iterator<JaxRsParamInfo> paramIterator = method.getParameters().iterator();
            while (paramIterator.hasNext()) {
                JaxRsParamInfo param = paramIterator.next();
                paramsBuilder.append(param.getType().toString()).append(".class");
                if (paramIterator.hasNext()) {
                    paramsBuilder.append(", ");
                }
            }
            builder.addStatement("this.delegateMethod = delegate.getClass().getMethod($S, $L)",
                    method.getMethodName(), paramsBuilder.toString());
        } else {
            builder.addStatement("this.delegateMethod = delegate.getClass().getMethod($S)", method.getMethodName());
        }
        builder.nextControlFlow("catch (NoSuchMethodException e) ")
                .addCode("// should not happen as Grapi scanned the source code!\n")
                .addStatement("throw new $T(\"Can't find method '$L.$L' through reflection\")",
                        RuntimeException.class, resourceClass, method.getMethodName())
                .endControlFlow();

        if (useMetrics) {
            builder.addStatement("this.timer = registry.timer(MetricRegistry.name($S, $S))",
                    resourceClass.simpleName(), method.getMethodName());
        }

        return builder.build();
    }

    private MethodSpec generateMatchesMethod(JaxRsMethodInfo method)
            throws IOException {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("matches")
                        .addAnnotation(Override.class)
                        .addModifiers(PUBLIC)
                        .addParameter(ApiRequest.class, "request")
                        .returns(boolean.class)
                // check against HTTP method
                .addStatement("boolean verbMatches = $T.$L.equals(request.method())",
                        HttpMethod.class, method.getVerb());

        // check against URI template
        if (UriTemplateUtils.hasParameters(method.getUriTemplate())) {
            builder.addStatement("boolean uriMatches = $T.extractParameters(URI_TEMPLATE, request.uri()).size() > 0",
                    UriTemplateUtils.class);
        } else if (method.hasQueryParameters()) {
            builder
                    .addStatement("$T dec = new $T(request.uri())", QueryStringDecoder.class, QueryStringDecoder.class)
                    .addStatement("boolean uriMatches = $S.equals(dec.path()) || $S.equals(dec.path())",
                            method.getUriTemplate(), method.getUriTemplate() + "/");
        } else {
            builder.addStatement("boolean uriMatches = $S.equals(request.uri()) || $S.equals(request.uri())",
                    method.getUriTemplate(), method.getUriTemplate() + "/");
        }

        return builder
                .addStatement("return verbMatches && uriMatches")
                .build();
    }

    @SuppressWarnings({"PMD.EmptyCatchBlock", "PMD.OnlyOneReturn"})
    private MethodSpec generateHandleMethod(JaxRsMethodInfo method) throws IOException {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("handle")
                .addAnnotation(Override.class)
                .addModifiers(PUBLIC)
                .addParameter(ApiRequest.class, "request")
                .addParameter(ChannelHandlerContext.class, "ctx")
                .returns(ApiResponse.class);

        if (useMetrics) {
            builder.addAnnotation(Timed.class);
            builder.addStatement("final $T context = timer.time()", Timer.Context.class).beginControlFlow("try");
        }

        builder.addCode("// TODO: extract expected charset from the API request instead of using the default charset\n")
                .addStatement("$T charset = $T.$L", Charset.class, CharsetUtil.class, "UTF_8");
        if (useShiro) {
            List<Annotation> shiroAnnotations = method.getShiroAnnotations();
            if (shiroAnnotations != null && shiroAnnotations.size() > 0) {
                ShiroGenerator.beginSubject(builder);
                for (Annotation shiroAnnotation : shiroAnnotations) {
                    Class<? extends Annotation> annotationType = shiroAnnotation.annotationType();
                    if (annotationType.isAssignableFrom(RequiresPermissions.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresPermissionsCheck(builder, (RequiresPermissions) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresRoles.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresRolesCheck(builder, (RequiresRoles) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresGuest.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresGuestCheck(builder, (RequiresGuest) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresUser.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresUserCheck(builder, (RequiresUser) shiroAnnotation);
                    } else if (annotationType.isAssignableFrom(RequiresAuthentication.class)) {
                        ShiroGenerator.generateShiroCodeForRequiresAuthenticationCheck(builder, (RequiresAuthentication) shiroAnnotation);
                    } else {
                        messager.printMessage(ERROR, "Can't process annotation of type " + annotationType.getSimpleName());
                    }
                }
                ShiroGenerator.endSubject(builder);
            }
        }

        // analyze @PathParam annotations
        Map<String, String> parametersMap = analyzer.analyzePathParamAnnotations(method);

        builder.beginControlFlow("try");

        boolean conversionNeeded = false;

        // check if JAX-RS resource method has parameters; if so extract them from URI
        if (method.hasParameters()) {
            builder.addCode("// Extract parameters from request\n")
                    .addStatement("$T parameters = $T.extractParameters(URI_TEMPLATE, request.uri())",
                            ParameterizedTypeName.get(Map.class, String.class, String.class), UriTemplateUtils.class);
            // extract each parameter
            for (JaxRsParamInfo parameter : method.getParameters()) {
                String parameterValueSource;
                if (parameter.getElement().getAnnotation(FormParam.class) != null) {
                    FormParam formParam = parameter.getElement().getAnnotation(FormParam.class);
                    builder.addCode("// Extract form param '$L'\n", formParam.value());
                    parameterValueSource = format("request.formParameter(\"%s\")", formParam.value());
                } else if (parameter.getElement().getAnnotation(QueryParam.class) != null) {
                    QueryParam queryParam = parameter.getElement().getAnnotation(QueryParam.class);
                    builder.addCode("// Extract query param '$L'\n", queryParam.value());
                    parameterValueSource = format("request.queryParameter(\"%s\")", queryParam.value());
                } else if (parameter.getElement().getAnnotation(HeaderParam.class) != null) {
                    HeaderParam headerParam = parameter.getElement().getAnnotation(HeaderParam.class);
                    builder.addCode("// Extract header param '$L'\n", headerParam.value());
                    parameterValueSource = format("request.headerParameter(\"%s\")", headerParam.value());
                } else if (parameter.getElement().getAnnotation(CookieParam.class) != null) {
                    CookieParam cookieParam = parameter.getElement().getAnnotation(CookieParam.class);
                    builder.addCode("// Extract cookie param '$L'\n", cookieParam.value());
                    parameterValueSource = format("request.cookieParameter(\"%s\")", cookieParam.value());
                } else {
                    String uriTemplateParameter = parametersMap.get(parameter.getName());
                    if (uriTemplateParameter == null) {
                        // consider this is actually content to be converted to an object
                        parameterValueSource = "request.content().toString(CharsetUtil.UTF_8)";
                    } else {
                        // otherwise this is extracted parameterValueSource URI
                        parameterValueSource = format("parameters.get(\"%s\")", uriTemplateParameter);
                    }
                }
                String typeClassName = parameter.getElement().asType().toString();
                Method typeValueOfMethod = null;
                Constructor<?> typeConstructorFromString = null;
                try {
                    Class<?> typeClass = Class.forName(typeClassName);
                    typeValueOfMethod = typeClass.getMethod("valueOf", String.class);
                    typeConstructorFromString = typeClass.getConstructor(String.class);
                } catch (Exception e) {
                    // ignore
                }
                if (typeClassName.startsWith("java.lang.")) {
                    typeClassName = typeClassName.substring("java.lang.".length());
                }
                TypeMirror type = parameter.getType();
                if (String.class.getSimpleName().equals(typeClassName)) {
                    builder.addStatement("String $L = $L", parameter.getName(), parameterValueSource);
                } else if (type.getKind().isPrimitive()) {
                    char firstChar = type.toString().charAt(0);
                    String shortName = Character.toUpperCase(firstChar) + type.toString().substring(1);
                    switch (type.getKind()) {
                        case INT:
                            builder.addStatement("$L $L = $L.parse$L($L)", type, parameter.getName(),
                                    Integer.class.getSimpleName(), shortName,
                                    parameterValueSource);
                            break;
                        default:
                            builder.addStatement("$L $L = $L.parse$L($L)", type, parameter.getName(),
                                    shortName, shortName, parameterValueSource);
                    }
                } else if (typeValueOfMethod != null) {
                    builder.addStatement("$L $L = $L.valueOf($L)",
                            typeClassName, parameter.getName(), typeClassName, parameterValueSource);
                } else if (typeConstructorFromString != null) {
                    builder.addStatement("$L $L = new $L($L)",
                            typeClassName, parameter.getName(), typeClassName, parameterValueSource);
                } else {
                    conversionNeeded = true;
                    builder.addStatement("$L $L = objectMapper.readValue($L, $L.class)",
                            type, parameter.getName(), parameterValueSource, type);
                }
            }
        }

        TypeName constraintViolationType = ParameterizedTypeName.get(
                ClassName.get(ConstraintViolation.class),
                TypeName.get(method.getResourceElement().asType()));

        // validate parameters
        if (method.hasParametersToValidate()) {
            StringBuilder paramsBuilder = new StringBuilder();
            Iterator<JaxRsParamInfo> iterator = method.getParameters().iterator();
            while (iterator.hasNext()) {
                JaxRsParamInfo param = iterator.next();
                paramsBuilder.append(param.getName());
                if (iterator.hasNext()) {
                    paramsBuilder.append(", ");
                }
            }
            builder.addCode("// Validate parameters\n")
                    .addStatement("$T violations = validator.forExecutables().validateParameters(delegate, delegateMethod, new Object[] { $L })",
                            ParameterizedTypeName.get(ClassName.get(Set.class), constraintViolationType), paramsBuilder.toString());
            builder
                    .beginControlFlow("if (!violations.isEmpty())")
                    .addStatement("$T iterator = violations.iterator()",
                            ParameterizedTypeName.get(ClassName.get(Iterator.class), constraintViolationType))
                    .addStatement("$T builder = new $T()", StringBuilder.class, StringBuilder.class)
                    .beginControlFlow("while (iterator.hasNext())")
                        .addStatement("$T violation = iterator.next()", constraintViolationType)
                        .addStatement("builder.append(\"Parameter \").append(violation.getMessage()).append('\\n')")
                    .endControlFlow()
                    .addStatement("return new $T(request.id(), $T.BAD_REQUEST, $T.copiedBuffer(builder.toString(), charset), $T.TEXT_PLAIN)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                    .endControlFlow();
        }

        // call JAX-RS resource method
        builder.addCode("// Call JAX-RS resource\n");
        if (method.hasParameters()) {
            StringBuilder paramsBuilder = new StringBuilder();
            for (int i = 0; i < method.getParameters().size(); i++) {
                JaxRsParamInfo paramInfo = method.getParameters().get(i);
                paramsBuilder.append(paramInfo.getName());
                if (i + 1 < method.getParameters().size()) {
                    paramsBuilder.append(", ");
                }
            }
            if (method.hasReturnType()) {
                builder.addStatement("$T result = delegate.$L($L)",
                        method.getReturnType(), method.getMethodName(), paramsBuilder.toString());
            } else {
                builder.addStatement("delegate.$L($L)", method.getMethodName(), paramsBuilder.toString());
            }
        } else if (method.hasReturnType()) {
            builder.addStatement("$T result = delegate.$L()", method.getReturnType(), method.getMethodName());
        } else {
            builder.addStatement("delegate.$L()", method.getMethodName());
        }

        // validate result
        if (method.hasReturnType()) {
            builder.addCode("// Validate result returned\n")
                    .addStatement("$T resultViolations = validator.forExecutables().validateReturnValue(delegate, delegateMethod, result)",
                            ParameterizedTypeName.get(ClassName.get(Set.class), constraintViolationType))
                    .beginControlFlow("if (!resultViolations.isEmpty())")
                        .addStatement("$T iterator = resultViolations.iterator()",
                                ParameterizedTypeName.get(ClassName.get(Iterator.class), constraintViolationType))
                        .addStatement("$T builder = new $T()", StringBuilder.class, StringBuilder.class)
                        .beginControlFlow("while (iterator.hasNext())")
                            .addStatement("$T violation = iterator.next()", constraintViolationType)
                            .addStatement("builder.append(\"Result \").append(violation.getMessage()).append('\\n')")
                        .endControlFlow()
                        .addStatement("return new $T(request.id(), $T.BAD_REQUEST, $T.copiedBuffer(builder.toString(), charset), $T.TEXT_PLAIN)",
                                ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                    .endControlFlow();
        }

        builder.addCode("// Build API response object\n");
        String produces = method.getProduces()[0];
        if (useRxJava && method.hasReturnType() && method.getReturnType().toString().startsWith("rx.Observable")) {
            builder.addStatement("return new $T(request.id(), $T.OK, result, $S)",
                    ObservableApiResponse.class, HttpResponseStatus.class, produces);
        } else if (method.hasReturnType() && Response.class.getName().equals(method.getReturnType().toString())) {
            builder.beginControlFlow("if (result.hasEntity())")
                    .addStatement("Object entity = result.getEntity()")
                    .addStatement("byte[] content")
                    .beginControlFlow("if (entity instanceof String)")
                        .addStatement("content = ((String) entity).getBytes($S)", "UTF-8")
                    .nextControlFlow("else")
                        .addStatement("content = objectMapper.writeValueAsBytes(entity)")
                    .endControlFlow()
                    .addStatement("return new $T(request.id(), " +
                            "$T.valueOf(result.getStatus()), $T.copiedBuffer(content), $S, result.getStringHeaders())",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, produces)
                    .nextControlFlow("else")
                    .addStatement("return new $T(request.id(), " +
                                    "$T.valueOf(result.getStatus()), $T.EMPTY_BUFFER, $S, result.getStringHeaders())",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, produces)
                    .endControlFlow();
        } else if (String.class.getName().equals(method.getReturnType().toString())) {
            builder
                    .addStatement("byte[] content = result == null ? new byte[] {} : result.getBytes($S)", "UTF-8")
                    .addStatement("return new $T(request.id(), $T.OK, $T.wrappedBuffer(content), $S)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, produces);
        } else if (method.hasReturnType()) {            // convert result only if there is one
            conversionNeeded = true;
            builder.addStatement("byte[] content = objectMapper.writeValueAsBytes(result)")
                    .addStatement("return new $T(request.id(), $T.OK, $T.wrappedBuffer(content), $S)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, produces);
        } else {
            builder.addStatement("return new $T(request.id(), $T.NO_CONTENT, $T.EMPTY_BUFFER, $S)",
                    ApiResponse.class, HttpResponseStatus.class, Unpooled.class, produces);
        }

        // conversion is only needed if returnType is not string and if at least one parameter need Json conversion
        if (conversionNeeded) {
            builder.nextControlFlow("catch (IllegalArgumentException|com.fasterxml.jackson.databind.JsonMappingException e) ");
        } else {
            builder.nextControlFlow("catch (IllegalArgumentException e) "/*, ClassName.get("java.lang", "IllegalArgumentException")*/);
        }
        builder
                    .addStatement("LOGGER.error(\"Bad request\", e)")
                    .addStatement("return new $T(request.id(), $T.BAD_REQUEST, $T.copiedBuffer(e.getMessage(), charset), $T.TEXT_PLAIN)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                .nextControlFlow("catch (javax.ws.rs.WebApplicationException e) ")
                    .addStatement("$T response = e.getResponse()", Response.class)
                    .addStatement("return new $T(request.id(), $T.valueOf(response.getStatus()), $T.copiedBuffer(e.getMessage(), charset), $T.TEXT_PLAIN)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                .nextControlFlow("catch (Exception e) ")
                    .addStatement("e.printStackTrace()")
                    .beginControlFlow("if (e.getMessage() != null)")
                        .addStatement("return new $T(request.id(), $T.INTERNAL_SERVER_ERROR, $T.copiedBuffer(e.getMessage(), charset), $T.TEXT_PLAIN)",
                                ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                    .nextControlFlow("else")
                        .addStatement("return new $T(request.id(), $T.INTERNAL_SERVER_ERROR, $T.copiedBuffer(e.toString(), charset), $T.TEXT_PLAIN)",
                                ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                    .endControlFlow()
                .endControlFlow();

        if (useMetrics) {
            builder.nextControlFlow("finally ").addStatement("context.stop()").endControlFlow();
        }

        return builder.build();
    }

}
