
package com.friendavailability.controller;

import com.friendavailability.model.User;
import com.friendavailability.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import javax.sound.midi.SysexMessage;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController{
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
        System.out.println("UserController created and connected to UserService");
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(){
        System.out.println("Getting all users");
        try{
            List<User> users = userService.findAllUsers();
            return ResponseEntity.ok(users);
        }catch(Exception err){
            System.err.println("Error getting all users: " + err.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id){
        System.out.println("Getting user with id: " + id);
        try{
            Optional<User> user = userService.findUserById(id);
            return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        }catch(Exception err){
            System.err.println("Error getting user by id " + id + ": " + err.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-email")
    public ResponseEntity<User> getUserByEmail(@RequestParam String email){
        System.out.println("Getting user with email " + email);
        try{
            Optional<User> user = userService.findUserByEmail(email);
            return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        }catch(Exception e){
            System.out.println("Error getting user by email: " + email + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user){
        System.out.println("Creating user: " + user);
        try{
            User createdUser = userService.createUser(user.getName(), user.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        }catch(RuntimeException e){
            System.err.println("Error creating user: " + e.getMessage());
            if(e.getMessage().contains("already exists")){
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }else{
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }catch(Exception e){
            System.err.println("Unexpected error creating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user){
        System.out.println("Updating user with Id " + id + ": " + user);
        try{
            Optional<User> updatedUser = userService.updateUser(id, user.getName(), user.getEmail());
            return updatedUser.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
        }catch(RuntimeException e){
            System.err.println("Error updating user: " + e.getMessage());
            if(e.getMessage().contains("already exists")){
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }else{
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }
        }catch(Exception e){
            System.err.println("Unexpected error updating user: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id){
        System.out.println("Deleting user with id: " + id);
        try{
            boolean deleted = userService.deleteUserById(id);

            if(deleted){
                return ResponseEntity.ok("User with id " + id + " deleted");
            }else{
                return ResponseEntity.notFound().build();
            }
        }catch(Exception e){
            System.err.println("Error deleting user with id " + id + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}