package ru.yandex.practicum.filmorate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.io.IOException;
import java.io.InputStream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
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
class FilmorateApiTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @ParameterizedTest
    @ValueSource(strings = {"/films", "/users"})
    void listsEmptyCollection(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void createsUpdatesAndListsFilmsInCreationOrder() throws Exception {
        ObjectNode firstFilm = readFixture("film")
                .put("id", 99)
                .put("name", "First");
        ObjectNode secondFilm = firstFilm.deepCopy().put("name", "Second");
        ObjectNode updatedFilm = readFixture("film")
                .put("id", 1)
                .put("name", "Updated")
                .put("rate", 4);

        mockMvc.perform(withJson(post("/films"), firstFilm))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.releaseDate").value("2000-01-01"));
        mockMvc.perform(withJson(post("/films"), secondFilm))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));


        mockMvc.perform(withJson(put("/films"), updatedFilm))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Updated"));


        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Updated"))
                .andExpect(jsonPath("$[1].name").value("Second"))
                .andExpect(jsonPath("$[2]").doesNotExist());
    }

    @Test
    void createsUpdatesAndListsUsers() throws Exception {
        ObjectNode user = readFixture("user").put("id", 99);
        ObjectNode updatedUser = readFixture("user")
                .put("id", 1)
                .put("login", "updated");

        mockMvc.perform(withJson(post("/users"), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.birthday").value("2000-01-01"));


        mockMvc.perform(withJson(put("/users"), updatedUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.login").value("updated"));


        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].login").value("updated"))
                .andExpect(jsonPath("$[1]").doesNotExist());
    }

    @Test
    void rejectsUpdatesToUnknownIds() throws Exception {
        ObjectNode film = readFixture("film").put("id", 1);
        ObjectNode user = readFixture("user").put("id", 1);


        mockMvc.perform(withJson(put("/films"), film))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(withJson(put("/users"), user))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void rejectsMissingRequestBody() throws Exception {
        assertBodyIsRejected(new byte[0]);
    }

    @Test
    void rejectsMalformedRequestBody() throws Exception {
        ClassPathResource fixture = new ClassPathResource("fixtures/malformed-request.json");


        assertBodyIsRejected(fixture.getContentAsByteArray());
    }

    @Test
    void rejectsUpdatesWithoutPositiveId() throws Exception {
        ObjectNode film = readFixture("film").put("id", 0);
        ObjectNode user = readFixture("user");


        mockMvc.perform(withJson(put("/films"), film))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(withJson(put("/users"), user))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void preservesFilmAfterRejectedUpdate() throws Exception {
        ObjectNode film = readFixture("film").put("name", "Original");
        ObjectNode invalidFilm = film.deepCopy()
                .put("id", 1)
                .put("name", " ");

        mockMvc.perform(withJson(post("/films"), film))
                .andExpect(status().isCreated());


        mockMvc.perform(withJson(put("/films"), invalidFilm))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());


        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Original"));
    }

    @Test
    void usesLoginWhenDisplayNameIsMissingOrBlank() throws Exception {
        ObjectNode user = readFixture("user");
        user.remove("name");
        ObjectNode updatedUser = user.deepCopy()
                .put("id", 1)
                .put("login", "updated")
                .put("name", "  ");

        mockMvc.perform(withJson(post("/users"), user))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("user"));


        mockMvc.perform(withJson(put("/users"), updatedUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated"));
    }

    private void assertBodyIsRejected(byte[] body) throws Exception {
        mockMvc.perform(post("/films").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
        mockMvc.perform(put("/users").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    private ObjectNode readFixture(String name) throws IOException {
        ClassPathResource fixture = new ClassPathResource("fixtures/%s.json".formatted(name));

        try (InputStream input = fixture.getInputStream()) {
            return objectMapper.readValue(input, ObjectNode.class);
        }
    }

    private MockHttpServletRequestBuilder withJson(MockHttpServletRequestBuilder request, ObjectNode body)
            throws JsonProcessingException {
        return request.contentType(APPLICATION_JSON).content(objectMapper.writeValueAsBytes(body));
    }
}
