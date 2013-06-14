package com.kalixia.grapi.apt.jaxrs;

import com.kalixia.grapi.apt.jaxrs.model.JaxRsMethodInfo;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsMethodInfoComparator;
import com.kalixia.grapi.apt.jaxrs.model.JaxRsParamInfo;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

@SupportedAnnotationTypes({ "javax.ws.rs.*" })
@SupportedSourceVersion(SourceVersion.RELEASE_7)
@SupportedOptions({ "dagger", "metrics" })
public class StaticAnalysisCompiler extends AbstractProcessor {
    private Elements elementUtils;
    private Messager messager;
    private final JaxRsAnalyzer analyzer = new JaxRsAnalyzer();
    private JaxRsMethodGenerator methodGenerator;
    private JaxRsModuleGenerator moduleGenerator;
    private JaxRsDaggerModuleGenerator daggerGenerator;
    private SortedMap<JaxRsMethodInfo,String> methodToHandlerName;
    private SortedSet<String> generatedHandlers;
    public static final String GENERATOR_NAME = "Grapi";

    @Override
    public void init(ProcessingEnvironment environment) {
        super.init(environment);
        Filer filer = environment.getFiler();
        this.messager = environment.getMessager();
        Map<String,String> options = environment.getOptions();
        this.elementUtils = environment.getElementUtils();
        methodGenerator = new JaxRsMethodGenerator(filer, messager, options);
        moduleGenerator = new JaxRsModuleGenerator(filer, messager, options);
        daggerGenerator = new JaxRsDaggerModuleGenerator(filer, messager, options);
        methodToHandlerName = new TreeMap<>(new JaxRsMethodInfoComparator());
        generatedHandlers = new TreeSet<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> resources = roundEnv.getElementsAnnotatedWith(Path.class);

        for (Element resource : resources) {
            // only keep classes
            if (!resource.getKind().isClass())
                continue;

            // extract class being analyzed
            PackageElement resourcePackage = elementUtils.getPackageOf(resource);
            String resourceClassName = resource.getSimpleName().toString();

            List<? extends Element> enclosedElements = resource.getEnclosedElements();
            for (Element elem : enclosedElements) {
                if (!ElementKind.METHOD.equals(elem.getKind()))
                    continue;
                ExecutableElement methodElement = (ExecutableElement) elem;

                // figure out if @GET, @POST, @DELETE, @PUT, etc are annotated on the method
                String verb = analyzer.extractVerb(elem);
                if (verb == null)
                    continue;
                String uriTemplate = analyzer.extractUriTemplate(resource, elem);
                String methodName = elem.getSimpleName().toString();
                String returnType = methodElement.getReturnType().toString();
                List<JaxRsParamInfo> parameters = analyzer.extractParameters(methodElement);
                // process @Produces annotations
                Produces producesAnnotation = resource.getAnnotation(Produces.class);
                if (producesAnnotation == null)
                    producesAnnotation = methodElement.getAnnotation(Produces.class);
                String[] produces;
                if (producesAnnotation != null)
                    produces = producesAnnotation.value();
                else
                    produces = new String[] { MediaType.TEXT_PLAIN };
                JaxRsMethodInfo methodInfo = new JaxRsMethodInfo(elem, verb, uriTemplate, methodName, returnType, parameters, produces);
                String generatedHandler = methodGenerator.generateHandlerClass(resourceClassName, resourcePackage, uriTemplate, methodInfo);
                methodToHandlerName.put(methodInfo, generatedHandler);
            }
        }

        generatedHandlers.addAll(methodToHandlerName.values());

        // TODO: use package from APT processor options
        if (roundEnv.processingOver() && generatedHandlers.size() > 0) {
            String firstHandlerName = generatedHandlers.first();
            String packageName = firstHandlerName.substring(0, firstHandlerName.lastIndexOf('.'));

            messager.printMessage(Diagnostic.Kind.MANDATORY_WARNING,
                    "Grapi: generated " + generatedHandlers.size() + " handlers");

            moduleGenerator.generateModuleClass(packageName, generatedHandlers);
            daggerGenerator.generateDaggerModule(packageName, generatedHandlers);
        }

        return false;
    }

}