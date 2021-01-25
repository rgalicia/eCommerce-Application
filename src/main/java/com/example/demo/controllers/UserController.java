package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @GetMapping("/id/{id}")
    public ResponseEntity<User> findById(@PathVariable Long id) {
         return ResponseEntity.of(userRepository.findById(id));
    }

    @GetMapping("/{username}")
    public ResponseEntity<User> findByUserName(@PathVariable String username) {
        User user = userRepository.findByUsername(username);
        return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
    }

    @PostMapping("/create")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {

        // password validation before saving
        if (createUserRequest.getPassword().length() < 7 || !createUserRequest.getPassword().equals(createUserRequest.getConfirmPassword())) {
            log.error("Error with user password. Cannot create user {}.", createUserRequest.getUsername());
            return ResponseEntity.badRequest().build();
        }

        // username and password are ok, continue
        User user = new User();
        user.setUsername(createUserRequest.getUsername());
        log.info("User name set with {}.", createUserRequest.getUsername());

        Cart cart = new Cart();
        cartRepository.save(cart);
        log.debug("Cart {} saved for proposed username {}.", cart.getId(), createUserRequest.getUsername());
        user.setCart(cart);

        {   // get salt and cipher
            final String encoded = bCryptPasswordEncoder.encode(createUserRequest.getPassword());
            user.setPassword(encoded); // includes salt
        }

        try {
            userRepository.save(user);
            log.info("Saved user {}.", user.getUsername());

            // don't return sensible data, only identification data
            User returnUser = new User();
            returnUser.setId(user.getId());
            returnUser.setUsername(user.getUsername());
            user = null;
            return ResponseEntity.ok(returnUser);

        } catch (RuntimeException rte) {
            // rollback
            cartRepository.delete(cart);
            log.debug("Saved cart {} delete for proposed username {}.", cart.getId(), createUserRequest.getUsername());
            log.error("Error saving username {}: {}", createUserRequest.getUsername(), rte.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

}
