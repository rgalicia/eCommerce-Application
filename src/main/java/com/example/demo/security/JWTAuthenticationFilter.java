package com.example.demo.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.auth0.jwt.JWT;
import com.example.demo.model.persistence.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.auth0.jwt.algorithms.Algorithm.HMAC512;

public class JWTAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private static Logger log = LoggerFactory.getLogger(JWTAuthenticationFilter.class);
    private AuthenticationManager authenticationManager;

    public JWTAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }
    
    @Override
    public Authentication attemptAuthentication(HttpServletRequest req,
                                                HttpServletResponse res) throws AuthenticationException {
        String username = null;
    	try {
            User credentials = new ObjectMapper()
                    .readValue(req.getInputStream(), User.class);

            username = credentials.getUsername();
            return authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            credentials.getUsername(),
                            credentials.getPassword(),
                            new ArrayList<>()));
    	} catch (IOException e) {
			log.error("I/O Error attempting authentication: {}", e.getMessage());																	 
    		throw new RuntimeException(e);
    	} catch (BadCredentialsException bce) {
            log.error("Bad credentials for {}", username);
            throw bce;
        }
    }
    
    @Override
    protected void successfulAuthentication(HttpServletRequest req,
                                            HttpServletResponse res,
                                            FilterChain chain,
                                            Authentication auth) throws IOException, ServletException {

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
}
