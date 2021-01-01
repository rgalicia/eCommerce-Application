package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {
	
	@Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;
	
//    public WebSecurityConfiguration(UserDetailsServiceImpl userDetailsService,
//			BCryptPasswordEncoder bCryptPasswordEncoder) {
//		this.userDetailsService = userDetailsService;
//		this.bCryptPasswordEncoder = bCryptPasswordEncoder;
//	}
    
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors().and().csrf().disable().authorizeRequests()
                .antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL).permitAll()
                // .antMatchers("/login").permitAll() // return 404
                .anyRequest().authenticated()
                .and()
                .addFilter(new JWTAuthenticationFilter(authenticationManager()))
                .addFilter(new JWTAuthenticationVerificationFilter(authenticationManager()))
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    // overridden just to be wired as @Bean, used by JWTAuthenticationVerificationFilter constructor
    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
    
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        // https://knowledge.udacity.com/questions/293950
        // https://www.freecodecamp.org/news/how-to-setup-jwt-authorization-and-authentication-in-spring/
        // avoid infinite loop when logging with invalid user
        auth // .parentAuthenticationManager(authenticationManagerBean())
                .userDetailsService(userDetailsService)
                .passwordEncoder(bCryptPasswordEncoder);
    }
}
