package com.example.demo;

import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.User;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)        // Jupiter
//@RunWith(SpringRunner.class) 			// Vintage JUnit
//@SelectClasses({UserController.class}) // suite
@SpringBootTest(classes = ECommerceApplication.class)    // to wire ALL beans and components
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)    // integration test
//@DataJpaTest		// only prepare persistence beans
public class ECommerceApplicationTests {

    private static final String HEADER_STRING = "Authorization";
    private static final String TEST_USERNAME = "testUsername01";
    private static final String TEST_NON_EXISTING_USERNAME = "nonExistingUsername";
    private static final String TEST_VALID_PASSWORD_1 = "abcdegh";
    private static final String TEST_VALID_PASSWORD_2 = "1234567";
    private static final String TEST_INVALID_PASSWORD = "12345";
    // user and jwt data
    private static String bearerValue = null;
    private static Long userId = null;
    @Autowired
    UserController userController;
    @Autowired
    MockMvc mvc;
    @Autowired
    private JacksonTester<CreateUserRequest> jsonCreateUserRequest;
    @Autowired
    private JacksonTester<ModifyCartRequest> jsonModifyCartRequest;
    @Autowired
    private JacksonTester<User> jsonUser;

    private String quote(final String str) {
        return "\"" + str + "\"";
    }

    private String getLoginDataJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append(quote("username") + ":" + quote(TEST_USERNAME) + ",");
        sb.append(quote("password") + ":" + quote(TEST_VALID_PASSWORD_1));
        sb.append("}");
        return sb.toString();
    }

    private CreateUserRequest getCreateUserRequest() {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(TEST_USERNAME);
        createUserRequest.setPassword(TEST_VALID_PASSWORD_1);
        createUserRequest.setConfirmPassword(TEST_VALID_PASSWORD_1);
        return createUserRequest;
    }

    /**
     * @see "https://www.baeldung.com/parameterized-tests-junit-5"
     * <p>
     * Before login and JWT generation, code 403 response is expected.
     */
    @ParameterizedTest
    @Order(0)
    @ValueSource(strings = {
            "/api/user/username",            // Logged in, user profile
            "/api/cart/details/username",    // Cart details
            "/api/order/history/username",    // Purchase History
    })
    public void authorizedOnlyTest(String uriText) throws Exception {
        mvc.perform(
                get(new URI(uriText))
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isForbidden());
    }

    /**
     * Fail creating users with wrong passwords.
     *
     * <ol>
     * <li>bad password</li>
     * <li>password and confirmation don't match</li>
     * </ol>
     *
     * @throws Exception
     */
    @Test
	@Tag("user")
    @Order(1)
    public void createUserInvalidPasswordTest() throws Exception {

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(TEST_USERNAME);

        // invalid password
        createUserRequest.setPassword(TEST_INVALID_PASSWORD);
        createUserRequest.setConfirmPassword(TEST_INVALID_PASSWORD);
        mvc.perform(
                post(new URI("/api/user/create"))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());

        // valid passwords, but different
        createUserRequest.setPassword(TEST_VALID_PASSWORD_1);
        createUserRequest.setConfirmPassword(TEST_VALID_PASSWORD_2);
        mvc.perform(
                post(new URI("/api/user/create"))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

    // TODO creates a loop
    @Test
	@Tag("user")
    @Order(2)
    public void loginNonExistingUserTest() throws Exception {
        mvc.perform(
                post(new URI("/login"))
                        .content(getLoginDataJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isUnauthorized());
    }

    @Test
	@Tag("user")
    @Order(3)
    public void createUserTest() throws Exception {
        final CreateUserRequest createUserRequest = getCreateUserRequest();
        mvc.perform(
                post(new URI("/api/user/create"))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.id").value(1L))
                // java.lang.AssertionError: No value at JSON path "$.password"
                .andExpect(jsonPath("$.password").doesNotExist());

        ECommerceApplicationTests.userId = 1L;
    }

    @Test
    @Tag("user")
    @Order(4)
    public void createExistingUserTest() throws Exception {
        final CreateUserRequest createUserRequest = getCreateUserRequest();
        mvc.perform(
                post(new URI("/api/user/create"))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }



    @Test
	@Tag("user")
    @Order(4)
    public void loginTest() throws Exception {
        MockHttpServletResponse response = mvc.perform(
                post(new URI("/login"))
                        .content(getLoginDataJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.ALL)) // return empty body
                .andExpect(status().isOk())
                .andExpect(header().exists(ECommerceApplicationTests.HEADER_STRING))
                .andReturn().getResponse();

        // save JWT
        ECommerceApplicationTests.bearerValue = response.getHeader(ECommerceApplicationTests.HEADER_STRING);
        Assertions.assertFalse(ECommerceApplicationTests.bearerValue.isEmpty());
    }

    @Test
	@Tag("user")
    @Order(5)
    public void loginPerformedByUsernameTest() throws Exception {
        Assertions.assertFalse(ECommerceApplicationTests.bearerValue.isEmpty());
        mvc.perform(
                get(new URI("/api/user/" + TEST_USERNAME))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }

    @Test
	@Tag("user")
    @Order(6)
    public void loginPerformedByUserIdTest() throws Exception {
        Assertions.assertFalse(ECommerceApplicationTests.bearerValue.isEmpty());
        Assertions.assertNotNull(ECommerceApplicationTests.userId);
        mvc.perform(
                get(new URI("/api/user/id/" + ECommerceApplicationTests.userId))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TEST_USERNAME));
    }

    /**
     * <p>has no sense using Stateless session</p>
     * <p>JWT token will be outdated following its expiration time (15 min, 10 days...)</p>
     */
    @Test
	@Tag("user")
    @Order(7) //@Disabled
    public void logoutTest() throws Exception {
        // logout
        mvc.perform(
                get(new URI("/logout")) // /logout Redirected URL = /login?logout
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // now it's logged out
    }

    // list items
    @Test
    @Tag("item")
    @Order(100)
    public void getItemsTest() throws Exception {
        mvc.perform(
                get(new URI("/api/item"))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
        String encodedItemName = "Name_not_available_at_database";
        mvc.perform(
                get(new URI("/api/item/name/" + encodedItemName))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("item")
    @Order(101)
    public void getBagItemById() throws Exception {
        mvc.perform(
                get(new URI("/api/item/0"))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }

    @Test
    @Tag("cart")
    @Order(200)
    public void emptyCartTest() throws Exception {
        mvc.perform(
                get(new URI("/api/cart/details/" + ECommerceApplicationTests.TEST_USERNAME))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    @Tag("cart")
    @Order(201)
    public void addToCartTest() throws Exception {
        ModifyCartRequest modifyCartRequest = new ModifyCartRequest();
        modifyCartRequest.setUsername(ECommerceApplicationTests.TEST_USERNAME);
        modifyCartRequest.setItemId(1L);
        modifyCartRequest.setQuantity(2);
        mvc.perform(
                post(new URI("/api/cart/addToCart"))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .content(jsonModifyCartRequest.write(modifyCartRequest).getJson())
                        .accept(MediaType.ALL))
                .andExpect(status().isOk());

        modifyCartRequest.setItemId(2L);
        modifyCartRequest.setQuantity(1);
        mvc.perform(
                post(new URI("/api/cart/addToCart"))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
                get(new URI("/api/cart/details/" + ECommerceApplicationTests.TEST_USERNAME))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
        modifyCartRequest.setUsername(ECommerceApplicationTests.TEST_USERNAME);
        modifyCartRequest.setItemId(1L);
        modifyCartRequest.setQuantity(1);
        mvc.perform(
                post(new URI("/api/cart/removeFromCart"))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
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
                get(new URI("/api/order/history/" + TEST_NON_EXISTING_USERNAME))
                        .header(ECommerceApplicationTests.HEADER_STRING, ECommerceApplicationTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isNotFound());
    }


}