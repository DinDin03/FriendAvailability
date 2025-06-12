package com.friendavailability.controller;

import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
        System.out.println("UserController created and connected to UserService");
    }

    @GetMapping
    public List<User> getAllUsers(){
        System.out.println("Getting all users");
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id){
        System.out.println("Getting user by id: " + id);

        Optional<User> user = userService.findUserById(id);

        if(user.isPresent()) {
            return user.get();
        }else{
            throw new NoSuchElementException("User not found with id: " + id);
        }
    }

    @PostMapping
    public User createUser(@RequestBody User user){
        System.out.println("Creating user: " + user);
        return userService.createUser(user.getName(), user.getEmail());
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @RequestBody User updatedUser){
        System.out.println("Updating user: " + updatedUser);
        return userService.updateUser(id, updatedUser.getName(), updatedUser.getEmail())
                .orElseThrow(() -> new NoSuchElementException("User not found with id: " + id));
    }


    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id){
        System.out.println("Delete user with id " + id);
        boolean deleted = userService.deleteUserById(id);

        if(deleted){
            return "Deleted user with id " + id;
        }else{
            throw new NoSuchElementException("User with id " + id + " not found");
        }
    }


}
