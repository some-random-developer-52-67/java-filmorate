package ru.yandex.practicum.filmorate.validation;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.Month;

@Component
public class FilmValidator {

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, Month.DECEMBER, 28);

    public void validate(Film film) {
        LocalDate releaseDate = film.getReleaseDate();

        if (releaseDate == null || releaseDate.isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Film release date must be on or after %s".formatted(MIN_RELEASE_DATE));
        }
    }
}
