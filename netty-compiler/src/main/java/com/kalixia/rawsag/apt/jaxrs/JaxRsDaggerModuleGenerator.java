package com.kalixia.rawsag.apt.jaxrs;

import com.squareup.java.JavaWriter;

import javax.annotation.Generated;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.squareup.java.JavaWriter.stringLiteral;
import static java.lang.reflect.Modifier.PUBLIC;

public class JaxRsDaggerModuleGenerator {
    private final Filer filer;
    private final Messager messager;
    private final boolean useDagger;
    private static final String MODULE_HANDLER = "GeneratedJaxRsDaggerModule";

    public JaxRsDaggerModuleGenerator(Filer filer, Messager messager, Map<String, String> options) {
        this.filer = filer;
        this.messager = messager;
        this.useDagger = options.containsKey("dagger") && "true".equals(options.get("dagger"));
    }

    public void generateDaggerModule(String destPackage, List<String> generatedHandlers) {
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
                            // add imports
                    .emitImports("com.kalixia.rawsag.codecs.jaxrs.JaxRsPipeline")
                    .emitImports("com.kalixia.rawsag.codecs.jaxrs.GeneratedJaxRsMethodHandler")
                    .emitImports("dagger.Module")
                    .emitImports("dagger.Provides")
                    .emitImports("com.fasterxml.jackson.databind.ObjectMapper")
                    .emitImports(Generated.class.getName())
                    .emitEmptyLine()
                            // begin class
                    .emitJavadoc("Dagger module for all generated classes.")
                    .emitAnnotation("Module(library = true)")
                    .emitAnnotation(Generated.class.getSimpleName(), stringLiteral(StaticAnalysisCompiler.GENERATOR_NAME))
                    .beginType(daggerModuleClassName, "class", PUBLIC)
            ;

//            generateProvideJaxRsPipelineMethod(writer, generatedHandlers);
            generateProvideObjectMapperMethod(writer);
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

    private JavaWriter generateProvideJaxRsPipelineMethod(JavaWriter writer, List<String> generatedHandlers)
            throws IOException {
        List<String> args = new ArrayList<>();
        args.add("ObjectMapper");
        args.add("objectMapper");
        for (int i = 0; i < generatedHandlers.size(); i++) {
            args.add("GeneratedJaxRsMethodHandler");
            args.add(String.format("handler%d", i + 1));
        }
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides")
                .beginMethod("JaxRsPipeline", "provideJaxRsPipeline", 0, args.toArray(new String[args.size()]))
                .emitStatement("return new %s(objectMapper)", JaxRsModuleGenerator.MODULE_HANDLER)
                .endMethod();
    }

    private JavaWriter generateProvideObjectMapperMethod(JavaWriter writer) throws IOException {
        return writer
                .emitEmptyLine()
                .emitAnnotation("Provides")
                .beginMethod("ObjectMapper", "provideObjectMapper", 0)
                .emitStatement("return new ObjectMapper()")
                .endMethod();
    }

}
