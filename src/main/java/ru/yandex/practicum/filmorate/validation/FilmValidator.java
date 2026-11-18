package ru.yandex.practicum.filmorate.validation;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.time.LocalDate;
import java.time.Month;

@Component
public class FilmValidator {

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, Month.DECEMBER, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    public void validate(Film film) {
        String name = film.getName();
        if (name == null || name.isBlank()) {
            throw new ValidationException("Film name must not be blank");
        }

        String description = film.getDescription();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Film description must be at most %d characters".formatted(MAX_DESCRIPTION_LENGTH));
        }

        LocalDate releaseDate = film.getReleaseDate();
        if (releaseDate == null) {
            throw new ValidationException("Film release date is required");
        }
        if (releaseDate.isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Film release date must be on or after %s".formatted(MIN_RELEASE_DATE));
        }

        Integer duration = film.getDuration();
        if (duration == null || duration <= 0) {
            throw new ValidationException("Film duration must be positive");
        }
    }
}
