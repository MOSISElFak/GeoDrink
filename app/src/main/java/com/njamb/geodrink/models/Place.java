package com.njamb.geodrink.models;

import com.google.firebase.database.IgnoreExtraProperties;


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

        imageUrl = "http://enigmaescape.gr/images/comingsoon.jpg";
    }

    @Override
    public String toString() {
        return "\nName: " + name + "\n" +
                "Date: " + date + "\n" +
                "Time: " + time + "\n" +
                "Added By: " + addedBy + "\n";
    }
}
