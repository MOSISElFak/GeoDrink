package com.njamb.geodrink.utils;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.njamb.geodrink.R;
import com.njamb.geodrink.activities.MapActivity;
import com.njamb.geodrink.activities.ProfileActivity;
import com.njamb.geodrink.models.Place;
import com.njamb.geodrink.models.User;

import java.util.Date;

public final class NotificationHelper {
    public static void displayUserNotification(final String id,
                                               final Context context,
                                               final double dist) {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        FirebaseDatabase.getInstance().getReference(String.format("users/%s", id))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User user = dataSnapshot.getValue(User.class);
                        assert user != null;
                        notificationBuilder
                                .setSmallIcon(R.mipmap.geodrink_blue_logo)
                                .setContentTitle(String.format("%s is near you", user.fullName))
                                .setContentText(String.format("It's only %d meters from you", (int)dist))
                                .setAutoCancel(true);
                        Intent intent = new Intent(context, ProfileActivity.class);
                        intent.putExtra("userId", id);
                        PendingIntent resultPendingIntent = PendingIntent
                                .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        notificationBuilder.setContentIntent(resultPendingIntent);
                        NotificationManager notificationManager
                                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(uniqueId(), notificationBuilder.build());
                    }

                    @Override public void onCancelled(DatabaseError databaseError) {}
                });
    }

    public static void displayPlaceNotification(final String id,
                                                final Context context,
                                                final double dist) {
        final NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context);
        FirebaseDatabase.getInstance().getReference(String.format("places/%s", id))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Place place = dataSnapshot.getValue(Place.class);
                        // TODO: do not display notification for 'my' places
                        // add 'addedBy' field in places ?
                        assert place != null;
                        notificationBuilder
                                .setSmallIcon(R.mipmap.geodrink_blue_logo)
                                .setContentTitle(String.format("%s is near you", place.name))
                                .setContentText(String.format("It's only %d meters from you", (int)dist))
                                .setAutoCancel(true);
                        Intent intent = new Intent(context, MapActivity.class);
                        // TODO: change from MapActivity to DetailsActivity & add placeId
                        PendingIntent resultPendingIntent = PendingIntent
                                .getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                        notificationBuilder.setContentIntent(resultPendingIntent);
                        NotificationManager notificationManager
                                = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.notify(uniqueId(), notificationBuilder.build());
                    }

                    @Override public void onCancelled(DatabaseError databaseError) {}
                });
    }

    private static int cnt = 0;

    private static int uniqueId() {
        cnt = (cnt + 1) % Integer.MAX_VALUE;
        return cnt;
    }
}
