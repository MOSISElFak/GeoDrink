package com.njamb.geodrink.utils


import com.google.android.gms.maps.model.Marker
import com.google.common.collect.BiMap
import com.njamb.geodrink.models.MarkerTagModel

class FilterHelper private constructor(private val mMarkers: BiMap<String, Marker>) {

    fun setVisibility(v: Boolean, cond: (tag: MarkerTagModel) -> Boolean) {
        mMarkers.inverse().keys.forEach {
            val tag = it.tag as MarkerTagModel
            if (cond(tag)) {
                tag.previousVisibilityState = it.isVisible
                it.isVisible = v
            }
        }
    }

    fun filter(text: String) {
        for (marker in mMarkers.inverse().keys) {
            val tag = marker.tag as MarkerTagModel
            marker.isVisible = tag.previousVisibilityState
            if (!tag.name.toLowerCase().contains(text.toLowerCase())) {
                marker.isVisible = false
            }
        }
    }

    companion object {
        var usersVisible = true
        var placesVisible = true
        var friendsVisible = true
        var rangeQueryEnabled = true

        private var ourInstance: FilterHelper? = null
        private val mutex = Any()


        fun getInstance(markers: BiMap<String, Marker>): FilterHelper {
            if (ourInstance == null) {
                synchronized(mutex) {
                    if (ourInstance == null) ourInstance = FilterHelper(markers)
                }
            }
            return ourInstance!!
        }
    }
}
