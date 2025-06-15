package com.friendavailability.service;

import com.friendavailability.model.User;
import org.springframework.stereotype.Service;
import java.util.*;

@Service //marks the class as a Spring service
public class UserService {
    private final List<User> users = new ArrayList<>(); //stores the users
    private Long nextId = 1L; //stores the next id

    public UserService() { //constructor
        // Let's add some sample users so we have data to work with
        User alice = new User("Alice Johnson", "alice@gmail.com"); //create a user
        alice.setId(nextId++); //set the id
        alice.setGoogleId("google_alice_123"); //set the google id

        User bob = new User("Bob Smith", "bob@gmail.com"); //create a user
        bob.setId(nextId++); //set the id
        bob.setGoogleId("google_bob_456"); //set the google id

        users.add(alice); //add the user to the list
        users.add(bob); //add the user to the list

        System.out.println("UserService created with sample data"); //print the message
    }

    public User createUser(String name, String email) { //create a user
        User newUser = new User(name, email); //create a user
        newUser.setId(nextId++); //set the id

        users.add(newUser); //add the user to the list

        System.out.println("Created new User: " + newUser); //print the message
        return newUser; //return the user
    }

    public List<User> findAllUsers() { //find all users
        return new ArrayList<>(users); //return the users
    }

    public Optional<User> findUserById(Long id) { //find a user by id
        return users.stream() //stream the users
                .filter(user -> user.getId().equals(id)) //filter the users by id
                .findFirst(); //find the first user
    }

    public Optional<User> findUserByEmail(String email){ //find a user by email
        return users.stream() //stream the users
                .filter(user -> user.getEmail().equals(email)) //filter the users by email
                .findFirst(); //find the first user
    }

    public boolean deleteUserById(Long id){ //delete a user by id
        boolean removed = users.removeIf(user -> user.getId().equals(id)); //remove the user
        if(removed){ //check if the user is removed
            System.out.println("Deleted user with id " + id); //print the message
            return true; //return true if the user is removed
        }
        System.out.println("User with id " + id + " not found"); //print the message
        return false; //return false if the user is not removed
    }

    public Optional<User> updateUser(Long id, String name, String email) { //update a user
        Optional<User> userOpt = findUserById(id); //find the user by id
        if (userOpt.isPresent()) { //check if the user is found
            User user = userOpt.get(); //get the user
            user.setName(name); //set the name
            user.setEmail(email); //set the email
            return Optional.of(user); //return the user
        }
        return Optional.empty(); //return empty if the user is not found
    }

}

