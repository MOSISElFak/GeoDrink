package com.njamb.geodrink.models

import com.google.firebase.database.IgnoreExtraProperties


@IgnoreExtraProperties
class Place {
    var location: Coordinates
    var imageUrl: String
    var date: String
    var time: String
    var name: String
    var addedBy: String

    constructor() {}

    constructor(name: String, date: String, time: String, addedBy: String, location: Coordinates) {
        this.name = name
        this.date = date
        this.time = time
        this.addedBy = addedBy
        this.location = location

        imageUrl = "http://enigmaescape.gr/images/comingsoon.jpg"
    }

    override fun toString(): String {
        return "\nName: " + name + "\n" +
                "Date: " + date + "\n" +
                "Time: " + time + "\n" +
                "Added By: " + addedBy + "\n"
    }
}
