package com.kalixia.rawsag.codecs.jaxrs;

import org.glassfish.jersey.uri.UriTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class UriTemplateUtils {
    @Deprecated
    private static final Pattern uriTemplatePattern = Pattern.compile("\\{(.*)\\}");
    private static final Map<String, UriTemplate> uriTemplatesCache = new ConcurrentHashMap<>();

    public static void prepareUriTemplate(String uriTemplate) {
        UriTemplate template = new UriTemplate(uriTemplate);
        uriTemplatesCache.put(uriTemplate, template);
    }

    /**
     * Extract parameters from URI, given a URI template.
     * @param uriTemplate
     * @return
     * @deprecated
     */
    public static Pattern extractRegexPattern(String uriTemplate) {
        // strip last '/' if present
        if (uriTemplate.endsWith("/"))
            uriTemplate = uriTemplate.substring(0, uriTemplate.length() - 1);

        return Pattern.compile('^' + uriTemplatePattern.matcher(uriTemplate).replaceAll("(.*)") + "/?$");
    }

    public static boolean hasParameters(String uriTemplate) {
        UriTemplate template = createUriTemplateOrGetFromCache(uriTemplate);
        return template.getNumberOfTemplateVariables() > 0;
    }

    public static Map<String, String> extractParameters(String uriTemplate, String uri) {
        UriTemplate template = createUriTemplateOrGetFromCache(uriTemplate);
        Map<String, String> parametersMap = new HashMap<>();
        boolean match = template.match(uri, parametersMap);
        if (!match) {
            return Collections.emptyMap();
        } else {
            return parametersMap;
        }
    }

    public static List<String> extractParametersNames(String uriTemplate) {
        UriTemplate template = createUriTemplateOrGetFromCache(uriTemplate);
        return template.getTemplateVariables();
    }

    public static int getNumberOfExplicitRegexes(String uriTemplate) {
        UriTemplate template = createUriTemplateOrGetFromCache(uriTemplate);
        return template.getNumberOfExplicitRegexes();
    }

    private static UriTemplate createUriTemplateOrGetFromCache(String uriTemplate) {
        UriTemplate template = uriTemplatesCache.get(uriTemplate);
        if (template == null) {
            template = new UriTemplate(uriTemplate);
            uriTemplatesCache.put(uriTemplate, template);
        }
        return template;
    }
}
