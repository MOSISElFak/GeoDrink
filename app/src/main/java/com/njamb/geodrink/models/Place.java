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

    public Place() {
//        lon = 0.0;
//        lat = 0.0;
//        imageUrl = "";
//        date = new SimpleDateFormat("dd/MM/yyyy").format(new Date());
//        time = new SimpleDateFormat("HH:mm:ss").format(new Date());
//        name = "";
    }


}
