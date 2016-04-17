package ru.spb.kupchinolabs.omnidrive;

import io.vertx.core.Vertx;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Created by inikolaev on 16/04/16.
 */
public class Main {
    public static void main(String[] args) throws UnsupportedEncodingException {
        String fileName = URLEncoder.encode("Мишки.jpg", "UTF-8");
        String payload = "{\"from\": \"" + fileName + "\", \"to\": \"test2\"}";

        Vertx.vertx().createHttpClient()
                .post(9090, "localhost", "/copy")
                .handler(res -> System.out.println(res))
                .putHeader("Content-length", Integer.toString(payload.length()))
                .putHeader("Content-type", "application/json")
                .write(payload)
                .end();


        Vertx.vertx().createHttpClient()
                .get(9090, "localhost", "/list")
                .handler(res -> {
                    res.bodyHandler(body -> System.out.println(body.toString()));
                })
                .putHeader("Content-length", Integer.toString(payload.length()))
                .putHeader("Content-type", "application/json")
                .write(payload)
                .end();
    }
}
