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
    public HashMap<String, Boolean> places = new HashMap<>();
    public HashMap<String, Long> drinks = initDrinks();
    public long points;

    public User() {}

    public User(String fullName, String username, String email, String birthday) {
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.birthday = birthday;
        this.points = 0L;
    }

    private HashMap<String, Long> initDrinks() {
        HashMap<String, Long> hm = new HashMap<>();

        hm.put("beer", 0L);
        hm.put("coffee", 0L);
        hm.put("cocktail", 0L);
        hm.put("juice", 0L);
        hm.put("soda", 0L);
        hm.put("alcohol", 0L);

        return hm;
    }

    @Override
    public int compareTo(@NonNull Object o) {
        return (int) (((User)o).points - this.points);
    }

    @Override
    public boolean equals(Object obj) {
        // email is unique for sure
        return obj instanceof User && this.email.equals(((User) obj).email);
    }
}
