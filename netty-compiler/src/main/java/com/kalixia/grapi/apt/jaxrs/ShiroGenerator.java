package com.kalixia.grapi.apt.jaxrs;

import com.kalixia.grapi.ApiResponse;
import com.kalixia.grapi.codecs.shiro.ShiroHandler;
import com.squareup.javapoet.MethodSpec;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.subject.Subject;

import javax.ws.rs.core.MediaType;
import java.io.IOException;

@SuppressWarnings("PMD.DataflowAnomalyAnalysis")
public class ShiroGenerator {

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public static void generateShiroCodeForRequiresPermissionsCheck(MethodSpec.Builder builder, RequiresPermissions ann) throws IOException {
        String[] perms = ann.value();
        if (perms.length == 1) {
            builder.addStatement("subject.checkPermission($S)", perms[0]);
        } else {
            StringBuilder permsBuilder = new StringBuilder();
            for (int i = 0; i < perms.length; i++) {
                String perm = perms[i];
                permsBuilder.append('"');
                permsBuilder.append(perm);
                permsBuilder.append('"');
                if (i < perms.length - 1) {
                    permsBuilder.append(", ");
                }
            }
            String permsAsString = permsBuilder.toString();
            switch (ann.logical()) {
                case AND:
                    builder.addStatement("subject.checkPermissions($L)", permsAsString);
                    break;
                case OR:
                    builder
                            .addCode("// Avoid processing exceptions unnecessarily - \"delay\" throwing the exception by calling hasRole first\n")
                            .addStatement("boolean hasAtLeastOnePermission = false");
                    for (String perm : perms) {
                        builder
                                .beginControlFlow("if (subject.isPermitted($S))", perm)
                                    .addStatement("hasAtLeastOnePermission = true")
                                .endControlFlow();
                    }
                    builder
                            .addCode("// Cause the exception if none of the role match, note that the exception message will be a bit misleading\n")
                            .beginControlFlow("if (!hasAtLeastOnePermission)")
                                .addStatement("subject.checkPermission($S)", perms[0])
                            .endControlFlow();
                    break;
            }
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public static void generateShiroCodeForRequiresRolesCheck(MethodSpec.Builder builder, RequiresRoles ann) throws IOException {
        String[] roles = ann.value();
        if (roles.length == 1) {
            builder.addStatement("subject.checkRole($S)", roles[0]);
        } else {
            StringBuilder permsBuilder = new StringBuilder();
            for (int i = 0; i < roles.length; i++) {
                String perm = roles[i];
                permsBuilder.append('"');
                permsBuilder.append(perm);
                permsBuilder.append('"');
                if (i < roles.length - 1) {
                    permsBuilder.append(", ");
                }
            }
            String permsAsString = permsBuilder.toString();
            switch (ann.logical()) {
                case AND:
                    builder.addStatement("subject.checkRoles($L)", permsAsString);
                    break;
                case OR:
                    builder
                            .addCode("// Avoid processing exceptions unnecessarily - \"delay\" throwing the exception by calling hasRole first\n")
                            .addStatement("boolean hasAtLeastOneRole = false");
                    for (String perm : roles) {
                        builder
                                .beginControlFlow("if (subject.hasRole($S))", perm)
                                    .addStatement("hasAtLeastOneRole = true")
                                .endControlFlow();
                    }
                    builder
                            .addCode("// Cause the exception if none of the role match, note that the exception message will be a bit misleading\n")
                            .beginControlFlow("if (!hasAtLeastOneRole)")
                                .addStatement("subject.checkRole($S)", roles[0])
                            .endControlFlow();
                    break;
            }
        }
    }

    public static void generateShiroCodeForRequiresGuestCheck(MethodSpec.Builder builder, RequiresGuest ann) throws IOException {
        builder
                .beginControlFlow("if (subject.getPrincipal() != null)")
                .addStatement("throw new $T($S)",
                        UnauthenticatedException.class,
                        "Attempting to perform a guest-only operation.  "
                                + "The current Subject is not a guest (they have been authenticated or remembered from a previous login)."
                                + "  Access denied.")
                .endControlFlow();
    }

    public static void generateShiroCodeForRequiresUserCheck(MethodSpec.Builder builder, RequiresUser ann) throws IOException {
        builder
                .beginControlFlow("if (subject.getPrincipal() == null)")
                .addStatement("throw new $T($S)",
                        UnauthenticatedException.class,
                        "Attempting to perform a user-only operation.  "
                        + "The current Subject is not a user (they haven't been authenticated or remembered from a previous login)."
                        + "  Access denied.")
                .endControlFlow();
    }

    public static void generateShiroCodeForRequiresAuthenticationCheck(MethodSpec.Builder builder, RequiresAuthentication ann) throws IOException {
        builder
                .beginControlFlow("if (!subject.isAuthenticated())")
                .addStatement("throw new $T($S)", UnauthenticatedException.class,
                        "The current Subject is not authenticated.  Access denied.")
                .endControlFlow();
    }

    public static void beginSubject(MethodSpec.Builder builder) throws IOException {
        builder
                .addCode("// Retrieve authentication subject from the Channel\n")
                .beginControlFlow("try")
                    .addStatement("final $T subject = ctx.channel().attr($T.ATTR_SUBJECT).get()",
                            Subject.class, ShiroHandler.class)
                    .beginControlFlow("if (subject == null)")
                        .addStatement("throw new $T($S)", UnauthenticatedException.class, "Shiro Subject is null")
                    .endControlFlow();
    }

    public static void endSubject(MethodSpec.Builder builder) throws IOException {
        builder
                .nextControlFlow("catch (org.apache.shiro.authz.UnauthenticatedException e) ")
                    .addStatement("return new $T(request.id(), $T.FORBIDDEN, $T.copiedBuffer(e.getMessage(), charset), $T.TEXT_PLAIN)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                .nextControlFlow("catch (org.apache.shiro.UnavailableSecurityManagerException e) ")
                    .addStatement("LOGGER.error($S, e)", "Shiro does not seems to be configured.")
                    .addStatement("return new $T(request.id(), $T.INTERNAL_SERVER_ERROR, $T.EMPTY_BUFFER, $T.TEXT_PLAIN)",
                            ApiResponse.class, HttpResponseStatus.class, Unpooled.class, MediaType.class)
                .endControlFlow();
    }

}
