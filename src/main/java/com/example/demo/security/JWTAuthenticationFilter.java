package com.example.demo.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.example.demo.model.persistence.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

// JSP Filter
@Slf4j
public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private AuthenticationManager authenticationManager;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
        this.setAuthenticationFailureHandler(new AuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {

            }
        });
        // this.setFilterProcessesUrl("/login");
    }

    /**
     * If authentication is successful, this method is invoked.
     * Adds the JWToken to the header.
     *
     * @throws IOException
     * @throws ServletException
     */
    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

        log.debug("successful Authentication!");
        final org.springframework.security.core.userdetails.User principal =
                (org.springframework.security.core.userdetails.User) auth.getPrincipal();
        // create the token to be sent
        String token = JWT.create()
                .withSubject(principal.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .sign(HMAC512(SecurityConstants.SECRET.getBytes()));
        // sent and continue executing filter chain
        res.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
    }

    /**
     * @throws BadCredentialsException if authentication fails
     * @throws RuntimeException if I/O error occurs
     */
    @Override
    @SneakyThrows({IOException.class})
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        log.debug("attempting Authentication...");
        String username = null;
            User credentials = new ObjectMapper()
                    .readValue(req.getInputStream(), User.class);

        try {
            username = credentials.getUsername();
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            credentials.getUsername(),
                            credentials.getPassword(),
                            new ArrayList<>()));
//        } catch (IOException e) {
//            log.error("I/O Error attempting authentication: {}", e.getMessage());
//            throw new RuntimeException("I/O Error attempting authentication", e);
        } catch (AuthenticationException bce) {
            log.error("Authentication Exception for {}: {}", username, bce.getMessage());
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }
    }
}
