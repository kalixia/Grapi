package com.kalixia.grapi;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresGuest;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.apache.shiro.authz.annotation.RequiresUser;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/secured/echo")
@Produces(MediaType.TEXT_PLAIN)
public class SecuredEchoResource {

    @Inject
    public SecuredEchoResource() {
    }

    @Path("{message}")
    @GET
    @RequiresPermissions("echo")
    public String echo(@PathParam("message") @NotNull String message) {
        return message;
    }

    @Path("/")
    @GET
    @RequiresGuest
    public String guestEcho() {
        return "guest";
    }

    @Path("/authenticated")
    @GET
    @RequiresAuthentication
    public String welcomeAuthenticated() {
        return "Welcome " + SecurityUtils.getSubject().getPrincipal();
    }

    @Path("/user")
    @GET
    @RequiresUser
    public String welcomeUser() {
        return "Hello " + SecurityUtils.getSubject().getPrincipal() + "!";
    }

    @Path("/admin")
    @GET
    @RequiresRoles("admin")
    public String welcomeAdmin() {
        return "Hello Administrator!";
    }

    @Path("/admin-or-user")
    @GET
    @RequiresRoles(value = {"admin", "user"}, logical = Logical.OR)
    public String welcomeAdminOrUser() {
        return "Hello Administrator!";
    }

    @Path("/admin-and-user")
    @GET
    @RequiresRoles({"admin", "user"})
    public String welcomeAdminAndUser() {
        return "Hello Administrator!";
    }

    @Path("two-permissions/{message}")
    @GET
    @RequiresPermissions({"echo", "move-than-one-perm"})
    public String twoPermissionsRequired(@PathParam("message") @NotNull String message) {
            return message;
        }

    @Path("many-permissions/{message}")
    @GET
    @RequiresPermissions({"echo", "move-than-one-perm", "another-perm"})
    public String moreThanOnePermissionRequired(@PathParam("message") @NotNull String message) {
            return message;
        }

    @Path("any-permission/{message}")
    @GET
    @RequiresPermissions(value = {"echo", "move-than-one-perm", "another-perm"}, logical = Logical.OR)
    public String atLeastOnePermissionRequired(@PathParam("message") @NotNull String message) {
            return message;
        }

}
