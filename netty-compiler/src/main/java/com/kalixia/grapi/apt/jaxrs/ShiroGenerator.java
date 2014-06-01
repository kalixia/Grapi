package com.kalixia.grapi.apt.jaxrs;

import com.kalixia.grapi.codecs.shiro.ShiroHandler;
import com.squareup.javawriter.JavaWriter;
import io.netty.channel.ChannelHandlerContext;
import org.apache.shiro.UnavailableSecurityManagerException;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.subject.Subject;

import java.io.IOException;

public class ShiroGenerator {

    public static void generateImports(JavaWriter writer) throws IOException {
        writer
                .emitImports(Subject.class)
                .emitImports(UnauthenticatedException.class)
                .emitImports(UnavailableSecurityManagerException.class)
                .emitImports(ShiroHandler.class)
                .emitImports(ChannelHandlerContext.class)
        ;
    }

    public static void generateShiroCodeForRequiresPermissionsCheck(JavaWriter writer, RequiresPermissions ann) throws IOException {
        String[] perms = ann.value();
        if (perms.length == 1) {
            writer.emitStatement("subject.checkPermission(\"%s\")", perms[0]);
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < perms.length; i++) {
                String perm = perms[i];
                builder.append('"');
                builder.append(perm);
                builder.append('"');
                if (i < perms.length - 1)
                    builder.append(", ");
            }
            String permsAsString = builder.toString();
            switch (ann.logical()) {
                case AND:
                    writer.emitStatement("subject.checkPermissions(%s)", permsAsString);
                    break;
                case OR:
                    writer
                            .emitSingleLineComment("Avoid processing exceptions unnecessarily - \"delay\" throwing the exception by calling hasRole first")
                            .emitStatement("boolean hasAtLeastOnePermission = false");
                    for (String perm : perms) {
                        writer
                                .beginControlFlow("if (subject.isPermitted(\"%s\"))", perm)
                                .emitStatement("hasAtLeastOnePermission = true")
                                .endControlFlow();
                    }
                    writer
                            .emitSingleLineComment("Cause the exception if none of the role match, note that the exception message will be a bit misleading")
                            .beginControlFlow("if (!hasAtLeastOnePermission)")
                            .emitStatement("subject.checkPermission(\"%s\")", perms[0])
                            .endControlFlow();
                    break;
            }
        }
    }

    public static void generateShiroCodeForRequiresRolesCheck(JavaWriter writer, RequiresRoles ann) throws IOException {
        String[] roles = ann.value();
        if (roles.length == 1) {
            writer.emitStatement("subject.checkRole(\"%s\")", roles[0]);
        } else {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < roles.length; i++) {
                String perm = roles[i];
                builder.append('"');
                builder.append(perm);
                builder.append('"');
                if (i < roles.length - 1)
                    builder.append(", ");
            }
            String permsAsString = builder.toString();
            switch (ann.logical()) {
                case AND:
                    writer.emitStatement("subject.checkRoles(%s)", permsAsString);
                    break;
                case OR:
                    writer
                            .emitSingleLineComment("Avoid processing exceptions unnecessarily - \"delay\" throwing the exception by calling hasRole first")
                            .emitStatement("boolean hasAtLeastOneRole = false");
                    for (String perm : roles) {
                        writer
                                .beginControlFlow("if (subject.hasRole(\"%s\"))", perm)
                                .emitStatement("hasAtLeastOneRole = true")
                                .endControlFlow();
                    }
                    writer
                            .emitSingleLineComment("Cause the exception if none of the role match, note that the exception message will be a bit misleading")
                            .beginControlFlow("if (!hasAtLeastOneRole)")
                            .emitStatement("subject.checkRole(\"%s\")", roles[0])
                            .endControlFlow();
                    break;
            }
        }
    }

    public static void generateShiroCodeForRequiresGuestCheck(JavaWriter writer, RequiresGuest ann) throws IOException {
        writer
                .beginControlFlow("if (subject.getPrincipal() != null)")
                .emitStatement("throw new UnauthenticatedException(\"Attempting to perform a guest-only operation.  The current Subject is \" +\n" +
                        "                    \"not a guest (they have been authenticated or remembered from a previous login).  Access \" +\n" +
                        "                    \"denied.\")")
                .endControlFlow();
    }

    public static void generateShiroCodeForRequiresUserCheck(JavaWriter writer, RequiresUser ann) throws IOException {
        writer
                .beginControlFlow("if (subject.getPrincipal() == null)")
                .emitStatement("throw new UnauthenticatedException(\"Attempting to perform a user-only operation.  The current Subject is \" +\n" +
                        "                    \"not a user (they haven't been authenticated or remembered from a previous login).  \" +\n" +
                        "                    \"Access denied.\")")
                .endControlFlow();
    }

    public static void generateShiroCodeForRequiresAuthenticationCheck(JavaWriter writer, RequiresAuthentication ann) throws IOException {
        writer
                .beginControlFlow("if (!subject.isAuthenticated())")
                .emitStatement("throw new UnauthenticatedException(\"The current Subject is not authenticated.  Access denied.\")")
                .endControlFlow();
    }

    public static void beginSubject(JavaWriter writer) throws IOException {
        writer
                .beginControlFlow("try")
                    .emitStatement("final Subject subject = ctx.channel().attr(ShiroHandler.ATTR_SUBJECT).get()");
    }

    public static void endSubject(JavaWriter writer) throws IOException {
        writer
                .nextControlFlow("catch (UnauthenticatedException e)")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.FORBIDDEN, " +
                                            "Unpooled.copiedBuffer(e.getMessage(), charset), MediaType.TEXT_PLAIN)")
                .nextControlFlow("catch (UnavailableSecurityManagerException e)")
                    .emitStatement("LOGGER.error(\"Shiro does not seems to be configured.\", e)")
                    .emitStatement("return new ApiResponse(request.id(), HttpResponseStatus.INTERNAL_SERVER_ERROR, " +
                            "Unpooled.EMPTY_BUFFER, MediaType.TEXT_PLAIN)")
                .endControlFlow();
    }

}
