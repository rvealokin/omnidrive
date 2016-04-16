package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Vertx;

/**
 * Created by inikolaev on 16/04/16.
 */
public class Main {
    public static void main(String[] args) {
        String payload = "{\"from\": \"%D0%93%D0%BE%D1%80%D1%8B.jpg\", \"to\": \"test2\"}";

        Vertx.vertx().createHttpClient()
                .post(9090, "localhost", "/copy")
                .handler(res -> System.out.println(res))
                .putHeader("Content-length", Integer.toString(payload.length()))
                .putHeader("Content-type", "application/json")
                .write(payload)
                .end();
    }
}
