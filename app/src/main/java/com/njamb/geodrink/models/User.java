package com.njamb.geodrink.models;

import android.support.annotation.NonNull;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;


@IgnoreExtraProperties
public class User implements Comparable {
    public String fullName;
    public String username;
    public String email;
    public String birthday;
    public String profileUrl;
    public Coordinates location;
    public HashMap<String, Boolean> friends = new HashMap<>();

    public User() {}

    public User(String username, String email, String birthday) {
        this.username = username;
        this.email = email;
        this.birthday = birthday;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        // TODO: change to points [o.points - this.points]
        return ((User)o).email.compareTo(this.email);
    }
}
