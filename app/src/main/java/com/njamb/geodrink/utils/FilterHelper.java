package com.njamb.geodrink.utils;


import com.google.android.gms.maps.model.Marker;
import com.google.common.collect.BiMap;
import com.njamb.geodrink.models.MarkerTagModel;

public class FilterHelper {
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
        for (Marker marker : mMarkers.inverse().keySet()) {
            MarkerTagModel tag = (MarkerTagModel) marker.getTag();
            assert tag != null;
            if (tag.isUser) {
                marker.setVisible(v);
            }
        }
    }

    public void setFriendsVisibility(boolean v) {
        for (Marker marker : mMarkers.inverse().keySet()) {
            MarkerTagModel tag = (MarkerTagModel) marker.getTag();
            assert tag != null;
            if (tag.isFriend) {
                marker.setVisible(v);
            }
        }
    }

    public void setPlacesVisibility(boolean v) {
        for (Marker marker : mMarkers.inverse().keySet()) {
            MarkerTagModel tag = (MarkerTagModel) marker.getTag();
            assert tag != null;
            if (tag.isPlace) {
                marker.setVisible(v);
            }
        }
    }
}
