package com.example.demo;

import com.example.demo.controllers.UserController;
import com.example.demo.model.persistence.User;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import com.example.demo.security.SecurityConstants;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)      // Jupiter
@SpringBootTest                         // to wire ALL beans and components
@AutoConfigureMockMvc
@AutoConfigureJsonTesters
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)    // integration test
public class UserControllerTests {

    private static final String TEST_VALID_PASSWORD_1 = "abcdegh";
    private static final String TEST_VALID_PASSWORD_2 = "1234567";
    private static final String TEST_INVALID_PASSWORD = "12345";

    // user and jwt data
    private static String bearerValue = null;
    private static Long userId = null;

    @Autowired UserController userController;
    @Autowired MockMvc mvc;
    @Autowired JacksonTester<CreateUserRequest> jsonCreateUserRequest;
    @Autowired JacksonTester<ModifyCartRequest> jsonModifyCartRequest;
    @Autowired JacksonTester<User> jsonUser;

    private static String quote(final String str) {
        return "\"" + str + "\"";
    }

    static String getLoginDataJson() {
        StringBuilder sb = new StringBuilder("{");
        sb.append(quote("username") + ":" + quote(TestConstants.TEST_USERNAME) + ",");
        sb.append(quote("password") + ":" + quote(TEST_VALID_PASSWORD_1));
        sb.append("}");
        return sb.toString();
    }

    static CreateUserRequest getCreateUserRequest() {
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(TestConstants.TEST_USERNAME);
        createUserRequest.setPassword(TEST_VALID_PASSWORD_1);
        createUserRequest.setConfirmPassword(TEST_VALID_PASSWORD_1);
        return createUserRequest;
    }

    /**
     * @see <a href="https://www.baeldung.com/parameterized-tests-junit-5">parameterized-tests-junit-5</a>
     * <p>
     * Before login and JWT generation, code 403 response is expected.
     * </p>
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
     */
    @Test
	@Tag("user")
    @Order(1)
    public void createUserInvalidPasswordTest() throws Exception {

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(TestConstants.TEST_USERNAME);

        // invalid password
        createUserRequest.setPassword(TEST_INVALID_PASSWORD);
        createUserRequest.setConfirmPassword(TEST_INVALID_PASSWORD);
        mvc.perform(
                post(new URI(SecurityConstants.SIGN_UP_URL))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());

        // valid passwords, but different
        createUserRequest.setPassword(TEST_VALID_PASSWORD_1);
        createUserRequest.setConfirmPassword(TEST_VALID_PASSWORD_2);
        mvc.perform(
                post(new URI(SecurityConstants.SIGN_UP_URL))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isBadRequest());
    }

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
                post(new URI(SecurityConstants.SIGN_UP_URL))
                        .content(jsonCreateUserRequest.write(createUserRequest).getJson())
                        .contentType(MediaType.APPLICATION_JSON_UTF8)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TestConstants.TEST_USERNAME))
                .andExpect(jsonPath("$.id").value(1L))
                // java.lang.AssertionError: No value at JSON path "$.password"
                .andExpect(jsonPath("$.password").doesNotExist());

        UserControllerTests.userId = 1L;
    }

    @Test
    @Tag("user")
    @Order(4)
    public void createExistingUserTest() throws Exception {
        final CreateUserRequest createUserRequest = getCreateUserRequest();
        mvc.perform(
                post(new URI(SecurityConstants.SIGN_UP_URL))
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
                .andExpect(header().exists(SecurityConstants.HEADER_STRING))
                .andReturn().getResponse();

        // save JWT
        UserControllerTests.bearerValue = response.getHeader(SecurityConstants.HEADER_STRING);
        Assertions.assertNotNull(bearerValue);
        Assertions.assertFalse(UserControllerTests.bearerValue.isEmpty());
    }

    @Test
	@Tag("user")
    @Order(5)
    public void loginPerformedByUsernameTest() throws Exception {
        Assertions.assertFalse(UserControllerTests.bearerValue.isEmpty());
        mvc.perform(
                get(new URI("/api/user/" + TestConstants.TEST_USERNAME))
                        .header(SecurityConstants.HEADER_STRING, UserControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TestConstants.TEST_USERNAME));
    }

    @Test
	@Tag("user")
    @Order(6)
    public void loginPerformedByUserIdTest() throws Exception {
        Assertions.assertFalse(UserControllerTests.bearerValue.isEmpty());
        Assertions.assertNotNull(UserControllerTests.userId);
        mvc.perform(
                get(new URI("/api/user/id/" + UserControllerTests.userId))
                        .header(SecurityConstants.HEADER_STRING, UserControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(TestConstants.TEST_USERNAME));
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
                        .header(SecurityConstants.HEADER_STRING, UserControllerTests.bearerValue)
                        .accept(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));

        // now it's logged out
    }

}