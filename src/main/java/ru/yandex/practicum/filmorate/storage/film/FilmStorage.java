package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {

    Film create(Film film);

    Film update(Film film);

    void deleteById(long id);

    Film getById(long id);

    List<Film> findAll();

    void addLike(long filmId, long userId);

    void removeLike(long filmId, long userId);

    void removeLikesByUserId(long userId);

}
