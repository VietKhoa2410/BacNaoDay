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

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class RelationPageOwnershipIntegrationTest {

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

    private static long readPageId(MvcResult create) {
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
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
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
    void ownerCanCreateAndReadPage_otherUserGets404OnSameId() throws Exception {
        String alice = loginAs("alice", "alicepw");
        MvcResult create = mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Alice tree\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        long pageId = readPageId(create);

        mockMvc.perform(get("/api/relation-pages/" + pageId).headers(bearer(alice))).andExpect(status().isOk());

        String bob = loginAs("bob", "bobpw");
        mockMvc.perform(get("/api/relation-pages/" + pageId).headers(bearer(bob))).andExpect(status().isNotFound());
    }

    @Test
    void ownerCanAddPerson_otherUserCannotPostPersonToForeignPage() throws Exception {
        String alice = loginAs("alice", "alicepw");
        MvcResult create = mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Tree\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long pageId = readPageId(create);

        mockMvc.perform(post("/api/relation-pages/" + pageId + "/persons")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Mom\",\"sortOrder\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("Mom"));

        String bob = loginAs("bob", "bobpw");
        mockMvc.perform(post("/api/relation-pages/" + pageId + "/persons")
                        .headers(bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Intruder\",\"sortOrder\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listPages_onlyReturnsOwnPages() throws Exception {
        String alice = loginAs("alice", "alicepw");
        mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"A1\"}"))
                .andExpect(status().isCreated());

        String bob = loginAs("bob", "bobpw");
        mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(bob))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"B1\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/relation-pages").headers(bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("A1"));

        mockMvc.perform(get("/api/relation-pages").headers(bearer(bob)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("B1"));
    }

    @Test
    void listPersons_onOthersPage_returns404() throws Exception {
        String alice = loginAs("alice", "alicepw");
        MvcResult create = mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Shared name\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long pageId = readPageId(create);

        String bob = loginAs("bob", "bobpw");
        mockMvc.perform(get("/api/relation-pages/" + pageId + "/persons").headers(bearer(bob)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPersonGraph_onOthersPage_returns404() throws Exception {
        String alice = loginAs("alice", "alicepw");
        MvcResult create = mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Private tree\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long pageId = readPageId(create);

        String bob = loginAs("bob", "bobpw");
        mockMvc.perform(get("/api/relation-pages/" + pageId + "/persons/graph").headers(bearer(bob)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerGetsPersonGraph_withNodesAndEdges() throws Exception {
        String alice = loginAs("alice", "alicepw");
        MvcResult create = mockMvc.perform(post("/api/relation-pages")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Family\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long pageId = readPageId(create);

        MvcResult childRes = mockMvc.perform(post("/api/relation-pages/" + pageId + "/persons")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"Child\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        long childId = readPageId(childRes);

        mockMvc.perform(post("/api/relation-pages/" + pageId + "/persons")
                        .headers(bearer(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"displayName\":\"Father\",\"toPersonId\":"
                                        + childId
                                        + ",\"relationType\":\"FATHER_OF\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/relation-pages/" + pageId + "/persons/graph").headers(bearer(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(2)))
                .andExpect(jsonPath("$.edges.length()").value(greaterThanOrEqualTo(1)));
    }
}
