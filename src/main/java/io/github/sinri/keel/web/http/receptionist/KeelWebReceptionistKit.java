package io.github.sinri.keel.web.http.receptionist;

import io.github.sinri.keel.web.http.ApiMeta;
import io.github.sinri.keel.web.http.prehandler.KeelPlatformHandler;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static io.github.sinri.keel.facade.KeelInstance.Keel;
import static io.github.sinri.keel.helper.KeelHelpersInterface.KeelHelpers;

/**
 * @param <R> class of a subclass of KeelWebReceptionist
 * @since 2.9.2
 * @since 3.0.0 TEST PASSED
 */
public class KeelWebReceptionistKit<R extends KeelWebReceptionist> {
    private final Router router;
    private final Class<R> classOfReceptionist;
    private final List<PlatformHandler> platformHandlers = new ArrayList<>();

    private final List<SecurityPolicyHandler> securityPolicyHandlers = new ArrayList<>();
    private final List<ProtocolUpgradeHandler> protocolUpgradeHandlers = new ArrayList<>();
    private final List<MultiTenantHandler> multiTenantHandlers = new ArrayList<>();
    /**
     * Tells who the user is
     */
    private final List<AuthenticationHandler> authenticationHandlers = new ArrayList<>();
    private final List<InputTrustHandler> inputTrustHandlers = new ArrayList<>();
    /**
     * Tells what the user is allowed to do
     */
    private final List<AuthorizationHandler> authorizationHandlers = new ArrayList<>();
    private final List<Handler<RoutingContext>> userHandlers = new ArrayList<>();
    private String uploadDirectory = BodyHandler.DEFAULT_UPLOADS_DIRECTORY;
    private String virtualHost = null;
    /**
     * @since 2.9.2
     */
    private Handler<RoutingContext> failureHandler = null;

    public KeelWebReceptionistKit(Class<R> classOfReceptionist, Router router) {
        this.classOfReceptionist = classOfReceptionist;
        this.router = router;
    }

    /**
     * Note: MAIN and TEST scopes are seperated.
     *
     * @param packageName the name of the package where the classes extending `R` are.
     * @since 3.2.11 Removed org.reflections:reflections, now self-implemented.
     */
    public void loadPackage(String packageName) {
        Set<Class<? extends R>> allClasses = Keel.reflectionHelper()
                .seekClassDescendantsInPackage(packageName, classOfReceptionist);

        try {
            allClasses.forEach(this::loadClass);
        } catch (Exception e) {
            Keel.getLogger().exception(e, r -> r.classification(getClass().getName(), "loadPackage"));
        }
    }

    public void loadClass(Class<? extends R> c) {
        ApiMeta[] apiMetaArray = KeelHelpers.reflectionHelper().getAnnotationsOfClass(c, ApiMeta.class);
        for (var apiMeta : apiMetaArray) {
            loadClass(c, apiMeta);
        }
    }

    private void loadClass(Class<? extends R> c, @Nonnull ApiMeta apiMeta) {
        Keel.getLogger().info(r -> r
                .classification(getClass().getName(), "loadClass")
                .message("Loading " + c.getName())
                .context(j -> {
                    JsonArray methods = new JsonArray();
                    Arrays.stream(apiMeta.allowMethods()).forEach(methods::add);
                    j.put("allowMethods", methods);
                    j.put("routePath", apiMeta.routePath());
                    if (apiMeta.isDeprecated()) {
                        j.put("isDeprecated", true);
                    }
                    if (apiMeta.remark() != null && !apiMeta.remark().isEmpty()) {
                        j.put("remark", apiMeta.remark());
                    }
                })
        );

        Constructor<? extends R> receptionistConstructor;
        try {
            receptionistConstructor = c.getConstructor(RoutingContext.class);
        } catch (NoSuchMethodException e) {
            Keel.getLogger().exception(e, r -> r.classification(getClass().getName(), "loadClass").message("HANDLER REFLECTION EXCEPTION"));
            return;
        }

        Route route = router.route(apiMeta.routePath());

        if (apiMeta.allowMethods() != null) {
            for (var methodName : apiMeta.allowMethods()) {
                route.method(HttpMethod.valueOf(methodName));
            }
        }

        if (apiMeta.virtualHost() != null && !apiMeta.virtualHost().isEmpty()) {
            route.virtualHost(apiMeta.virtualHost());
        } else if (this.virtualHost != null && !this.virtualHost.isEmpty()) {
            route.virtualHost(this.virtualHost);
        }

        // === HANDLERS WEIGHT IN ORDER ===
        // PLATFORM
        route.handler(new KeelPlatformHandler());
        if (apiMeta.timeout() > 0) {
            // PlatformHandler
            route.handler(TimeoutHandler.create(apiMeta.timeout(), apiMeta.statusCodeForTimeout()));
        }
        route.handler(ResponseTimeHandler.create());
        this.platformHandlers.forEach(route::handler);

        //    SECURITY_POLICY,
        // SecurityPolicyHandler
        // CorsHandler: Cross Origin Resource Sharing
        this.securityPolicyHandlers.forEach(route::handler);

        //    PROTOCOL_UPGRADE,
        protocolUpgradeHandlers.forEach(route::handler);
        //    BODY,
        if (apiMeta.requestBodyNeeded()) {
            route.handler(BodyHandler.create(uploadDirectory));
        }
        //    MULTI_TENANT,
        multiTenantHandlers.forEach(route::handler);
        //    AUTHENTICATION,
        authenticationHandlers.forEach(route::handler);
        //    INPUT_TRUST,
        inputTrustHandlers.forEach(route::handler);
        //    AUTHORIZATION,
        authorizationHandlers.forEach(route::handler);
        //    USER
        userHandlers.forEach(route::handler);

        // finally!
        route.handler(routingContext -> {
            try {
                R receptionist = receptionistConstructor.newInstance(routingContext);
                //receptionist.setApiMeta(apiMeta);
                receptionist.handle();
            } catch (Throwable e) {
                routingContext.fail(e);
            }
        });

        // failure handler since 2.9.2
        if (failureHandler != null) {
            route.failureHandler(failureHandler);
        }
    }

    /**
     * @since 2.9.2
     */
    public KeelWebReceptionistKit<R> setFailureHandler(Handler<RoutingContext> failureHandler) {
        this.failureHandler = failureHandler;
        return this;
    }


    public KeelWebReceptionistKit<R> addPlatformHandler(PlatformHandler handler) {
        this.platformHandlers.add(handler);
        return this;
    }

    public KeelWebReceptionistKit<R> addSecurityPolicyHandler(SecurityPolicyHandler handler) {
        this.securityPolicyHandlers.add(handler);
        return this;
    }

    public KeelWebReceptionistKit<R> addProtocolUpgradeHandler(ProtocolUpgradeHandler handler) {
        this.protocolUpgradeHandlers.add(handler);
        return this;
    }

    public KeelWebReceptionistKit<R> addMultiTenantHandler(MultiTenantHandler handler) {
        this.multiTenantHandlers.add(handler);
        return this;
    }

    /**
     * 追加一个认证校验器
     */
    public KeelWebReceptionistKit<R> addAuthenticationHandler(AuthenticationHandler handler) {
        this.authenticationHandlers.add(handler);
        return this;
    }

    public KeelWebReceptionistKit<R> addInputTrustHandler(InputTrustHandler handler) {
        this.inputTrustHandlers.add(handler);
        return this;
    }

    /**
     * 追加一个授权校验器
     */
    public KeelWebReceptionistKit<R> addAuthorizationHandler(AuthorizationHandler handler) {
        this.authorizationHandlers.add(handler);
        return this;
    }

    public KeelWebReceptionistKit<R> addUserHandler(Handler<RoutingContext> handler) {
        this.userHandlers.add(handler);
        return this;
    }

    public KeelWebReceptionistKit<R> setUploadDirectory(String uploadDirectory) {
        this.uploadDirectory = uploadDirectory;
        return this;
    }

    /**
     * @since 2.9
     */
    public KeelWebReceptionistKit<R> setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
        return this;
    }

}
