package com.kalixia.rawsag.apt.jaxrs;

import com.squareup.java.JavaWriter;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.inject.Singleton;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
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
                    .emitImports("com.fasterxml.jackson.databind.ObjectMapper");

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
                .emitStatement("return new ObjectMapper()")
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
