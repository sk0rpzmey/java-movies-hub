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
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore moviesStore;
    private final Gson gson;
    private static final int MIN_YEAR = 1888;

    public MoviesHandler(MoviesStore moviesStore) {
        this.moviesStore = moviesStore;
        gson = new Gson();
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        Endpoint endpoint = getEndpoint(
                ex.getRequestURI().getPath(),
                ex.getRequestMethod(),
                ex.getRequestURI().getQuery()
        );

        switch (endpoint) {
            case GET_MOVIES -> handleGetFilms(ex);
            case POST_MOVIE -> handlePostFilm(ex);
            case GET_MOVIE_BY_ID -> handleGetFilmById(ex);
            case DELETE_MOVIE -> handleDeleteFilm(ex);
            case GET_MOVIE_BY_YEAR -> handleGetFilmByYear(ex);
            case UNKNOWN -> sendJson(ex, 405, "Такого эндпоинта не существует");
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

    private void handleGetFilmById(HttpExchange ex) throws IOException {
        Optional<Integer> idOpt = getId(ex);
        if (idOpt.isEmpty()) {
            ErrorResponse error = new ErrorResponse("Некорректный ID",
                    List.of("ID, указанный в пути запроса, не число"));
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

        int id = idOpt.get();
        Movie movie = moviesStore.findById(id);
        if (movie == null) {
            ErrorResponse error = new ErrorResponse("Фильм не найден",
                    List.of("Фильм, по указанному в запросе id, не найден"));
            sendJson(ex, 404, gson.toJson(error));
            return;
        }
        sendJson(ex, 200, gson.toJson(movie));
    }

    private void handleDeleteFilm(HttpExchange ex) throws IOException {
        Optional<Integer> idOpt = getId(ex);
        if (idOpt.isEmpty()) {
            ErrorResponse error = new ErrorResponse("Некорректный ID",
                    List.of("ID, указанный в пути запроса, не число"));
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

        int id = idOpt.get();
        boolean isDeleted = moviesStore.deleteMovie(id);
        if (!isDeleted) {
            ErrorResponse error = new ErrorResponse("Фильм не найден",
                    List.of("Фильм, по указанному в запросе id, не найден"));
            sendJson(ex, 404, gson.toJson(error));
            return;
        }
        sendNoContent(ex, 204);
    }

    private void handleGetFilmByYear(HttpExchange ex) throws IOException {
        String query = ex.getRequestURI().getQuery();
        String queryParam = getQueryParam(query, "year");
        if (queryParam == null) {
            ErrorResponse error = new ErrorResponse("Некорректный параметр запроса — 'year'",
                    List.of("year, указанный в параметрах запроса, не число"));
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

        int year;
        try {
            year = Integer.parseInt(queryParam);
        } catch (NumberFormatException e) {
            ErrorResponse error = new ErrorResponse("Некорректный параметр запроса — 'year'",
                    List.of("year, указанный в параметрах запроса, не число"));
            sendJson(ex, 400, gson.toJson(error));
            return;
        }

        sendJson(ex, 200, gson.toJson(moviesStore.findByYear(year)));
    }

    private String getQueryParam(String query, String paramName) {
        if (query == null) {
            return null;
        }

        // Если нашел нужный параметр запроса, то возвращаю его значение
        String param = paramName + "=";
        for (String part : query.split("&")) {
            if (part.startsWith(param)) {
                return part.substring(param.length());
            }
        }

        return null;
    }

    private Optional<Integer> getId(HttpExchange ex) {
        String[] pathParts = ex.getRequestURI().getPath().split("/");
        try {
            return Optional.of(Integer.parseInt(pathParts[2]));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private Endpoint getEndpoint(String requestPath, String requestMethod, String query) {
        String[] pathParts = requestPath.split("/");

        if (pathParts.length == 2 && pathParts[1].equals("movies")) {
            if (requestMethod.equals("GET")) {
                if (query != null && query.startsWith("year=")) {
                    return Endpoint.GET_MOVIE_BY_YEAR;
                }
                return Endpoint.GET_MOVIES;
            }
            if (requestMethod.equals("POST")) {
                return Endpoint.POST_MOVIE;
            }
        }

        if (pathParts.length == 3 && pathParts[1].equals("movies")) {
            if (requestMethod.equals("GET")) {
                return Endpoint.GET_MOVIE_BY_ID;
            }
            if (requestMethod.equals("DELETE")) {
                return Endpoint.DELETE_MOVIE;
            }
        }
        return Endpoint.UNKNOWN;
    }

    private Movie parseMovie(InputStream inputStream) throws IOException {
        try (inputStream) {
            return gson.fromJson(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8), Movie.class);
        }
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
        if (year < MIN_YEAR || year >= currentYear + 1) {
            errors.add("Год должен быть между " + MIN_YEAR + " и " + (currentYear + 1));
        }
        return errors;
    }

    enum Endpoint {
        GET_MOVIES, POST_MOVIE, GET_MOVIE_BY_ID, DELETE_MOVIE, GET_MOVIE_BY_YEAR, UNKNOWN
    }
}
