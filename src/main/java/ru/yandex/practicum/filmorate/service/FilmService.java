package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class FilmService {

    private final Map<Long, Film> films = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized Film create(Film film) {
        return save(film, nextId++);
    }

    public synchronized Film update(Film film) {
        Long id = film.getId();

        if (id == null || id <= 0) {
            throw new ValidationException("Film id must be positive");
        }

        if (!films.containsKey(id)) {
            throw new NotFoundException("Film with id %d was not found".formatted(id));
        }

        return save(film, id);
    }

    public synchronized List<Film> findAll() {
        return films.values().stream()
                .map(film -> copyWithId(film, film.getId()))
                .toList();
    }

    private Film save(Film film, long id) {
        Film savedFilm = copyWithId(film, id);
        films.put(id, savedFilm);
        return copyWithId(savedFilm, id);
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
