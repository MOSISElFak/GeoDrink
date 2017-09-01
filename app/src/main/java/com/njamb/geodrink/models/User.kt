package com.njamb.geodrink.models

import com.google.firebase.database.IgnoreExtraProperties

import java.util.HashMap


@IgnoreExtraProperties
class User : Comparable<*> {
    var fullName: String
    var username: String
    var email: String
    var birthday: String
    var profileUrl: String? = null
    var location: Coordinates? = null
    var friends = HashMap<String, Boolean>()
    var places = HashMap<String, Boolean>()
    var drinks = initDrinks()
    var points: Long = 0

    constructor() {}

    constructor(fullName: String, username: String, email: String, birthday: String) {
        this.fullName = fullName
        this.username = username
        this.email = email
        this.birthday = birthday
        this.points = 0L
    }

    private fun initDrinks(): HashMap<String, Long> {
        val hm = HashMap<String, Long>()

        hm.put("beer", 0L)
        hm.put("coffee", 0L)
        hm.put("cocktail", 0L)
        hm.put("juice", 0L)
        hm.put("soda", 0L)
        hm.put("alcohol", 0L)

        return hm
    }

    override operator fun compareTo(o: Any): Int {
        return ((o as User).points - this.points).toInt()
    }

    override fun equals(obj: Any?): Boolean {
        // email is unique for sure
        return obj is User && this.email == obj.email
    }
}
