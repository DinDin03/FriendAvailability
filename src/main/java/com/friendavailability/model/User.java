package com.friendavailability.model;

//defines the user entity 
public class User {
    private Long id; //stores the internal id of the user
    private String name; //stores the name of the user
    private String email; //stores the email of the user
    private String googleId; //google id of the user

    public User() {} //default constructor

    public User(final Long id, final String name, final String email, final String googleId) { //constructor with all fields
        this.id = id; //set the id
        this.name = name; //set the name
        this.email = email; //set the email
        this.googleId = googleId; //set the google id
    }

    public User(String alice, String mail) { //constructor with name and email
        this.email = mail; //set the email
        this.name = alice; //set the name
    }

    public Long getId() { //get the id
        return id; //return the id
    }

    public void setId(Long id) { //set the id
        this.id = id;
    }

    public String getName() {
        return name; //return the name
    }

    public void setName(String name) { //set the name
        this.name = name; //set the name
    }

    public String getEmail() {
        return email; //return the email
    }

    public void setEmail(String email) { //set the email
        this.email = email; //set the email
    }

    public String getGoogleId() {
        return googleId; //return the google id
    }

    public void setGoogleId(String googleId) { //set the google id
        this.googleId = googleId; //set the google id
    }

    @Override
    public String toString() { //return the string representation of the user
        return "User{" + //return the string representation of the user
                "id=" + id + //return the id
                ", name='" + name + '\'' +
                ", email='" + email + '\'' + //return the email
                ", googleId='" + googleId + '\'' + //return the google id
                '}'; //return the string representation of the user
    }
}

