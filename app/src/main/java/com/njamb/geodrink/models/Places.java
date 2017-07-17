package com.njamb.geodrink.models;

/**
 * Created by njamb94 on 7/17/2017.
 */

public class Places {
    public String[] places;

    public Places() {
    }

    @Override
    public String toString() {
        String str = "";
        for (int i = 0; i < places.length; i++) {
            str += places[i] + "\n";
        }

        return str;
    }
}
