package com.njamb.geodrink.models;

import com.google.firebase.database.IgnoreExtraProperties;

/**
 * Created by njamb94 on 7/17/2017.
 */

@IgnoreExtraProperties
public class Place {
    public Coordinates location;
    public String imageUrl;
    public String date;
    public String time;
    public String name;
    public String addedBy;

    public Place() {}

    public Place(String name, String date, String time, String addedBy, Coordinates location) {
        this.name = name;
        this.date = date;
        this.time = time;
        this.addedBy = addedBy;
        this.location = location;

        imageUrl = "http://bit.ly/2tuVdKI";
    }
}
