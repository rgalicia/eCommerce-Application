package com.example.demo;

import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
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
public class CartAndOrderControllersTests {

    // user and jwt data
    private static String bearerValue = null;

    @Autowired MockMvc mvc;
    @Autowired JacksonTester<CreateUserRequest> jsonCreateUserRequest;
    @Autowired JacksonTester<ModifyCartRequest> jsonModifyCartRequest;


    @Test
    @Order(0)
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


    @Test
    @Tag("cart")
    @Order(200)
    public void emptyCartTest() throws Exception {
        mvc.perform(
                get(new URI("/api/cart/details/" + TestConstants.TEST_USERNAME))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @Tag("cart")
    @Order(201)
    public void addToCartTest() throws Exception {
        ModifyCartRequest modifyCartRequest = new ModifyCartRequest();
        modifyCartRequest.setUsername(TestConstants.TEST_USERNAME);
        modifyCartRequest.setItemId(1L);
        modifyCartRequest.setQuantity(2);
        mvc.perform(
                post(new URI("/api/cart/addToCart"))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonModifyCartRequest.write(modifyCartRequest).getJson())
                        .accept(MediaType.ALL))
                .andExpect(status().isOk());

        modifyCartRequest.setItemId(2L);
        modifyCartRequest.setQuantity(1);
        mvc.perform(
                post(new URI("/api/cart/addToCart"))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonModifyCartRequest.write(modifyCartRequest).getJson())
                        .accept(MediaType.ALL))
                .andExpect(status().isOk());
    }

    @Test
    @Tag("cart")
    @Order(202)
    public void retrieveItemsCartTest() throws Exception {
        mvc.perform(
                get(new URI("/api/cart/details/" + TestConstants.TEST_USERNAME))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.items[0].name").value("Round Widget"));
    }

    @Test
    @Tag("cart")
    @Order(203)
    public void deleteFromCartTest() throws Exception {
        ModifyCartRequest modifyCartRequest = new ModifyCartRequest();
        modifyCartRequest.setUsername(TestConstants.TEST_USERNAME);
        modifyCartRequest.setItemId(1L);
        modifyCartRequest.setQuantity(1);
        mvc.perform(
                post(new URI("/api/cart/removeFromCart"))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .content(jsonModifyCartRequest.write(modifyCartRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.ALL))
                .andExpect(status().isOk());
    }

    @Test
    @Tag("order")
    @Order(301)
    public void orderSubmitTest() throws Exception {
        mvc.perform(
                post(new URI("/api/order/submit/" + TEST_USERNAME))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.ALL))
                .andExpect(status().isOk());
    }

    @Test
    @Tag("order")
    @Order(302)
    public void orderHistoryTest() throws Exception {
        mvc.perform(
                get(new URI("/api/order/history/" + TEST_USERNAME))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1)) // one order
                .andExpect(jsonPath("$.[0].items.length()").value(2)); // two items
    }

    @Test
    @Tag("order")
    @Order(303)
    public void orderHistoryBadUsernameTest() throws Exception {
        mvc.perform(
                get(new URI("/api/order/history/" + TestConstants.TEST_NON_EXISTING_USERNAME))
                        .header(SecurityConstants.HEADER_STRING, bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

}
