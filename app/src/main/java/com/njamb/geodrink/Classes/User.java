package com.njamb.geodrink.Classes;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by stefan on 7.6.17..
 */

@IgnoreExtraProperties
public class User {
    public String username;
    public String email;
    public String birthday;

    public User() {}

    public User(String username, String email, String birthday) {
        this.username = username;
        this.email = email;
        this.birthday = birthday;
    }
}
