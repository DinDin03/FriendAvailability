package com.friendavailability.service;

import com.friendavailability.model.User;
import com.friendavailability.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService{

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User createUser(String name, String email){
        System.out.println("Creating User with name = " + name + " email = " + email);

        if(userRepository.existsByEmail(email)){
            throw new RuntimeException("User with email " + email + " already exists");
        }

        User newUser = User.builder()
                .name(name)
                .email(email)
                .isActive(true)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(newUser);
        System.out.println("Created new user: " + savedUser);
        return savedUser;
    }
}