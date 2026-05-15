package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Endpoint endpoint = getEndpoint(ex.getRequestURI().getPath(), ex.getRequestMethod());

        switch (endpoint) {
            case GET_MOVIES -> handleGetFilms(ex);
            case POST_MOVIE -> handlePostFilm(ex);
            default -> sendJson(ex, 404, "Такого эндпоинта не существует");
        }
    }

    private void handleGetFilms(HttpExchange ex) throws IOException {
        String response = gson.toJson(moviesStore.getAll());
        sendJson(ex, 200, response);
    }

    private void handlePostFilm(HttpExchange ex) throws IOException {
        // Проверка заголовка Content-type
        if (!checkHeader(ex)) {
            sendNoContent(ex, 415);
            return;
        }

        Movie movie;
        try {
            // Парсинг
            movie = parseMovie(ex.getRequestBody());
            if (movie == null) {
                throw new IllegalArgumentException();
            }
        } catch (JsonSyntaxException | IllegalArgumentException e) {
            ErrorResponse error = new ErrorResponse("Некорректный JSON",
                    List.of("Тело запроса не является валидным JSON"));
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

        // Валидация(проверка на соответствие требованиям)
        List<String> validationErrors = validateMovie(movie);
        if (!validationErrors.isEmpty()) {
            ErrorResponse error = new ErrorResponse("Ошибка валидации", validationErrors);
            sendJson(ex, 422, gson.toJson(error));
            return;
        }

        Movie savedMovie = moviesStore.add(movie);
        sendJson(ex, 201, gson.toJson(savedMovie));
    }

    private Endpoint getEndpoint(String requestPath, String requestMethod) {
        String[] pathParts = requestPath.split("/");

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            if (requestMethod.equals("GET")) {
                return Endpoint.GET_MOVIES;
            }
            if (requestMethod.equals("POST")) {
                return Endpoint.POST_MOVIE;
            }
        }
        return Endpoint.UNKNOWN;
    }

    private Movie parseMovie(InputStream inputStream) throws IOException {
        return gson.fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), Movie.class);
    }

    // Метод для проверки заголовка Content-type
    private boolean checkHeader(HttpExchange ex) {
        return ex.getRequestHeaders().entrySet().stream()
                .filter(entry -> "Content-Type".equalsIgnoreCase(entry.getKey()))
                .flatMap(entry -> entry.getValue().stream())
                .anyMatch(value -> value.contains("application/json") && value.contains("charset=UTF-8"));
    }

    // Метод для проверки соответствия тела запроса(movie) требования
    private List<String> validateMovie(Movie movie) {
        List<String> errors = new ArrayList<>();
        String title = movie.title();
        int year = movie.year();

        if (title.isEmpty()) {
            errors.add("Название не должно быть пустым");
        } else if (title.length() > 100) {
            errors.add("Название не должно быть длиной больше ста символов");
        }

        int currentYear = Year.now().getValue();
        if (year < 1888 || year >= currentYear + 1) {
            errors.add("Год должен быть между 1888 и " + (currentYear + 1));
        }
        return errors;
    }

    enum Endpoint {GET_MOVIES, POST_MOVIE, GET_MOVIE_BY_ID, DELETE_MOVIE, GET_MOVIE_BY_YEAR, UNKNOWN}
}
