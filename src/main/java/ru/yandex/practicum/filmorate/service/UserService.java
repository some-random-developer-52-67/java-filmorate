package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.validation.UserValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserValidator userValidator;

    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized User create(User user) {
        userValidator.validate(user);

        User createdUser = save(user, nextId++);
        log.info("Created user with id {}", createdUser.getId());
        return createdUser;
    }

    public synchronized User update(User user) {
        Long id = user.getId();

        if (id == null || id <= 0) {
            throw new ValidationException("User id must be positive");
        }

        userValidator.validate(user);

        if (!users.containsKey(id)) {
            throw new NotFoundException("User with id %d was not found".formatted(id));
        }

        User updatedUser = save(user, id);
        log.info("Updated user with id {}", id);
        return updatedUser;
    }

    public synchronized List<User> findAll() {
        return users.values().stream()
                .map(user -> copyWithId(user, user.getId()))
                .toList();
    }

    private User save(User user, long id) {
        User savedUser = copyWithId(user, id);
        if (savedUser.getName() == null || savedUser.getName().isBlank()) {
            savedUser.setName(savedUser.getLogin());
        }

        users.put(id, savedUser);
        return copyWithId(savedUser, id);
    }

    private User copyWithId(User user, long id) {
        return new User(
                id,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday()
        );
    }
}
