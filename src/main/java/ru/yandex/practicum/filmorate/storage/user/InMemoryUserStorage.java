package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized User create(User user) {
        return save(user, nextId++);
    }

    @Override
    public synchronized User update(User user) {
        long id = user.getId();
        getStoredUser(id);
        return save(user, id);
    }

    @Override
    public synchronized void deleteById(long id) {
        getStoredUser(id);
        users.remove(id);
    }

    @Override
    public synchronized User getById(long id) {
        return copy(getStoredUser(id));
    }

    @Override
    public synchronized List<User> findAll() {
        return users.values().stream()
                .map(this::copy)
                .toList();
    }

    private User getStoredUser(long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User with id %d was not found".formatted(id));
        }

        return user;
    }

    private User save(User user, long id) {
        User savedUser = copyWithId(user, id);
        users.put(id, savedUser);
        return copy(savedUser);
    }

    private User copy(User user) {
        return copyWithId(user, user.getId());
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
