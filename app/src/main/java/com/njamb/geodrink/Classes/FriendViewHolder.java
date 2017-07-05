package com.njamb.geodrink.Classes;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.njamb.geodrink.R;


public class FriendViewHolder extends RecyclerView.ViewHolder {
    public TextView tvFullName;
    public TextView tvUsername;
    public TextView tvEmail;
    public ImageView ivProfileImg;

    public FriendViewHolder(View itemView) {
        super(itemView);

        tvFullName = (TextView) itemView.findViewById(R.id.friend_full_name);
        tvUsername = (TextView) itemView.findViewById(R.id.friend_username);
        tvEmail = (TextView) itemView.findViewById(R.id.friend_email);
        ivProfileImg = (ImageView) itemView.findViewById(R.id.friend_profile_pic);
    }
}
