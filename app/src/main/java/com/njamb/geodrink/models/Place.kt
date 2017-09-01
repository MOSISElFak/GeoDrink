package com.njamb.geodrink.models

import com.google.firebase.database.IgnoreExtraProperties


@IgnoreExtraProperties
class Place {
    lateinit var location: Coordinates
    lateinit var imageUrl: String
    lateinit var date: String
    lateinit var time: String
    lateinit var name: String
    lateinit var addedBy: String

    constructor() {}

    constructor(name: String, date: String, time: String, addedBy: String, location: Coordinates) {
        this.name = name
        this.date = date
        this.time = time
        this.addedBy = addedBy
        this.location = location

        imageUrl = "http://enigmaescape.gr/images/comingsoon.jpg"
    }

    override fun toString(): String = """
        Name: $name
        Date: $date
        Time: $time
        Added By: $addedBy"""
}
