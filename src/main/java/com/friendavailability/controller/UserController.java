package com.friendavailability.controller;

import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController //marks the class as a Spring REST controller
@RequestMapping("/api/users") //maps the class to the /api/users endpoint
public class UserController { //defines the user controller
    private final UserService userService; //stores the user service

    public UserController(UserService userService) { //constructor with user service
        this.userService = userService; //set the user service
        System.out.println("UserController created and connected to UserService"); //print the message
    }

    @GetMapping
    public List<User> getAllUsers(){ //get all users
        System.out.println("Getting all users"); //print the message
        return userService.findAllUsers(); //return the users
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id){ //get a user by id
        System.out.println("Getting user by id: " + id); //print the message

        Optional<User> user = userService.findUserById(id); //find the user by id

        if(user.isPresent()) { //check if the user is found
            return user.get(); //return the user
        }else{
            throw new NoSuchElementException("User not found with id: " + id); //throw an exception if the user is not found
        }
    }

    @GetMapping("/by-email")
    public User getUserById(@RequestParam String email){
        System.out.println("Getting user by email: " + email);

        Optional<User> user = userService.findUserByEmail(email);

        if(user.isPresent()) { //check if the user is found
            return user.get(); //return the user
        }else{
            throw new NoSuchElementException("User not found with email: " + email); //throw an exception if the user is not found
        }
    }

    @PostMapping
    public User createUser(@RequestBody User user){ //create a user
        System.out.println("Creating user: " + user); //print the message
        return userService.createUser(user.getName(), user.getEmail()); //create a user
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updatedUser){ //update a user
        System.out.println("Updating user: " + updatedUser); //print the message
        return userService.updateUser(id, updatedUser.getName(), updatedUser.getEmail())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id)); //throw an exception if the user is not found
    }


    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id){ //delete a user by id
        System.out.println("Delete user with id " + id); //print the message
        boolean deleted = userService.deleteUserById(id); //delete a user by id

        if(deleted){ //check if the user is deleted
            return "Deleted user with id " + id; //return the message
        }else{
            throw new NoSuchElementException("User with id " + id + " not found"); //throw an exception if the user is not found
        }
    }


}
