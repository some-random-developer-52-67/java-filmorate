package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validation.FilmValidator;
import ru.yandex.practicum.filmorate.validation.UserValidator;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelValidationTest {

    private final FilmValidator filmValidator = new FilmValidator();
    private final UserValidator userValidator = new UserValidator();

    @Test
    void acceptsFilmOnValidationBoundaries() {
        Film film = validFilm();
        film.setDescription("a".repeat(200));
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(1);


        assertDoesNotThrow(() -> filmValidator.validate(film));
    }

    @Test
    void acceptsMissingFilmDescription() {
        Film film = validFilm();


        assertDoesNotThrow(() -> filmValidator.validate(film));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t"})
    void rejectsBlankFilmName(String name) {
        Film film = validFilm();
        film.setName(name);


        assertThrows(
                ValidationException.class,
                () -> filmValidator.validate(film)
        );
    }

    @Test
    void rejectsDescriptionLongerThan200Characters() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));


        assertThrows(
                ValidationException.class,
                () -> filmValidator.validate(film)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"1895-12-27"})
    void rejectsMissingOrTooEarlyReleaseDate(String releaseDate) {
        Film film = validFilm();
        film.setReleaseDate(releaseDate == null
                ? null
                : LocalDate.parse(releaseDate)
        );


        assertThrows(
                ValidationException.class,
                () -> filmValidator.validate(film)
        );
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = {0, -1})
    void rejectsMissingOrNonPositiveDuration(Integer duration) {
        Film film = validFilm();
        film.setDuration(duration);


        assertThrows(
                ValidationException.class,
                () -> filmValidator.validate(film)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "mail.example.com"})
    void rejectsBlankEmailOrEmailWithoutAtSign(String email) {
        User user = validUser();
        user.setEmail(email);


        assertThrows(
                ValidationException.class,
                () -> userValidator.validate(user)
        );
    }

    @Test
    void acceptsEmailContainingAtSignInBasicValidation() {
        User user = validUser();
        user.setEmail("incomplete@");


        assertDoesNotThrow(() -> userValidator.validate(user));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "user name", "user\tname"})
    void rejectsBlankLoginOrLoginWithWhitespace(String login) {
        User user = validUser();
        user.setLogin(login);


        assertThrows(
                ValidationException.class,
                () -> userValidator.validate(user)
        );
    }

    @Test
    void acceptsBirthdayToday() {
        User user = validUser();
        user.setBirthday(LocalDate.now());


        assertDoesNotThrow(() -> userValidator.validate(user));
    }

    @Test
    void rejectsBirthdayTomorrow() {
        User user = validUser();
        user.setBirthday(LocalDate.now().plusDays(1));


        assertThrows(
                ValidationException.class,
                () -> userValidator.validate(user)
        );
    }

    @Test
    void rejectsMissingBirthday() {
        User user = validUser();
        user.setBirthday(null);


        assertThrows(
                ValidationException.class,
                () -> userValidator.validate(user)
        );
    }

    private Film validFilm() {
        return new Film(
                null,
                "Film",
                null,
                LocalDate.of(2000, 1, 1),
                100
        );
    }

    private User validUser() {
        return new User(
                null,
                "user@example.com",
                "user",
                null,
                LocalDate.of(2000, 1, 1)
        );
    }
}
