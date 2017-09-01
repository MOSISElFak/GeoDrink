package com.njamb.geodrink.models

import com.njamb.geodrink.utils.FilterHelper

class MarkerTagModel private constructor(var isUser: Boolean,
                                         var isFriend: Boolean,
                                         var isPlace: Boolean,
                                         var id: String,
                                         var name: String) {
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

    override fun toString(): String = """
        Id: $id
        Name: $name
        User: $isUser
        Friend: $isFriend
        Place: $isPlace
    """.trimIndent()

    companion object {

        fun createPlaceTag(id: String, name: String): MarkerTagModel =
                MarkerTagModel(isUser=false, isFriend=false, isPlace=true, id=id, name=name)

        fun createUserTag(id: String, name: String, isFriend: Boolean): MarkerTagModel =
                MarkerTagModel(isUser=!isFriend, isFriend=isFriend, isPlace=false, id=id, name=name)
    }
}
