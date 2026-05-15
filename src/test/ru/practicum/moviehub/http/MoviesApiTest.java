package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080"; // !!! добавьте базовую часть URL
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore moviesStore;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        // Хранилище
        moviesStore = new MoviesStore();

        // HTTP-сервер
        server = new MoviesServer(moviesStore, 8080);
        server.start();

        // HTTP-клиент
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        // Gson
        gson = new Gson();
    }

    @BeforeEach
    void beforeEach() {
        moviesStore.clear();
    }

    @AfterAll
    static void afterAll() {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        // Объект GET-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Ответ
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // Проверка, что был возвращён массив
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    void getMovies_withData_returnsList() throws Exception {
        // Добавляю фильм напрямую
        Movie movie = moviesStore.add(new Movie(0, "Film1", 1999));

        // Объект GET-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(BASE + "/movies"))
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Ответ
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // Проверка наличия фильма
        List<Movie> body = gson.fromJson(resp.body().trim(), new ListOfMoviesTypeToken().getType());
        assertEquals(1, body.size(),
                "Ожидается JSON-массив");

        // Проверка правильности полей тестового фильма
        Movie actualMovie = body.getFirst();
        assertEquals(movie.title(), actualMovie.title());
        assertEquals(movie.id(), actualMovie.id());
        assertEquals(movie.year(), actualMovie.year());
    }

    @Test
    void postMovies_withCorrectData_returns201AndCreatedMovie() throws Exception {
        // Новый фильм в формате Json
        String movieJson = "{\"title\":\"Film1\",\"year\":1999}";

        // Объект POST-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // Проверка наличия фильма
        assertEquals(1, moviesStore.getAll().size(), "Ожидается добавление 1 фильма в хранилище");

        //Проверка корректности данных
        Movie body = gson.fromJson(resp.body().trim(), Movie.class);
        assertEquals("Film1", body.title(), "title должен быть Film1");
        int actualId = body.id();
        assertTrue(actualId > 0, "id должен быть больше 0");
        assertEquals(1999, body.year(), "year должен быть 1999");
    }

    @Test
    void postMovies_withEmptyTitle_returns422() throws Exception {
        // Новый фильм в формате Json
        String movieJson = "{\"title\":\"\",\"year\":1999}";

        // Объект POST-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        //Проверка соответствия ошибки
        ErrorResponse body = gson.fromJson(resp.body().trim(), ErrorResponse.class);
        assertEquals("Ошибка валидации", body.getError());
        // Должен вернуть(Название не должно быть пустым)
        assertTrue(body.getDetails().contains("Название не должно быть пустым"));

        // Проверка отсутсвия фильма
        assertEquals(0, moviesStore.getAll().size(), "Ожидается пустое хранилище");
    }

    @Test
    void postMovies_withTitleMoreThen100Char_returns422() throws Exception {
        String title = "Lorem ipsum dolor sit amet, " +
                "consectetur adipiscing elit. Sed massa arcu, " +
                "elementum vel risus ligula.103";
        // Новый фильм в формате Json
        String movieJson = "{\"title\":\"" + title + "\",\"year\":1999}";

        // Объект POST-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        //Проверка соответствия ошибки
        ErrorResponse body = gson.fromJson(resp.body().trim(), ErrorResponse.class);
        assertEquals("Ошибка валидации", body.getError());
        // Должен вернуть(Название не должно быть пустым)
        assertTrue(body.getDetails().contains("Название не должно быть длиной больше ста символов"));

        // Проверка отсутсвия фильма
        assertEquals(0, moviesStore.getAll().size(), "Ожидается пустое хранилище");
    }

    @Test
    void postMovies_withYearLess1888_returns422() throws Exception {
        // Новый фильм в формате Json
        String movieJson = "{\"title\":\"Film1\",\"year\":1887}";

        // Объект POST-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(422, resp.statusCode(), "POST /movies должен вернуть 422");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        //Проверка соответствия ошибки
        ErrorResponse body = gson.fromJson(resp.body().trim(), ErrorResponse.class);
        assertEquals("Ошибка валидации", body.getError());
        // Должен вернуть(Название не должно быть пустым)
        int currentYear = Year.now().getValue();
        assertTrue(body.getDetails().contains("Год должен быть между 1888 и " + (currentYear + 1)));

        // Проверка отсутсвия фильма
        assertEquals(0, moviesStore.getAll().size(), "Ожидается пустое хранилище");
    }

    @Test
    void postMovies_withDifferentContentType_returns415() throws Exception {
        // Новый фильм в формате Json
        String movieJson = "{\"title\":\"Film1\",\"year\":1887}";

        // Объект POST-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(415, resp.statusCode(), "POST /movies должен вернуть 415");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        // Проверка отсутсвия фильма
        assertEquals(0, moviesStore.getAll().size(), "Ожидается пустое хранилище");
    }

    @Test
    void postMovies_withInvalidJson_returns400() throws Exception {
        // Новый фильм в формате Json
        String movieJson = "{\"title\"\"Film1\",\"year\":1887}";

        // Объект POST-запроса на эндпоинт /movies
        HttpRequest req = HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofString(movieJson))
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();

        // Обработчик тела запроса
        HttpResponse.BodyHandler<String> responseBodyHandler =
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8);

        // Запрос
        HttpResponse<String> resp = client.send(req, responseBodyHandler);

        // Проверка кода ответа
        assertEquals(400, resp.statusCode(), "POST /movies должен вернуть 400");

        // Проверка заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8", contentTypeHeaderValue,
                "Content-Type должен содержать формат данных и кодировку");

        //Проверка соответствия ошибки
        ErrorResponse body = gson.fromJson(resp.body().trim(), ErrorResponse.class);
        assertEquals("Некорректный JSON", body.getError());
        // Должен вернуть(Название не должно быть пустым)
        assertTrue(body.getDetails().contains("Тело запроса не является валидным JSON"));

        // Проверка отсутсвия фильма
        assertEquals(0, moviesStore.getAll().size(), "Ожидается пустое хранилище");
    }
}

