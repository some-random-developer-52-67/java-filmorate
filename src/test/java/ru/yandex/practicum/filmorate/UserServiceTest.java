package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class UserServiceTest {

    @Test
    void excludesAFriendMovedBetweenUsersAfterTheFirstStorageRead() {
        UserStorage storage = new InMemoryUserStorage(new InMemoryFilmStorage());
        long userId = storage.create(new User()).getId();
        long otherId = storage.create(new User()).getId();
        long friendId = storage.create(new User()).getId();
        storage.addFriendship(userId, friendId);

        UserStorage reads = mock(UserStorage.class, delegatesTo(storage));
        UserService userService = new UserService(reads);


        List<User> commonFriends;
        try (ExecutorService writer = Executors.newSingleThreadExecutor()) {
            Runnable moveFriendship = () -> {
                storage.removeFriendship(userId, friendId);
                storage.addFriendship(otherId, friendId);
            };

            doAnswer(invocation -> {
                User snapshot = storage.getById(userId);
                writer.submit(moveFriendship).get(5, SECONDS);
                return snapshot;
            }).when(reads).getById(userId);
            doAnswer(invocation -> {
                List<User> snapshot = storage.findCommonFriends(userId, otherId);
                writer.submit(moveFriendship).get(5, SECONDS);
                return snapshot;
            }).when(reads).findCommonFriends(userId, otherId);
            commonFriends = userService.findCommonFriends(userId, otherId);
        }


        assertThat(commonFriends).isEmpty();
        assertThat(storage.findFriends(userId)).isEmpty();
        assertThat(storage.findFriends(otherId)).extracting(User::getId).containsExactly(friendId);
    }

}
