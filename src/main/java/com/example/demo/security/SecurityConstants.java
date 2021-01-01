package com.example.demo.security;

public class SecurityConstants {

    // package view
	static final String SECRET = "very_new_secret_key";
    static final long EXPIRATION_TIME = 864_000_000; // 10 days in seconds
    static final String TOKEN_PREFIX = "Bearer ";
    static final String HEADER_STRING = "Authorization";
    static final String SIGN_UP_URL = "/api/user/create";
}
