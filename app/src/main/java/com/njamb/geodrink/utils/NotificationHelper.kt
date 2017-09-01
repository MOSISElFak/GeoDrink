package com.njamb.geodrink.utils


import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.njamb.geodrink.R
import com.njamb.geodrink.activities.DetailsActivity
import com.njamb.geodrink.activities.ProfileActivity
import com.njamb.geodrink.models.Place
import com.njamb.geodrink.models.User

object NotificationHelper {
    fun displayUserNotification(id: String,
                                context: Context,
                                dist: Double) {
        val notificationBuilder = NotificationCompat.Builder(context)
        FirebaseDatabase.getInstance().getReference(String.format("users/%s", id))
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val user = dataSnapshot.getValue(User::class.java)!!
                        notificationBuilder
                                .setSmallIcon(R.mipmap.geodrink_blue_logo)
                                .setContentTitle(String.format("%s is near you", user.fullName))
                                .setContentText(String.format("It's only %d meters from you", dist.toInt()))
                                .setAutoCancel(true)
                        val intent = Intent(context, ProfileActivity::class.java)
                        intent.putExtra("userId", id)
                        val resultPendingIntent = PendingIntent
                                .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                        notificationBuilder.setContentIntent(resultPendingIntent)
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(uniqueId(), notificationBuilder.build())
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    fun displayPlaceNotification(id: String,
                                 context: Context,
                                 dist: Double) {
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        val notificationBuilder = NotificationCompat.Builder(context)
        FirebaseDatabase.getInstance().getReference(String.format("places/%s", id))
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        val place = dataSnapshot.getValue(Place::class.java)!!
                        if (place.addedBy == userId) return

                        notificationBuilder
                                .setSmallIcon(R.mipmap.geodrink_blue_logo)
                                .setContentTitle(String.format("%s is near you", place.name))
                                .setContentText(String.format("It's only %d meters from you", dist.toInt()))
                                .setAutoCancel(true)
                        val intent = Intent(context, DetailsActivity::class.java)
                        intent.putExtra("placeId", id)
                        val resultPendingIntent = PendingIntent
                                .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
                        notificationBuilder.setContentIntent(resultPendingIntent)
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(uniqueId(), notificationBuilder.build())
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })
    }

    private var cnt = 0

    private fun uniqueId(): Int {
        cnt = (cnt + 1) % Integer.MAX_VALUE
        return cnt
    }
}
