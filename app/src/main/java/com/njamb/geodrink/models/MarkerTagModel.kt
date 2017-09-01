package com.njamb.geodrink.models

import com.njamb.geodrink.utils.FilterHelper

class MarkerTagModel
// more?

private constructor(var isUser: Boolean, var isFriend: Boolean, var isPlace: Boolean, var id: String, var name: String) {
    var previousVisibilityState: Boolean = false /* for filtering only */

    init {
        // set previous visibility state to value corresponding to tag type (without too many ifs)
        previousVisibilityState = isUser && FilterHelper.usersVisible
                || isFriend && FilterHelper.friendsVisible
                || isPlace && FilterHelper.placesVisible
    }

    fun setIsFriend() {
        isUser = false
        isFriend = true
        previousVisibilityState = FilterHelper.friendsVisible
    }

    override fun toString(): String {
        return String.format("Id: %s\nName: %s\nUser: %s\nFriend: %s\nPlace: %s\n",
                             id, name,
                             isUser.toString(), isFriend.toString(), isPlace.toString())
    }

    companion object {

        fun createPlaceTag(id: String, name: String): MarkerTagModel {
            return MarkerTagModel(false, false, true, id, name)
        }

        fun createUserTag(id: String, name: String, isFriend: Boolean): MarkerTagModel {
            return MarkerTagModel(!isFriend, isFriend, false, id, name)
        }
    }
}
