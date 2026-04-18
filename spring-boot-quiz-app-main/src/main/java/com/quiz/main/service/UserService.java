package com.quiz.main.service;

import com.quiz.main.model.User;
import com.quiz.main.model.UserDto;
import com.quiz.main.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public User registerNewUser(UserDto userDto) {
        String username = userDto.getUsername() != null ? userDto.getUsername().trim() : "";
        String email = userDto.getEmail() != null ? userDto.getEmail().trim() : "";

        if (username.isEmpty() || email.isEmpty() || userDto.getPassword() == null || userDto.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("All fields are required.");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("That username is already taken. Please choose another one.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("That email is already registered. Try logging in instead.");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(userDto.getPassword()); // Storing password as-is, without encoding
        newUser.setEmail(email);
        newUser.setRole("ROLE_USER"); // Assign the default role directly as a string

        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalArgumentException("That username or email is already registered.");
        }
    }

    public User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UsernameNotFoundException("No authenticated user found");
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

    // Other service methods
}
