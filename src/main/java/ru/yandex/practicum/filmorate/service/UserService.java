package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Service
public class UserService {

    private final UserStorage userStorage;

    public User create(User user) {
        User createdUser = userStorage.create(withDisplayName(user));
        log.info("Created user with id {}", createdUser.getId());
        return createdUser;
    }

    public User update(User user) {
        Long id = user.getId();

        if (id == null || id <= 0) {
            throw new ValidationException("User id must be positive");
        }

        User updatedUser = userStorage.update(withDisplayName(user));
        log.info("Updated user with id {}", id);
        return updatedUser;
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    private User withDisplayName(User user) {
        if (user.getName() != null && !user.getName().isBlank()) {
            return user;
        }

        return new User(
                user.getId(),
                user.getEmail(),
                user.getLogin(),
                user.getLogin(),
                user.getBirthday()
        );
    }

}
