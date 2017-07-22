package com.njamb.geodrink.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by njamb94 on 7/17/2017.
 */

@IgnoreExtraProperties
public class Places {
    public String places = "";

    public Places() {
    }

    public void addPlace(String str) {
        places += !places.contains(";") ? str : ";" + str;
    }

    @Override
    public String toString() {
        String str = "";
        String[] strarr = places.split(";");
        for (int i = 0; i < strarr.length; i++) {
            str += strarr[i] + "\n";
        }
        return str;
    }
}
