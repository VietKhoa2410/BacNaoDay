package demo.bacnaoday.api;

import demo.bacnaoday.repository.PersonRelationRepository;
import demo.bacnaoday.repository.PersonRepository;
import demo.bacnaoday.repository.RelationPageRepository;
import demo.bacnaoday.repository.UserRepository;
import demo.bacnaoday.service.UserCredentialService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RelationLookupIntegrationTest {

    @Autowired
    WebApplicationContext webApplicationContext;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RelationPageRepository relationPageRepository;

    @Autowired
    PersonRepository personRepository;

    @Autowired
    PersonRelationRepository personRelationRepository;

    @Autowired
    UserCredentialService userCredentialService;

    MockMvc mockMvc;

    private static long readId(MvcResult create) {
        String json = new String(create.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
        int key = json.indexOf("\"id\":");
        if (key < 0) {
            throw new IllegalStateException("no id in " + json);
        }
        int i = key + 5;
        while (i < json.length() && (json.charAt(i) == ' ' || json.charAt(i) == ':')) {
            i++;
        }
        int end = i;
        while (end < json.length() && Character.isDigit(json.charAt(end))) {
            end++;
        }
        return Long.parseLong(json.substring(i, end));
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).apply(springSecurity()).build();
        personRelationRepository.deleteAll();
        personRepository.deleteAll();
        relationPageRepository.deleteAll();
        userRepository.deleteAll();
        userCredentialService.createUserWithEncodedPassword("alice", "alicepw");
        userCredentialService.createUserWithEncodedPassword("bob", "bobpw");
    }

    private String loginAs(String username, String password) throws Exception {
        MvcResult result =
                mockMvc.perform(
                                post("/api/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"username\":\""
                                                        + username
                                                        + "\",\"password\":\""
                                                        + password
                                                        + "\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        return LoginApiIntegrationTest.readAccessToken(result);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.add(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return h;
    }

    @Test
    void relativeLevel_requiresMarkedPerson() throws Exception {
        String alice = loginAs("alice", "alicepw");
        long pageId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"name\":\"Tree\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        long targetId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages/" + pageId + "/persons")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"displayName\":\"Target\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        mockMvc.perform(get("/api/persons/" + targetId + "/relative-level").headers(bearer(alice)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void relativeLevel_computesParentAndChildLevels() throws Exception {
        String alice = loginAs("alice", "alicepw");
        long pageId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"name\":\"Family\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        long childId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages/" + pageId + "/persons")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"displayName\":\"Child\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        long fatherId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages/" + pageId + "/persons")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"displayName\":\"Father\",\"toPersonId\":"
                                                                + childId
                                                                + ",\"relationType\":\"FATHER_OF\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        long grandChildId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages/" + pageId + "/persons")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(
                                                        "{\"displayName\":\"GrandChild\",\"toPersonId\":"
                                                                + childId
                                                                + ",\"relationType\":\"SON_OF\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        mockMvc.perform(
                        put("/api/relation-page/" + pageId + "/marked-person")
                                .headers(bearer(alice))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"personId\":" + childId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedPersonId").value((int) childId));

        mockMvc.perform(get("/api/persons/" + fatherId + "/relative-level").headers(bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(-1));

        mockMvc.perform(get("/api/persons/" + grandChildId + "/relative-level").headers(bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1));
    }

    @Test
    void relativeLevel_otherUserPersonId_returns404() throws Exception {
        String alice = loginAs("alice", "alicepw");
        long pageId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"name\":\"Alice tree\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        long alicePersonId =
                readId(
                        mockMvc.perform(
                                        post("/api/relation-pages/" + pageId + "/persons")
                                                .headers(bearer(alice))
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content("{\"displayName\":\"Alice\"}"))
                                .andExpect(status().isCreated())
                                .andReturn());

        String bob = loginAs("bob", "bobpw");
        mockMvc.perform(get("/api/persons/" + alicePersonId + "/relative-level").headers(bearer(bob)))
                .andExpect(status().isNotFound());
    }
}

