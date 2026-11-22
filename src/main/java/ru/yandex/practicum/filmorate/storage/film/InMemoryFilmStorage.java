package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized Film create(Film film) {
        return save(film, nextId++);
    }

    @Override
    public synchronized Film update(Film film) {
        long id = film.getId();
        getStoredFilm(id);
        return save(film, id);
    }

    @Override
    public synchronized void deleteById(long id) {
        getStoredFilm(id);
        films.remove(id);
    }

    @Override
    public synchronized Film getById(long id) {
        return copy(getStoredFilm(id));
    }

    @Override
    public synchronized List<Film> findAll() {
        return films.values().stream()
                .map(this::copy)
                .toList();
    }

    private Film getStoredFilm(long id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Film with id %d was not found".formatted(id));
        }

        return film;
    }

    private Film save(Film film, long id) {
        Film savedFilm = copyWithId(film, id);
        films.put(id, savedFilm);
        return copy(savedFilm);
    }

    private Film copy(Film film) {
        return copyWithId(film, film.getId());
    }

    private Film copyWithId(Film film, long id) {
        return new Film(
                id,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration()
        );
    }

}
