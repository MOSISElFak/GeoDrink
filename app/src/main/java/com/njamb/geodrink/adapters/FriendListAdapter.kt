package com.njamb.geodrink.adapters

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.njamb.geodrink.R
import com.njamb.geodrink.models.User
import com.njamb.geodrink.view_holders.FriendViewHolder

import java.util.ArrayList

class FriendListAdapter(private val mContext: Context, private val mRecyclerView: RecyclerView, u: User) : RecyclerView.Adapter<FriendViewHolder>() {
    private val mFriends = ArrayList<User>()

    private val mUsersDatabase: DatabaseReference

    init {

        mUsersDatabase = FirebaseDatabase.getInstance().getReference("users")

        for (key in u.friends.keys) {
            // Get friend info
            mUsersDatabase.child(key).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    mFriends.add(dataSnapshot.getValue(User::class.java))
                    notifyItemInserted(mFriends.size - 1)
                }

                override fun onCancelled(databaseError: DatabaseError) {}
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val inflater = LayoutInflater.from(mContext)
        val view = inflater.inflate(R.layout.list_item_friends, parent, false)
        view.setOnClickListener { v ->
            val index = mRecyclerView.getChildLayoutPosition(v)
            // TODO: open profile for that user?
            Toast.makeText(mContext,
                           String.format("%s says hi", mFriends[index].username),
                           Toast.LENGTH_SHORT).show()
        }
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friend = mFriends[position]

        holder.tvUsername.text = friend.username
        holder.tvFullName.text = friend.fullName
        Glide.with(mContext)
                .load(friend.profileUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .apply(RequestOptions.circleCropTransform())
                .into(holder.ivProfileImg)
        holder.tvEmail.text = friend.email
    }

    override fun getItemCount(): Int {
        return mFriends.size
    }
}
