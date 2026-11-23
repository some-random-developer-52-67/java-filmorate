package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;

public interface UserStorage {

    User create(User user);

    User update(User user);

    void deleteById(long id);

    User getById(long id);

    List<User> findAll();

    void addFriendship(long userId, long friendId);

    void removeFriendship(long userId, long friendId);

    List<User> findFriends(long userId);

    List<User> findCommonFriends(long userId, long otherId);

    void addFilmLike(long userId, long filmId);

    void removeFilmLike(long userId, long filmId);

}
