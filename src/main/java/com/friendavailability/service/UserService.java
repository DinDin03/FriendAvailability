package com.friendavailability.service;

import com.friendavailability.model.User;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class UserService {
    private final List<User> users = new ArrayList<>();
    private Long nextId = 1L;

    public UserService() {
        // Let's add some sample users so we have data to work with
        User alice = new User("Alice Johnson", "alice@gmail.com");
        alice.setId(nextId++);
        alice.setGoogleId("google_alice_123");

        User bob = new User("Bob Smith", "bob@gmail.com");
        bob.setId(nextId++);
        bob.setGoogleId("google_bob_456");

        users.add(alice);
        users.add(bob);

        System.out.println("UserService created with sample data");
    }

    public User createUser(String name, String email) {
        User newUser = new User(name, email);
        newUser.setId(nextId++);

        users.add(newUser);

        System.out.println("Created new User: " + newUser);
        return newUser;
    }

    public List<User> findAllUsers() {
        return new ArrayList<>(users);
    }

    public Optional<User> findUserById(Long id) {
        return users.stream()
                .filter(user -> user.getId().equals(id))
                .findFirst();
    }

    public Optional<User> findUserByEmail(String email){
        return users.stream()
                .filter(user -> user.getEmail().equals(email))
                .findFirst();
    }

    public boolean deleteUserById(Long id){
        boolean removed = users.removeIf(user -> user.getId().equals(id));
        if(removed){
            System.out.println("Deleted user with id " + id);
            return true;
        }
        System.out.println("User with id " + id + " not found");
        return false;
    }

    public Optional<User> updateUser(Long id, String name, String email) {
        Optional<User> userOpt = findUserById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setName(name);
            user.setEmail(email);
            return Optional.of(user);
        }
        return Optional.empty();
    }

}

