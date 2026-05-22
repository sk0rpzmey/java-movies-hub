package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8"; // Cодержимое заголовка Content-Type

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        // Общий для всех хендлеров метод
        // для отправки ответа с телом в формате JSON
        try (OutputStream os = ex.getResponseBody()) {
            ex.getResponseHeaders().set("Content-Type", CT_JSON);
            ex.sendResponseHeaders(status, 0);
            os.write(json.getBytes(StandardCharsets.UTF_8));
        }
    }

    protected void sendNoContent(HttpExchange ex, int status) throws IOException {
        // Общий для всех хендлеров метод
        // для отправки ответа без тела и кодом 204
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        ex.sendResponseHeaders(status, -1);
    }
}