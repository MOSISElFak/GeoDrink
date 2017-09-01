package com.njamb.geodrink.view_holders

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

import com.njamb.geodrink.R


class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var tvFullName: TextView
    var tvUsername: TextView
    var tvEmail: TextView
    var ivProfileImg: ImageView

    init {

        tvFullName = itemView.findViewById(R.id.friend_full_name) as TextView
        tvUsername = itemView.findViewById(R.id.friend_username) as TextView
        tvEmail = itemView.findViewById(R.id.friend_email) as TextView
        ivProfileImg = itemView.findViewById(R.id.friend_profile_pic) as ImageView
    }
}
