package com.kalixia.rawsag.apt.jaxrs;

import com.kalixia.rawsag.codecs.jaxrs.UriTemplateUtils;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

/**
 * Comparator taking into account JAX-RS Path precedence.
 *
 * @see <a href="http://www.j2eeprogrammer.com/2010/11/jax-rs-path-precedence-rules.html">JAX-RS Path precedence rules</a>
 */
class UriTemplatePrecedenceComparator implements Comparator<String>, Serializable {
    @Override
    public int compare(String uriTemplate1, String uriTemplate2) {

        // sort by the number of literal characters contained within the expression
        if (uriTemplate1.length() > uriTemplate2.length()) {
            return 1;
        }

        // sort by the number of template expressions within the expression
        List<String> expressions1 = UriTemplateUtils.extractParametersNames(uriTemplate1);
        List<String> expressions2 = UriTemplateUtils.extractParametersNames(uriTemplate2);
        if (expressions1.size() > expressions2.size()) {
            return 1;
        }

        // sort by the number of regular expressions contained within the expression
        return Integer.compare(
                UriTemplateUtils.getNumberOfExplicitRegexes(uriTemplate1),
                UriTemplateUtils.getNumberOfExplicitRegexes(uriTemplate2));
    }
}
