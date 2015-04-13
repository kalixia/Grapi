package com.kalixia.grapi.apt.jaxrs;

import com.codahale.metrics.MetricRegistry;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import dagger.Module;
import dagger.Provides;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Singleton;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.SortedSet;

import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.MANDATORY_WARNING;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class JaxRsDaggerModuleGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    private final boolean useMetrics;
    private static final String MODULE_HANDLER = "GeneratedJaxRsDaggerModule";

    public JaxRsDaggerModuleGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey(Options.DAGGER.getValue())
                && "true".equals(options.get(Options.DAGGER.getValue()));
        this.useMetrics = options.containsKey(Options.METRICS.getValue())
                && "true".equals(options.get(Options.METRICS.getValue()));
    }

    public void generateDaggerModule(String destPackage, SortedSet<String> generatedHandlers) {
        if (!useDagger) {
            return;
        }
        Writer writer = null;
        try {
            TypeSpec.Builder daggerModule = TypeSpec.classBuilder(MODULE_HANDLER)
                .addModifiers(Modifier.PUBLIC);

            daggerModule.addAnnotation(AnnotationSpec.builder(Module.class)
                    .addMember("library", "$L", "true")
                    .build());
            daggerModule.addAnnotation(AnnotationSpec.builder(Generated.class)
                    .addMember("value", "$S", StaticAnalysisCompiler.GENERATOR_NAME)
                    .build());

            daggerModule.addMethod(generateValidationFactoryMethod());
            daggerModule.addMethod(generateValidatorMethod());
            if (useMetrics) {
                daggerModule.addMethod(generateProvideMetricRegistryMethod());
            }

            JavaFile javaFile = JavaFile.builder(destPackage, daggerModule.build()).build();
            JavaFileObject sourceFile = filer.createSourceFile(destPackage + '.' + MODULE_HANDLER);
            writer = sourceFile.openWriter();
            javaFile.writeTo(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                    messager.printMessage(MANDATORY_WARNING, "Grapi: generated Dagger module for Netty handlers");
                } catch (IOException e) {
                    messager.printMessage(ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private MethodSpec generateValidationFactoryMethod() throws IOException {
        return MethodSpec.methodBuilder("provideValidationFactory")
                .addAnnotation(Provides.class)
                .addAnnotation(Singleton.class)
                .returns(ValidatorFactory.class)
                .addStatement("return $T.buildDefaultValidatorFactory()", Validation.class)
                .build();
    }

    private MethodSpec generateValidatorMethod() throws IOException {
        return MethodSpec.methodBuilder("provideValidator")
                .addAnnotation(Provides.class)
                .addAnnotation(Singleton.class)
                .addParameter(ValidatorFactory.class, "factory")
                .returns(Validator.class)
                .addStatement("return $L.getValidator()", "factory")
                .build();
    }

    private MethodSpec generateProvideMetricRegistryMethod() throws IOException {
        return MethodSpec.methodBuilder("provideMetricRegistry")
                .addAnnotation(Provides.class)
                .addAnnotation(Singleton.class)
                .returns(MetricRegistry.class)
                .addStatement("return new $T()", MetricRegistry.class)
                .build();
    }

}
