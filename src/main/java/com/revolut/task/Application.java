package com.revolut.task;

import com.revolut.task.protocol.BaseHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Application {

    private static final Logger LOG = LogManager.getLogger(Application.class);

    /**
     * The Port number of running RESTfull service
     */
    private int port;
    private BaseHandler baseHandler;
    private HttpServer httpServer;

    public Application(int port) {
        this.port = port;
        this.baseHandler = new BaseHandler();
    }

    public void start() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(port), 0);

        // 200 just because apache Tomcat use 200 threads in pool by default
        httpServer.setExecutor(Executors.newFixedThreadPool(200, r -> {
            Thread thread = new Thread(r);
            // to see application threads in logs
            thread.setName(String.format("%s-%04d", "MainExecutor", thread.getId()));
            return thread;
        }));
        baseHandler.init();
        httpServer.createContext("/api", baseHandler::handle);
        httpServer.start();
        LOG.info(String.format("Server started successfully port:%d", port));
    }

    public void stop() {
        httpServer.stop(0);
        LOG.info("Server stopped");
    }
}
