package com.idea;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class HelloControllerTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    MockMvc mvc;

    @Test
    void hello_returnsOkAndUserCount() throws Exception {
        mvc.perform(get("/hello"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("idea-backend is running"))
            .andExpect(jsonPath("$.userCount").isNumber());
    }

    @Test
    void me_withoutToken_returns401() throws Exception {
        mvc.perform(get("/hello/me"))
            .andExpect(status().isUnauthorized());
    }
}
