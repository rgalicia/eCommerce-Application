package com.example.demo.controllers;

import java.util.Optional;
import java.util.stream.IntStream;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.ModifyCartRequest;

@Slf4j
@RestController
@RequestMapping("/api/cart")
public class CartController {

	@Autowired private UserRepository userRepository;
	
	@Autowired private CartRepository cartRepository;
	
	@Autowired private ItemRepository itemRepository;
	
	@PostMapping("/addToCart")
	public ResponseEntity<Cart> addToCart(@RequestBody ModifyCartRequest request) {
		User user = userRepository.findByUsername(request.getUsername());
		if(user == null) {
			log.error("Username '{}' not found.", request.getUsername());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Optional<Item> item = itemRepository.findById(request.getItemId());
		if(!item.isPresent()) {
			log.error("Item {} not found.", request.getItemId());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Cart cart = user.getCart();
		IntStream.range(0, request.getQuantity())
			.forEach(i -> cart.addItem(item.get()));
		cartRepository.save(cart);
		log.info("User '{}' added {} items with id {} to cart.", user.getUsername(), request.getQuantity(), request.getItemId());
		return ResponseEntity.ok(cart);
	}
	
	@PostMapping("/removeFromCart")
	public ResponseEntity<Cart> removeFromCart(@RequestBody ModifyCartRequest request) {
		User user = userRepository.findByUsername(request.getUsername());
		if(user == null) {
			log.error("Username '{}' not found.", request.getUsername());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Optional<Item> item = itemRepository.findById(request.getItemId());
		if(!item.isPresent()) {
			log.error("Item {} not found.", request.getItemId());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		Cart cart = user.getCart();
		IntStream.range(0, request.getQuantity())
			.forEach(i -> cart.removeItem(item.get()));
		cartRepository.save(cart);
		log.info("User '{}' removed {} items with id {} to cart.", user.getUsername(), request.getQuantity(), request.getItemId());
		return ResponseEntity.ok(cart);
	}

	// added to check Cart Details
	@GetMapping("/details/{username}")
	public ResponseEntity<Cart> get(@PathVariable String username) {
		final User user = userRepository.findByUsername(username);
		if(user == null) {
			log.error("Username '{}' not found.", username);
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		final Cart cart = user.getCart();
		log.info("User '{}' retrieves {} items from cart.", user.getUsername(), cart.getItems().size());
		return ResponseEntity.ok(cart);
	}
}
