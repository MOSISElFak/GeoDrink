package com.njamb.geodrink.Models;

public final class MarkerTagModel {
    public boolean isUser;
    public boolean isFriend;
    public boolean isPlace;
    public String id;
    public String name;
    // more?

    private MarkerTagModel(boolean u, boolean f, boolean p, String id, String name) {
        isUser = u;
        isFriend = f;
        isPlace = p;
        this.id = id;
        this.name = name;
    }

    public static MarkerTagModel createPlaceTag(String id, String name) {
        return new MarkerTagModel(false, false, true, id, name);
    }

    public static MarkerTagModel createUserTag(String id, String name, boolean isFriend) {
        return new MarkerTagModel(!isFriend, isFriend, false, id, name);
    }
}
