package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    public synchronized User create(User user) {
        return save(user, nextId++);
    }

    public synchronized User update(User user) {
        Long id = user.getId();

        if (id == null || id <= 0) {
            throw new ValidationException("User id must be positive");
        }

        if (!users.containsKey(id)) {
            throw new NotFoundException("User with id %d was not found".formatted(id));
        }

        return save(user, id);
    }

    public synchronized List<User> findAll() {
        return users.values().stream()
                .map(user -> copyWithId(user, user.getId()))
                .toList();
    }

    private User save(User user, long id) {
        User savedUser = copyWithId(user, id);
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
