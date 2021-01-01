package com.example.demo.controllers;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Matcher;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private static final String USERNAME_REGEXP = "[\\d\\w\\.-_]";

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

        // TODO check username: no empty, no spaces, no special characters... Use RegExp
        // password validation before saving
        // REQ Password should check some length requirement and a confirmation field in the request to check for typos
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
            // https://stackoverflow.com/questions/6832445/how-can-bcrypt-have-built-in-salts#6833165
            // parse the value to get salt and cyphered password
            // The first 22 characters decode to a 16-byte value for the salt. The remaining characters are cipher text to be compared for authentication.
            final String encoded = bCryptPasswordEncoder.encode(createUserRequest.getPassword());
            // final String saltAndCypher = encoded.split("\\$")[3];
            // user.setSalt(saltAndCypher.substring(0, 22).getBytes());
            user.setPassword(encoded); // includes salt
        }
        // TODO unique constraint can crash the save operation and Cart should be deleted
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
            // TODO How to use @Transactional here?
            log.debug("Saved cart {} delete for proposed username {}.", cart.getId(), createUserRequest.getUsername());
            cartRepository.delete(cart);
            log.error("Error saving username {}: {}", createUserRequest.getUsername(), rte.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

}
