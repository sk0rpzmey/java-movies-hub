package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoviesStore {
    private final Map<Integer, Movie> moviesStore;
    private int id = 1;

    public MoviesStore() {
         moviesStore = new HashMap<>();
    }

    public Movie add(Movie movie) {
        Movie movieWithId = new Movie(id++, movie.title(), movie.year());
        moviesStore.put(movieWithId.id(), movieWithId);
        return movieWithId;
    }

    public List<Movie> getAll() {
        return List.copyOf(moviesStore.values());
    }

    public void clear() {
        id = 1;
        moviesStore.clear();
    }
//
//    public Movie findById(int id) {
//        return null;
//    }
//
//    public boolean deleteMovie(int id) {
//        return false;
//    }
//
//    public List<Movie> findByYear(int year) {
//        return null;
//    }
}