package com.njamb.geodrink.Classes;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;


@IgnoreExtraProperties
public class User {
    public String fullName;
    public String username;
    public String email;
    public String birthday;
    public String profileUrl;
    public HashMap<String, Boolean> friends = new HashMap<>();

    public User() {}

    public User(String username, String email, String birthday) {
        this.username = username;
        this.email = email;
        this.birthday = birthday;
    }
}
