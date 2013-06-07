package com.kalixia.rawsag.apt.jaxrs;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.java.JavaWriter;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Singleton;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.SortedSet;

import static com.squareup.java.JavaWriter.stringLiteral;
import static java.lang.reflect.Modifier.PUBLIC;

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
        if (!useDagger)
            return;
        Writer handlerWriter = null;
        try {
            // TODO: only uppercase the first character
            String daggerModuleClassName = destPackage + '.' + MODULE_HANDLER;
            JavaFileObject handlerFile = filer.createSourceFile(daggerModuleClassName);
            handlerWriter = handlerFile.openWriter();
            JavaWriter writer = new JavaWriter(handlerWriter);
            writer
                    .emitPackage(destPackage.toString())
                    .emitImports("dagger.Module")
                    .emitImports("dagger.Provides")
                    .emitImports(ObjectMapper.class.getName())
                    .emitImports(JsonParser.class.getName())
                    .emitImports(Validator.class.getName())
                    .emitImports(Validation.class.getName())
                    .emitImports(ValidatorFactory.class.getName());

            if (useMetrics) {
                writer.emitImports("com.codahale.metrics.MetricRegistry");
            }

            writer
                    .emitImports(Singleton.class.getName())
                    .emitImports(Generated.class.getName())
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc("Dagger module for all generated classes.")
                    .emitAnnotation("Module(library = true)")
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
                    .beginType(daggerModuleClassName, "class", PUBLIC);

            generateProvideObjectMapperMethod(writer);
            generateValidationFactoryMethod(writer);
            generateValidatorMethod(writer);
            if (useMetrics)
                generateProvideMetricRegistryMethod(writer);

            // end class
            writer.endType();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (handlerWriter != null) {
                try {
                    handlerWriter.close();
                } catch (IOException e) {
                    messager.printMessage(Diagnostic.Kind.ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private JavaWriter generateProvideObjectMapperMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("ObjectMapper", "provideObjectMapper", 0)
                .emitStatement("ObjectMapper objectMapper = new ObjectMapper()")
                .emitStatement("objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)")
                .emitStatement("objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)")
                .emitStatement("return objectMapper")
                .endMethod();
    }

    private JavaWriter generateValidationFactoryMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("ValidatorFactory", "provideValidationFactory", 0)
                .emitStatement("return Validation.buildDefaultValidatorFactory()")
                .endMethod();
    }

    private JavaWriter generateValidatorMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("Validator", "provideValidator", 0, "ValidatorFactory", "factory")
                .emitStatement("return factory.getValidator()")
                .endMethod();
    }

    private JavaWriter generateProvideMetricRegistryMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("MetricRegistry", "provideMetricRegistry", 0)
                .emitStatement("return new MetricRegistry()")
                .endMethod();
    }

}
