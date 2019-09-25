package com.revolut.task.caller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.revolut.task.protocol.Request;
import com.revolut.task.service.RemoteService;
import com.revolut.task.utils.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Proxy for service method.
 */
public class ServiceMethodCaller {

    private static final Logger LOG = LogManager.getLogger(ServiceMethodCaller.class);

    @NotNull
    private final RemoteService service;
    @NotNull
    private final Method method;
    @NotNull
    private final String[] argumentNames;
    @NotNull
    private final Type[] argumentTypes;


    private ServiceMethodCaller(@NotNull RemoteService service,
                                @NotNull Method method,
                                @NotNull String[] argumentNames,
                                @NotNull Type[] argumentTypes) {
        this.service = service;
        this.method = method;
        this.argumentNames = argumentNames;
        this.argumentTypes = argumentTypes;
    }

    public static ServiceMethodCaller create(@NotNull RemoteService remoteService, @NotNull Method method) {
        String[] argumentNames = getArgumentNames(method);
        Type[] argumentTypes = method.getGenericParameterTypes();
        return new ServiceMethodCaller(remoteService, method, argumentNames, argumentTypes);
    }

    /**
     * Read method parameters
     *
     * @return name of parameters
     */
    @NotNull
    private static String[] getArgumentNames(@NotNull Method method) {
        Annotation[][] ann = method.getParameterAnnotations();
        String[] argNames = new String[ann.length];

        for (int i = 0; i < ann.length; i++) {
            for (Annotation a : ann[i]) {
                if (a instanceof Arg) {
                    argNames[i] = ((Arg) a).value();
                }
            }
            if (argNames[i] == null) {
                throw new IllegalArgumentException(
                        String.format("Annotation @Arg not present for parameter #%s of method %s"
                                , i, method));
            }
        }
        return argNames;
    }

    /**
     * Service method invocation
     *
     * @throws Exception
     */
    @Nullable
    public Object call(@NotNull Request requestApp) throws Exception {
        Map<String, Object> argsMap = readParams(requestApp);
        LOG.debug(String.format("Invoking %s.%s() with args:%s", service.getName(), method.getName(), argsMap));

        return method.invoke(service, argsMap.values().toArray());
    }

    /**
     * Read parameters from request
     *
     * @return Map: parameter name -> parameter value
     */
    @NotNull
    private Map<String, Object> readParams(@NotNull Request requestApp) {
        Map<String, Object> result = new LinkedHashMap<>();

        JsonObject jsonObject = JsonUtils.toJsonObject(requestApp.getBody());

        for (int i = 0; i < argumentTypes.length; i++) {
            String name = argumentNames[i];
            Type type = argumentTypes[i];
            JsonElement e = jsonObject.get(name);
            Object arg = e == null ? null : JsonUtils.fromJson(e, type);
            result.put(name, arg);
        }
        return result;
    }
}
