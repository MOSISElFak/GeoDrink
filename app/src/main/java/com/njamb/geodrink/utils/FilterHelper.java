package com.njamb.geodrink.utils;


import com.google.android.gms.maps.model.Marker;
import com.google.common.collect.BiMap;
import com.njamb.geodrink.models.MarkerTagModel;

public class FilterHelper {
    public static boolean usersVisible = true;
    public static boolean placesVisible = true;
    public static boolean friendsVisible = true;
    public static boolean rangeQueryEnabled = true;

    private static FilterHelper ourInstance = null;
    private static final Object mutex = new Object();
    private BiMap<String, Marker> mMarkers;


    public static FilterHelper getInstance(BiMap<String, Marker> markers) {
        if (ourInstance == null) {
            synchronized (mutex) {
                if (ourInstance == null) ourInstance = new FilterHelper(markers);
            }
        }
        return ourInstance;
    }

    private FilterHelper(BiMap<String, Marker> markers) {
        mMarkers = markers;
    }

    public void setUsersVisibility(boolean v) {
        FilterHelper.usersVisible = v;
        setVisibility(new UserStrategy(), v);
    }

    public void setFriendsVisibility(boolean v) {
        FilterHelper.friendsVisible = v;
        setVisibility(new FriendStrategy(), v);
    }

    public void setPlacesVisibility(boolean v) {
        FilterHelper.placesVisible = v;
        setVisibility(new PlaceStrategy(), v);
    }

    private void setVisibility(Strategy strategy, boolean v) {
        for (Marker marker : mMarkers.inverse().keySet()) {
            MarkerTagModel tag = (MarkerTagModel) marker.getTag();
            assert tag != null;
            if (strategy.is(tag)) {
                tag.previousVisibilityState = marker.isVisible();
                marker.setVisible(v);
            }
        }
    }

    public void filter(String text) {
        for (Marker marker : mMarkers.inverse().keySet()) {
            MarkerTagModel tag = (MarkerTagModel) marker.getTag();
            assert tag != null;
            marker.setVisible(tag.previousVisibilityState);
            if (!tag.name.toLowerCase().contains(text.toLowerCase())) {
                marker.setVisible(false);
            }
        }
    }



    private interface Strategy {
        boolean is(MarkerTagModel tag);
    }

    private class UserStrategy implements Strategy {
        @Override
        public boolean is(MarkerTagModel tag) {
            return tag.isUser;
        }
    }
    private class FriendStrategy implements Strategy {
        @Override
        public boolean is(MarkerTagModel tag) {
            return tag.isFriend;
        }
    }
    private class PlaceStrategy implements Strategy {
        @Override
        public boolean is(MarkerTagModel tag) {
            return tag.isPlace;
        }
    }
}
