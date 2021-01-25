package com.example.demo.security;

import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@EnableWebSecurity
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

	@Autowired private UserDetailsServiceImpl userDetailsService;
    @Autowired private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        // overridden just to be wired as @Bean used by JWTAuthenticationVerificationFilter constructor
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        log.debug("configuring AuthenticationManagerBuilder...");
        // https://knowledge.udacity.com/questions/293950
        // https://www.freecodecamp.org/news/how-to-setup-jwt-authorization-and-authentication-in-spring/
        // avoid infinite loop when logging with invalid user
        auth
                .userDetailsService(userDetailsService)
                .passwordEncoder(bCryptPasswordEncoder);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        log.debug("configuring HttpSecurity...");

        // global configuration
        http
                .cors().disable().csrf().disable()
                .formLogin().disable()  // no MVC, disable form login
                .logout().and()         // customize logout
                .anonymous().disable()  // disable anonymous usage

                // request authorization
                .authorizeRequests()
                .filterSecurityInterceptorOncePerRequest(true)
                .antMatchers(HttpMethod.POST, SecurityConstants.SIGN_UP_URL)
                    .permitAll()
                .anyRequest()
                    .denyAll()
                .and()

                // filters used to implement JWT authentication
                .addFilter(new JWTAuthenticationVerificationFilter(authenticationManager()))
                .addFilter(new JWTAuthenticationFilter(authenticationManager()))

                // session management
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }
}
