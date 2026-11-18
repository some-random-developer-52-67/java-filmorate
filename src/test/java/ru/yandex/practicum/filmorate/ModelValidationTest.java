package ru.yandex.practicum.filmorate;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validation.FilmValidator;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelValidationTest {

    private static final Clock VALIDATION_CLOCK = Clock.fixed(
            Instant.parse("2026-01-01T12:00:00Z"),
            ZoneOffset.UTC
    );

    private static final ValidatorFactory VALIDATOR_FACTORY = Validation.byDefaultProvider()
            .configure()
            .clockProvider(() -> VALIDATION_CLOCK)
            .buildValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    private final FilmValidator filmValidator = new FilmValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void acceptsFilmOnValidationBoundaries() {
        Film film = validFilm();
        film.setDescription("a".repeat(200));
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        film.setDuration(1);


        assertThat(VALIDATOR.validate(film)).isEmpty();
        assertDoesNotThrow(() -> filmValidator.validate(film));
    }

    @Test
    void acceptsMissingFilmDescription() {
        Film film = validFilm();


        assertThat(VALIDATOR.validate(film)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t"})
    void rejectsBlankFilmName(String name) {
        Film film = validFilm();
        film.setName(name);


        assertInvalidField(film, "name");
    }

    @Test
    void rejectsDescriptionLongerThan200Characters() {
        Film film = validFilm();
        film.setDescription("a".repeat(201));


        assertInvalidField(film, "description");
    }

    @Test
    void rejectsMissingReleaseDate() {
        Film film = validFilm();
        film.setReleaseDate(null);


        assertInvalidField(film, "releaseDate");
    }

    @Test
    void rejectsReleaseDateBefore1895December28() {
        Film film = validFilm();
        film.setReleaseDate(LocalDate.of(1895, 12, 27));


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


        assertInvalidField(film, "duration");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "mail.example.com", "incomplete@"})
    void rejectsBlankOrMalformedEmail(String email) {
        User user = validUser();
        user.setEmail(email);


        assertInvalidField(user, "email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "user name", "user\tname"})
    void rejectsBlankLoginOrLoginWithWhitespace(String login) {
        User user = validUser();
        user.setLogin(login);


        assertInvalidField(user, "login");
    }

    @Test
    void acceptsBirthdayToday() {
        User user = validUser();
        user.setBirthday(LocalDate.now(VALIDATION_CLOCK));


        assertThat(VALIDATOR.validate(user)).isEmpty();
    }

    @Test
    void rejectsBirthdayTomorrow() {
        User user = validUser();
        user.setBirthday(LocalDate.now(VALIDATION_CLOCK).plusDays(1));


        assertInvalidField(user, "birthday");
    }

    @Test
    void rejectsMissingBirthday() {
        User user = validUser();
        user.setBirthday(null);


        assertInvalidField(user, "birthday");
    }

    private void assertInvalidField(Object model, String field) {
        assertThat(VALIDATOR.validate(model))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly(field);
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
