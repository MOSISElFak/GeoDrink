package com.njamb.geodrink.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;

/**
 * Created by njamb94 on 7/17/2017.
 */

@IgnoreExtraProperties
public class Places {
    public HashMap<String, Object> places = new HashMap<>();

    public Places() {
    }
}
