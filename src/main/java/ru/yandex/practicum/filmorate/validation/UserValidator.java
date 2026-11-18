package ru.yandex.practicum.filmorate.validation;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

@Component
public class UserValidator {

    public void validate(User user) {
        String email = user.getEmail();
        if (email == null || !email.contains("@")) {
            throw new ValidationException("User email must contain @");
        }

        String login = user.getLogin();
        if (login == null || login.isEmpty() || login.codePoints().anyMatch(Character::isWhitespace)) {
            throw new ValidationException("User login must be non-empty and contain no whitespace");
        }

        LocalDate birthday = user.getBirthday();
        if (birthday == null) {
            throw new ValidationException("User birthday is required");
        }
        if (birthday.isAfter(LocalDate.now())) {
            throw new ValidationException("User birthday must not be in the future");
        }
    }
}
