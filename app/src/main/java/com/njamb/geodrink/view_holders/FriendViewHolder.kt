package com.njamb.geodrink.view_holders

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.list_item_friends.view.*

class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var tvFullName: TextView = itemView.friend_full_name
    var tvUsername: TextView = itemView.friend_username
    var tvEmail: TextView = itemView.friend_email
    var ivProfileImg: ImageView = itemView.friend_profile_pic

}
