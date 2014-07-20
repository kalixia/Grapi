package com.kalixia.grapi.apt.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javawriter.JavaWriter;

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
import java.util.EnumSet;
import java.util.Map;
import java.util.SortedSet;

import static com.squareup.javawriter.JavaWriter.stringLiteral;
import static javax.lang.model.element.Modifier.PUBLIC;
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
        Writer handlerWriter = null;
        try {
            // TODO: only uppercase the first character
            String daggerModuleClassName = destPackage + '.' + MODULE_HANDLER;
            JavaFileObject handlerFile = filer.createSourceFile(daggerModuleClassName);
            handlerWriter = handlerFile.openWriter();
            JavaWriter writer = new JavaWriter(handlerWriter);
            writer
                    .emitPackage(destPackage)
                    .emitImports("dagger.Module")
                    .emitImports("dagger.Provides")
                    .emitImports(ObjectMapper.class)
                    .emitImports(Validator.class)
                    .emitImports(Validation.class)
                    .emitImports(ValidatorFactory.class);

            if (useMetrics) {
                writer.emitImports("com.codahale.metrics.MetricRegistry");
            }

            writer
                    .emitImports(Singleton.class)
                    .emitImports(Generated.class)
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc("Dagger module for all generated classes.")
                    .emitAnnotation("Module(library = true)")
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
                    .beginType(daggerModuleClassName, "class", EnumSet.of(PUBLIC));

            generateValidationFactoryMethod(writer);
            generateValidatorMethod(writer);
            if (useMetrics) {
                generateProvideMetricRegistryMethod(writer);
            }

            // end class
            writer.endType();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (handlerWriter != null) {
                try {
                    handlerWriter.close();
                    messager.printMessage(MANDATORY_WARNING, "Grapi: generated Dagger module for Netty handlers");
                } catch (IOException e) {
                    messager.printMessage(ERROR, "Can't close generated source file");
                }
            }
        }
    }

    private JavaWriter generateValidationFactoryMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("ValidatorFactory", "provideValidationFactory", EnumSet.noneOf(Modifier.class))
                .emitStatement("return Validation.buildDefaultValidatorFactory()")
                .endMethod();
    }

    private JavaWriter generateValidatorMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("Validator", "provideValidator", EnumSet.noneOf(Modifier.class), "ValidatorFactory", "factory")
                .emitStatement("return factory.getValidator()")
                .endMethod();
    }

    private JavaWriter generateProvideMetricRegistryMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides").emitAnnotation("Singleton")
                .beginMethod("MetricRegistry", "provideMetricRegistry", EnumSet.noneOf(Modifier.class))
                .emitStatement("return new MetricRegistry()")
                .endMethod();
    }

}
