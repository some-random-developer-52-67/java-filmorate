package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.validation.FilmValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class FilmService {

    private final FilmValidator filmValidator;

    private final Map<Long, Film> films = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized Film create(Film film) {
        filmValidator.validate(film);

        Film createdFilm = save(film, nextId++);
        log.info("Created film with id {}", createdFilm.getId());
        return createdFilm;
    }

    public synchronized Film update(Film film) {
        Long id = film.getId();

        if (id == null || id <= 0) {
            throw new ValidationException("Film id must be positive");
        }

        filmValidator.validate(film);

        if (!films.containsKey(id)) {
            throw new NotFoundException("Film with id %d was not found".formatted(id));
        }

        Film updatedFilm = save(film, id);
        log.info("Updated film with id {}", id);
        return updatedFilm;
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
