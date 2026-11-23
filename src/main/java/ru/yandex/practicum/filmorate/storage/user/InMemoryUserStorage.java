package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Component
public class InMemoryUserStorage implements UserStorage {

    private final FilmStorage filmStorage;

    private final Map<Long, User> users = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized User create(User user) {
        return save(user, nextId++, Set.of());
    }

    @Override
    public synchronized User update(User user) {
        User currentUser = getStoredUser(user.getId());
        return save(user, currentUser.getId(), currentUser.getFriendIds());
    }

    @Override
    public synchronized void deleteById(long id) {
        User user = getStoredUser(id);
        filmStorage.removeLikesByUserId(id);

        for (long friendId : List.copyOf(user.getFriendIds())) {
            getStoredUser(friendId).getFriendIds().remove(id);
        }
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

    @Override
    public synchronized void addFriendship(long userId, long friendId) {
        User user = getStoredUser(userId);
        User friend = getStoredUser(friendId);

        user.getFriendIds().add(friendId);
        friend.getFriendIds().add(userId);
    }

    @Override
    public synchronized void removeFriendship(long userId, long friendId) {
        User user = getStoredUser(userId);
        User friend = getStoredUser(friendId);

        user.getFriendIds().remove(friendId);
        friend.getFriendIds().remove(userId);
    }

    @Override
    public synchronized List<User> findFriends(long userId) {
        return getStoredUser(userId).getFriendIds().stream()
                .map(this::getStoredUser)
                .map(this::copy)
                .toList();
    }

    @Override
    public synchronized List<User> findCommonFriends(long userId, long otherId) {
        Set<Long> friendIds = getStoredUser(userId).getFriendIds();
        Set<Long> otherFriendIds = getStoredUser(otherId).getFriendIds();

        return friendIds.stream()
                .filter(otherFriendIds::contains)
                .map(this::getStoredUser)
                .map(this::copy)
                .toList();
    }

    @Override
    public synchronized void addFilmLike(long userId, long filmId) {
        getStoredUser(userId);
        filmStorage.addLike(filmId, userId);
    }

    @Override
    public synchronized void removeFilmLike(long userId, long filmId) {
        getStoredUser(userId);
        filmStorage.removeLike(filmId, userId);
    }

    private User getStoredUser(long id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("User with id %d was not found".formatted(id));
        }

        return user;
    }

    private User save(User user, long id, Set<Long> friendIds) {
        User savedUser = copyWithIdAndFriends(user, id, friendIds);
        users.put(id, savedUser);
        return copy(savedUser);
    }

    private User copy(User user) {
        return copyWithIdAndFriends(user, user.getId(), user.getFriendIds());
    }

    private User copyWithIdAndFriends(User user, long id, Set<Long> friendIds) {
        User copy = new User(
                id,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday()
        );
        copy.getFriendIds().addAll(friendIds);
        return copy;
    }

}
