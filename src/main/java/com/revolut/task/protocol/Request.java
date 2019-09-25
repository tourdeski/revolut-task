package com.revolut.task.protocol;

import org.jetbrains.annotations.Nullable;

public class Request {

    @Nullable
    private String body;

    @Nullable
    public String getBody() {
        return body;
    }

    public void setBody(@Nullable String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "Request{" +
                "body='" + body + '\'' +
                '}';
    }
}
