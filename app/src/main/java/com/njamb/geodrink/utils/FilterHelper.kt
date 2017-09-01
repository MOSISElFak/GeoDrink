package com.njamb.geodrink.utils


import com.google.android.gms.maps.model.Marker
import com.google.common.collect.BiMap
import com.njamb.geodrink.models.MarkerTagModel

class FilterHelper private constructor(private val mMarkers: BiMap<String, Marker>) {

    fun setUsersVisibility(v: Boolean) {
        FilterHelper.usersVisible = v
        setVisibility(UserStrategy(), v)
    }

    fun setFriendsVisibility(v: Boolean) {
        FilterHelper.friendsVisible = v
        setVisibility(FriendStrategy(), v)
    }

    fun setPlacesVisibility(v: Boolean) {
        FilterHelper.placesVisible = v
        setVisibility(PlaceStrategy(), v)
    }

    private fun setVisibility(strategy: Strategy, v: Boolean) {
        for (marker in mMarkers.inverse().keys) {
            val tag = (marker.tag as MarkerTagModel?)!!
            if (strategy.`is`(tag)) {
                tag.previousVisibilityState = marker.isVisible
                marker.isVisible = v
            }
        }
    }

    fun filter(text: String) {
        for (marker in mMarkers.inverse().keys) {
            val tag = (marker.tag as MarkerTagModel?)!!
            marker.isVisible = tag.previousVisibilityState
            if (!tag.name.toLowerCase().contains(text.toLowerCase())) {
                marker.isVisible = false
            }
        }
    }


    private interface Strategy {
        fun `is`(tag: MarkerTagModel): Boolean
    }

    private inner class UserStrategy : Strategy {
        override fun `is`(tag: MarkerTagModel): Boolean {
            return tag.isUser
        }
    }

    private inner class FriendStrategy : Strategy {
        override fun `is`(tag: MarkerTagModel): Boolean {
            return tag.isFriend
        }
    }

    private inner class PlaceStrategy : Strategy {
        override fun `is`(tag: MarkerTagModel): Boolean {
            return tag.isPlace
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
            return ourInstance
        }
    }
}
