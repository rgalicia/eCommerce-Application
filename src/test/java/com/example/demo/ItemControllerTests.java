package com.example.demo;

import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.security.SecurityConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static com.example.demo.TestConstants.TEST_USERNAME;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)      // Jupiter
@SpringBootTest                         // to wire ALL beans and components
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)    // integration test
@ContextConfiguration
public class ItemControllerTests {

    // user and jwt data
    private static String bearerValue = null;

    @Autowired MockMvc mvc;
    @Autowired JacksonTester<CreateUserRequest> jsonCreateUserRequest;

    @Test
    @Order(1)
    public void createUserAndLoginTest() throws Exception {
        final CreateUserRequest createUserRequest = UserControllerTests.getCreateUserRequest();
        mvc.perform(
                post(new URI(SecurityConstants.SIGN_UP_URL))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.id").value(1L))
                // java.lang.AssertionError: No value at JSON path "$.password"
                .andExpect(jsonPath("$.password").doesNotExist());

        MockHttpServletResponse response = mvc.perform(
                post(new URI("/login"))
                        .content(UserControllerTests.getLoginDataJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.ALL)) // return empty body
                .andExpect(status().isOk())
                .andExpect(header().exists(SecurityConstants.HEADER_STRING))
                .andReturn().getResponse();

        // save JWT
        bearerValue = response.getHeader(SecurityConstants.HEADER_STRING);
        Assertions.assertNotNull(bearerValue);
        Assertions.assertFalse(bearerValue.isEmpty());

    }

    /** list items correctly */
    @Test
    @Tag("item")
    @Order(100)
    public void getItemsTest() throws Exception {
        mvc.perform(
                get(new URI("/api/item"))
                        .header(SecurityConstants.HEADER_STRING, ItemControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @Tag("item")
    @Order(101)
    public void getItemById() throws Exception {
        mvc.perform(
                get(new URI("/api/item/1"))
                        .header(SecurityConstants.HEADER_STRING, ItemControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));
    }

    @Test
    @Tag("item")
    @Order(102)
    public void getItemsByName() throws Exception {
        // https://attacomsian.com/blog/encode-decode-url-string-java
        String encodedItemName = "Round%20Widget";
        mvc.perform(
                get(new URI("/api/item/name/" + encodedItemName))
                        .header(SecurityConstants.HEADER_STRING, ItemControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1L));
    }

    @Test
    @Tag("item")
    @Order(103)
    public void getBadItemsByName() throws Exception {
        // https://attacomsian.com/blog/encode-decode-url-string-java
        final String encodedItemName = "Name_not_available_at_database";
        mvc.perform(
                get(new URI("/api/item/name/" + encodedItemName))
                        .header(SecurityConstants.HEADER_STRING, ItemControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("item")
    @Order(104)
    public void getBadItemById() throws Exception {
        mvc.perform(
                get(new URI("/api/item/0"))
                        .header(SecurityConstants.HEADER_STRING, ItemControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }
}