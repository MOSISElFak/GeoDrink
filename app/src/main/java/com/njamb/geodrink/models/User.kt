package com.njamb.geodrink.models

import com.google.firebase.database.IgnoreExtraProperties

import java.util.HashMap


@IgnoreExtraProperties
class User : Comparable<Any> {
    lateinit var fullName: String
    lateinit var username: String
    lateinit var email: String
    lateinit var birthday: String
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

    override operator fun compareTo(other: Any): Int = ((other as User).points - this.points).toInt()

    override fun equals(other: Any?): Boolean = // email is unique for sure
            other is User && this.email == other.email

    override fun hashCode(): Int {
        var result = fullName.hashCode()
        result = 31 * result + username.hashCode()
        result = 31 * result + email.hashCode()
        result = 31 * result + birthday.hashCode()
        result = 31 * result + (profileUrl?.hashCode() ?: 0)
        result = 31 * result + (location?.hashCode() ?: 0)
        result = 31 * result + friends.hashCode()
        result = 31 * result + places.hashCode()
        result = 31 * result + drinks.hashCode()
        result = 31 * result + points.hashCode()
        return result
    }
}
