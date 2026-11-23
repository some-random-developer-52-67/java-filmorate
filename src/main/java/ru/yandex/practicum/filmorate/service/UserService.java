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

    public User getById(long id) {
        return userStorage.getById(id);
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public void addFriend(long userId, long friendId) {
        validateFriendship(userId, friendId);
        userStorage.addFriendship(userId, friendId);
        log.debug("Friendship is active between users {} and {}", userId, friendId);
    }

    public void removeFriend(long userId, long friendId) {
        validateFriendship(userId, friendId);
        userStorage.removeFriendship(userId, friendId);
        log.debug("Friendship is inactive between users {} and {}", userId, friendId);
    }

    public List<User> findFriends(long userId) {
        return userStorage.findFriends(userId);
    }

    public List<User> findCommonFriends(long userId, long otherId) {
        return userStorage.findCommonFriends(userId, otherId);
    }

    private void validateFriendship(long userId, long friendId) {
        if (userId == friendId) {
            userStorage.getById(userId);
            throw new ValidationException("A user cannot be friends with themselves");
        }
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
