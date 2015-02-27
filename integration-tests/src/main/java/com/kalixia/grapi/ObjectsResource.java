package com.kalixia.grapi;

import com.kalixia.grapi.codecs.jaxrs.UriTemplateUtils;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

@Singleton
@Path("/objects")
@Produces(MediaType.APPLICATION_JSON)
@SuppressWarnings("PMD.UnnecessaryConstructor")
public class ObjectsResource {
    private final List<Test> tests = new ArrayList<>();

    @Inject
    public ObjectsResource() {
    }

    @GET
    public List<Test> getAllTests() {
        LoggerFactory.getLogger(getClass()).info("Returning tests: " + tests);
        return tests;
    }

    @GET
    @Path("/count")
    public Integer getCountOfTests() {
        return tests.size();
    }

    @POST
    public Response addTest(@Valid Test test) throws URISyntaxException {
        tests.add(test);
        URI testURI = new URI(UriTemplateUtils.createURI("/objects/{test}", test.getName()));
        return Response.created(testURI).build();
    }

    @SuppressWarnings({"PMD.OnlyOneReturn", "PMD.IfStmtsMustUseBraces"})
    public static class Test {
        @NotNull
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Test{");
            sb.append("name='").append(name).append('\'');
            sb.append('}');
            return sb.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Test)) return false;

            Test test = (Test) o;

            if (name != null ? !name.equals(test.name) : test.name != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            return name != null ? name.hashCode() : 0;
        }
    }
}
