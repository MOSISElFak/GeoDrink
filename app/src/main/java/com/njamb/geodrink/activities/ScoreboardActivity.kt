package com.njamb.geodrink.activities

import android.app.Activity
import android.databinding.BindingAdapter
import android.databinding.ObservableArrayList
import android.databinding.ObservableList
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.ImageView
import android.widget.Toast

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.github.nitrico.lastadapter.LastAdapter
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.njamb.geodrink.BR
import com.njamb.geodrink.R
import com.njamb.geodrink.models.User
import kotlinx.android.synthetic.main.activity_scoreboard.*

import java.util.Collections

fun Activity.toast(msg: String, len: Int = Toast.LENGTH_SHORT)
        = Toast.makeText(this, msg, len).show()

class ScoreboardActivity : AppCompatActivity() {

    private val listOfUsers = ObservableArrayList<User>()
    private var mChildEventListener: ChildEventListener? = null
    private lateinit var mLastAdapter: LastAdapter

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scoreboard)

        mChildEventListener = childEventListener

        FirebaseDatabase.getInstance().getReference("users")
                .addChildEventListener(mChildEventListener)

        rv_scoreboard.layoutManager = LinearLayoutManager(this)
        rv_scoreboard.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        mLastAdapter = LastAdapter(listOfUsers, BR.user)
                .map(User::class.java, R.layout.list_item_user)
                .into(rv_scoreboard)
    }

    private val childEventListener: ChildEventListener
        get() = object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String) {
                listOfUsers.add(dataSnapshot.getValue(User::class.java))
                Collections.sort(listOfUsers)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String) {
                val user = dataSnapshot.getValue(User::class.java)!!
                val i = listOfUsers.indexOf(user)
                if (listOfUsers[i].points != user.points) {
                    listOfUsers[i] = user
                    Collections.sort(listOfUsers)
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                val user = dataSnapshot.getValue(User::class.java)
                listOfUsers.removeAt(listOfUsers.indexOf(user))
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String) {
                toast("onChildMoved")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                toast("onCancelled")
            }
        }

    companion object {

        @BindingAdapter("android:src")
        fun setImageUri(view: ImageView, uri: String) {
            Glide.with(view.context)
                    .load(uri)
                    .apply(RequestOptions.circleCropTransform())
                    .apply(RequestOptions.errorOf(R.mipmap.geodrink_blue_logo))
                    .into(view)
        }
    }
}
