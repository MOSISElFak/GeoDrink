package com.njamb.geodrink.models;

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

    public void setIsFriend() {
        isUser = false;
        isFriend = true;
    }

    public static MarkerTagModel createPlaceTag(String id, String name) {
        return new MarkerTagModel(false, false, true, id, name);
    }

    public static MarkerTagModel createUserTag(String id, String name, boolean isFriend) {
        return new MarkerTagModel(!isFriend, isFriend, false, id, name);
    }

    @Override
    public String toString() {
        return String.format("Id: %s\nName: %s\nUser: %s\nFriend: %s\nPlace: %s\n",
                id, name,
                String.valueOf(isUser), String.valueOf(isFriend), String.valueOf(isPlace));
    }
}
