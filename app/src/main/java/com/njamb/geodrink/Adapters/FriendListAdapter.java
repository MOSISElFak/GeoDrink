package com.njamb.geodrink.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.njamb.geodrink.ViewHolders.FriendViewHolder;
import com.njamb.geodrink.Models.User;
import com.njamb.geodrink.R;

import java.util.ArrayList;

public class FriendListAdapter extends RecyclerView.Adapter<FriendViewHolder> {
    private Context mContext;
    private ArrayList<User> mFriends = new ArrayList<>();
    private RecyclerView mRecyclerView;

    private DatabaseReference mUsersDatabase;

    public FriendListAdapter(final Context context, RecyclerView rv, User u) {
        mContext = context;
        mRecyclerView = rv;

        mUsersDatabase = FirebaseDatabase.getInstance().getReference("users");

        for (String key : u.friends.keySet()) {
            // Get friend info
            mUsersDatabase.child(key).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    mFriends.add(dataSnapshot.getValue(User.class));
                    notifyItemInserted(mFriends.size()-1);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
        }
    }

    @Override
    public FriendViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.list_item_friends, parent, false);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int index = mRecyclerView.getChildLayoutPosition(v);
                Toast.makeText(mContext,
                        String.format("%s says hi", mFriends.get(index).username),
                        Toast.LENGTH_SHORT).show();
            }
        });
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(FriendViewHolder holder, int position) {
        User friend = mFriends.get(position);

        holder.tvUsername.setText(friend.username);
        holder.tvFullName.setText(friend.fullName);
        Glide.with(mContext)
                .load(friend.profileUrl)
                .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                .apply(RequestOptions.circleCropTransform())
                .into(holder.ivProfileImg);
        holder.tvEmail.setText(friend.email);
    }

    @Override
    public int getItemCount() {
        return mFriends.size();
    }
}
