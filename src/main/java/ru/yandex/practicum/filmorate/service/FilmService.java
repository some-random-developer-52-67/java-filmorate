package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.validation.FilmValidator;

import java.util.Comparator;
import java.util.List;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final FilmValidator filmValidator;

    public Film create(Film film) {
        filmValidator.validate(film);

        Film createdFilm = filmStorage.create(film);
        log.info("Created film with id {}", createdFilm.getId());
        return createdFilm;
    }

    public Film update(Film film) {
        Long id = film.getId();

        if (id == null || id <= 0) {
            throw new ValidationException("Film id must be positive");
        }

        filmValidator.validate(film);

        Film updatedFilm = filmStorage.update(film);
        log.info("Updated film with id {}", id);
        return updatedFilm;
    }

    public Film getById(long id) {
        return filmStorage.getById(id);
    }

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public void addLike(long filmId, long userId) {
        userStorage.addFilmLike(userId, filmId);
        log.debug("Film {} has a like from user {}", filmId, userId);
    }

    public void removeLike(long filmId, long userId) {
        userStorage.removeFilmLike(userId, filmId);
        log.debug("Film {} has no like from user {}", filmId, userId);
    }

    public List<Film> findPopular(int count) {
        if (count < 0) {
            throw new ValidationException("Count must be non-negative");
        }

        Comparator<Film> byPopularity = Comparator.<Film>comparingInt(film -> film.getLikedUserIds().size())
                .reversed()
                .thenComparing(Film::getId);

        return filmStorage.findAll().stream()
                .sorted(byPopularity)
                .limit(count)
                .toList();
    }

}
