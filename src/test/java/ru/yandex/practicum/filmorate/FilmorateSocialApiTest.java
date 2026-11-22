package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ApiExceptionHandler;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class FilmorateSocialApiTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final UserStorage userStorage;

    @Test
    @SneakyThrows
    void findsFilmsAndUsersById() {
        createFilm();
        createUser();


        mockMvc.perform(get("/films/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Film"))
                .andExpect(jsonPath("$.likedUserIds").doesNotExist());
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("user"))
                .andExpect(jsonPath("$.friendIds").doesNotExist());
        assertNotFound(get("/films/99"));
        assertNotFound(get("/users/99"));
    }

    @Test
    @SneakyThrows
    void addsAndRemovesFriendshipInBothDirections() {
        createUser();
        createUser();


        mockMvc.perform(put("/users/1/friends/2")).andExpect(status().isNoContent());


        mockMvc.perform(get("/users/1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(2)));
        mockMvc.perform(get("/users/2/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(1)));

        mockMvc.perform(put("/users/2/friends/1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/users/1/friends"))
                .andExpect(jsonPath("$[*].id").value(contains(2)));

        mockMvc.perform(delete("/users/2/friends/1")).andExpect(status().isNoContent());
        mockMvc.perform(get("/users/1/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/users/2/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(delete("/users/1/friends/2")).andExpect(status().isNoContent());
    }

    @Test
    @SneakyThrows
    void findsOnlyCommonFriendsAndKeepsFriendshipsAfterUserUpdate() {
        createUser();
        createUser();
        createUser();
        createUser();
        mockMvc.perform(put("/users/1/friends/3")).andExpect(status().isNoContent());
        mockMvc.perform(put("/users/1/friends/4")).andExpect(status().isNoContent());
        mockMvc.perform(put("/users/2/friends/3")).andExpect(status().isNoContent());

        ObjectNode user = readFixture("user").put("id", 3).put("name", "Updated friend");
        user.putArray("friendIds").add(4);


        mockMvc.perform(withJson(put("/users"), user)).andExpect(status().isOk());


        mockMvc.perform(get("/users/1/friends/common/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(3)))
                .andExpect(jsonPath("$[0].name").value("Updated friend"));
        mockMvc.perform(get("/users/3/friends"))
                .andExpect(jsonPath("$[*].id").value(contains(1, 2)));
    }

    @Test
    @SneakyThrows
    void ranksFilmsByUniqueLikesAndKeepsLikesAfterFilmUpdate() {
        createUser();
        createUser();
        createFilm();
        createFilm();
        createFilm();
        mockMvc.perform(put("/films/2/like/1")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/2/like/1")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/3/like/1")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/3/like/2")).andExpect(status().isNoContent());

        ObjectNode film = readFixture("film").put("id", 3).put("name", "Updated film");
        film.putArray("likedUserIds");
        mockMvc.perform(withJson(put("/films"), film)).andExpect(status().isOk());


        mockMvc.perform(get("/films/popular").param("count", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(3, 2)))
                .andExpect(jsonPath("$[0].name").value("Updated film"));

        mockMvc.perform(delete("/films/3/like/2")).andExpect(status().isNoContent());
        mockMvc.perform(delete("/films/3/like/2")).andExpect(status().isNoContent());
        mockMvc.perform(get("/films/popular").param("count", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(2, 3, 1)));
    }

    @Test
    @SneakyThrows
    void returnsTenFilmsByDefaultAndBreaksTiesById() {
        for (int number = 0; number < 11; number++) {
            createFilm();
        }


        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)));
    }

    @Test
    @SneakyThrows
    void returnsAnEmptyListForZeroCount() {
        createFilm();


        mockMvc.perform(get("/films/popular").param("count", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @SneakyThrows
    void removesTheDeletedUsersLikesFromTheRanking() {
        createUser();
        createUser();
        createFilm();
        createFilm();
        createFilm();
        mockMvc.perform(put("/users/1/friends/2")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/1/like/1")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/2/like/2")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/3/like/1")).andExpect(status().isNoContent());
        mockMvc.perform(put("/films/3/like/2")).andExpect(status().isNoContent());

        mockMvc.perform(get("/films/popular"))
                .andExpect(jsonPath("$[*].id").value(contains(3, 1, 2)));


        userStorage.deleteById(1);


        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains(2, 3, 1)));
        mockMvc.perform(get("/users/2/friends"))
                .andExpect(jsonPath("$").isEmpty());
        assertNotFound(put("/films/3/like/1"));
    }

    @Test
    @SneakyThrows
    void rejectsMissingParticipantsWithoutChangingExistingEntities() {
        createUser();
        createFilm();
        createFilm();


        assertNotFound(put("/users/1/friends/99"));
        assertNotFound(delete("/users/99/friends/1"));
        assertNotFound(get("/users/99/friends"));
        assertNotFound(get("/users/1/friends/common/99"));
        assertNotFound(put("/films/2/like/99"));
        assertNotFound(delete("/films/99/like/1"));


        mockMvc.perform(get("/users/1/friends")).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/films/popular")).andExpect(jsonPath("$[*].id").value(contains(1, 2)));
    }

    @Test
    @SneakyThrows
    void rejectsInvalidParametersAndRetainsMvcMethodErrors() {
        createUser();


        assertBadRequest(get("/films/not-an-id"));
        assertBadRequest(get("/films/popular").param("count", "none"));
        assertBadRequest(get("/films/popular").param("count", "-1"));
        assertBadRequest(put("/users/1/friends/1"));
        mockMvc.perform(delete("/films")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @SneakyThrows
    void returnsGenericServerErrorForAnUnexpectedException() {
        FilmService filmService = mock(FilmService.class);
        when(filmService.findAll()).thenThrow(new IllegalStateException("Internal details"));
        MockMvc failingMvc = MockMvcBuilders.standaloneSetup(new FilmController(filmService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();


        failingMvc.perform(get("/films"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Internal server error"));
    }

    @SneakyThrows
    private void createFilm() {
        mockMvc.perform(withJson(post("/films"), readFixture("film"))).andExpect(status().isCreated());
    }

    @SneakyThrows
    private void createUser() {
        mockMvc.perform(withJson(post("/users"), readFixture("user"))).andExpect(status().isCreated());
    }

    @SneakyThrows
    private void assertNotFound(MockHttpServletRequestBuilder request) {
        mockMvc.perform(request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @SneakyThrows
    private void assertBadRequest(MockHttpServletRequestBuilder request) {
        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @SneakyThrows
    private ObjectNode readFixture(String name) {
        ClassPathResource fixture = new ClassPathResource("fixtures/%s.json".formatted(name));
        return objectMapper.readValue(fixture.getContentAsByteArray(), ObjectNode.class);
    }

    @SneakyThrows
    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder request, ObjectNode body) {
        return request.contentType(APPLICATION_JSON).content(objectMapper.writeValueAsBytes(body));
    }

}
