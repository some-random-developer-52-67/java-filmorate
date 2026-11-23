package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Long, Film> films = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized Film create(Film film) {
        return save(film, nextId++, Set.of());
    }

    @Override
    public synchronized Film update(Film film) {
        Film currentFilm = getStoredFilm(film.getId());
        return save(film, currentFilm.getId(), currentFilm.getLikedUserIds());
    }

    @Override
    public synchronized void deleteById(long id) {
        getStoredFilm(id);
        films.remove(id);
    }

    @Override
    public synchronized Film getById(long id) {
        return copy(getStoredFilm(id));
    }

    @Override
    public synchronized List<Film> findAll() {
        return films.values().stream()
                .map(this::copy)
                .toList();
    }

    @Override
    public synchronized void addLike(long filmId, long userId) {
        getStoredFilm(filmId).getLikedUserIds().add(userId);
    }

    @Override
    public synchronized void removeLike(long filmId, long userId) {
        getStoredFilm(filmId).getLikedUserIds().remove(userId);
    }

    @Override
    public synchronized void removeLikesByUserId(long userId) {
        films.values().forEach(film -> film.getLikedUserIds().remove(userId));
    }

    private Film getStoredFilm(long id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Film with id %d was not found".formatted(id));
        }

        return film;
    }

    private Film save(Film film, long id, Set<Long> likedUserIds) {
        Film savedFilm = copyWithIdAndLikes(film, id, likedUserIds);
        films.put(id, savedFilm);
        return copy(savedFilm);
    }

    private Film copy(Film film) {
        return copyWithIdAndLikes(film, film.getId(), film.getLikedUserIds());
    }

    private Film copyWithIdAndLikes(Film film, long id, Set<Long> likedUserIds) {
        Film copy = new Film(
                id,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration()
        );
        copy.getLikedUserIds().addAll(likedUserIds);
        return copy;
    }

}
