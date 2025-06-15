package com.friendavailability.model;

public class User {
    private Long id; //stores the internal id of the user
    private String name; //stores the name of the user
    private String email; //stores the email of the user
    private String googleId;

    public User() {}
    public User(final Long id, final String name, final String email, final String googleId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.googleId = googleId;
    }

    public User(String alice, String mail) {
        this.email = mail;
        this.name = alice;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", googleId='" + googleId + '\'' +
                '}';
    }
}

