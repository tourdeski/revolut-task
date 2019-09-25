package com.revolut.task.protocol;

import com.revolut.task.caller.RemoteMethod;
import com.revolut.task.caller.ServiceMethodCaller;
import com.revolut.task.service.RemoteService;
import com.revolut.task.service.Services;
import com.revolut.task.utils.JsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.http.protocol.HTTP;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Base http requests handler
 */
public class BaseHandler implements HttpHandler {

    private static final Logger LOG = LogManager.getLogger(BaseHandler.class);

    /**
     * Set of available service methods
     */
    private Map<String, ServiceMethodCaller> methodsByName = new ConcurrentHashMap<>();

    public void init() {
        for (Services services : Services.values()) {

            RemoteService service = services.getServiceInstance();
            for (Method method : service.getClass().getDeclaredMethods()) {

                if (method.isAnnotationPresent(RemoteMethod.class)) {
                    methodsByName.put(method.getName(), ServiceMethodCaller.create(service, method));
                }
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) {
        try {
            exchange.getResponseHeaders().set("Content-Type", "application/json");

            InputStream is = exchange.getRequestBody();
            Request request = JsonUtils.fromJson(new InputStreamReader(is, HTTP.UTF_8), Request.class);

            LOG.debug(String.format("Get request: %s", request));
            ServiceMethodCaller caller = resolveCaller(exchange);

            Object result = caller.call(request);
            String response = JsonUtils.toJson(result);

            OutputStream out = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, response.getBytes(HTTP.UTF_8).length);
            out.write(response.getBytes(HTTP.UTF_8));
            out.flush();
        } catch (Exception e) {
            handleException(exchange, e);
        } finally {
            exchange.close();
        }
    }

    /**
     * Search method caller by name from request path
     *
     * @return method caller
     * @throws NullPointerException if caller not found
     */
    @NotNull
    private ServiceMethodCaller resolveCaller(@NotNull HttpExchange exchange) {
        String serviceMethod = exchange.getRequestURI().getRawPath().split("/api/")[1];
        ServiceMethodCaller caller = methodsByName.get(serviceMethod);
        Objects.requireNonNull(caller, String.format("Resource not found: %s", serviceMethod));
        return caller;
    }

    private void handleException(@NotNull HttpExchange exchange, @NotNull Exception ex) {
        LOG.warn("Exception:", ex);
        String result = String.format("Internal server exception: %s", ex.getCause());
        try {
            exchange.sendResponseHeaders(500, result.getBytes().length);
            OutputStream output = exchange.getResponseBody();
            output.write(result.getBytes(HTTP.UTF_8));
            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
