package com.revolut.task.utils;

import com.google.gson.*;

import java.io.Reader;
import java.lang.reflect.Type;

public class JsonUtils {

    private static Gson gson = new GsonBuilder().create();
    private static JsonParser jsonParser = new JsonParser();

    public static <T> T fromJson(Reader reader, Class<T> type) {
        return gson.fromJson(reader, type);
    }



    public static String toJson(Object o) {
        return gson.toJson(o);
    }

    public static JsonObject toJsonObject(String json) {
        return jsonParser.parse(json).getAsJsonObject();
    }

    public static Object fromJson(JsonElement e, Type type) {
        return gson.fromJson(e, type);
    }
}
