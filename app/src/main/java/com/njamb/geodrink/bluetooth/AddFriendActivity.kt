package com.njamb.geodrink.bluetooth

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.njamb.geodrink.R

class AddFriendActivity : AppCompatActivity() {

    private var mAdapter: BluetoothAdapter? = null
    private var mBtService: BluetoothService? = null

    private var mPairedAdapter: ArrayAdapter<String>? = null
    private var mOtherAdapter: ArrayAdapter<String>? = null
    private var mPairingDevice: String? = null

    private var mUserId: String? = null
    private var mUsername: String? = null

    private var mProgressDialog: ProgressDialog? = null


    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_friend)

        mAdapter = BluetoothAdapter.getDefaultAdapter()
        if (mAdapter == null) {
            showToast("Device does not support bluetooth")
            finish()
        }

        // Get User ID & User's username
        mUserId = intent.extras.getString("userId")
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/username", mUserId))
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        mUsername = dataSnapshot.value as String?
                    }

                    override fun onCancelled(databaseError: DatabaseError) {}
                })

        // Button for starting discovery process
        val btnScan = findViewById(R.id.btn_scan) as Button
        btnScan.setOnClickListener {
            supportActionBar!!.setSubtitle(R.string.scanning)
            findDevices()
        }

        // Button for enabling discoverability
        val btnDiscover = findViewById(R.id.btn_discover) as Button
        btnDiscover.setOnClickListener {
            if (mAdapter!!.scanMode != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                startActivity(discoverableIntent)
            }
        }

        // ArrayAdapter & ListView for paired devices
        mPairedAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        val paired = findViewById(R.id.paired_devices) as ListView
        paired.adapter = mPairedAdapter
        paired.onItemClickListener = mListener

        // ArrayAdapter & ListView for other (non-paired) devices
        mOtherAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        val other = findViewById(R.id.other_devices) as ListView
        other.adapter = mOtherAdapter
        other.onItemClickListener = mListener

        // Register for broadcasts when a device is discovered
        var filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        this.registerReceiver(mReceiver, filter)
        // Register for broadcasts when discovery has finished
        filter = IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        this.registerReceiver(mReceiver, filter)
        // Register for broadcasts when a device is paired
        filter = IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        this.registerReceiver(mReceiver, filter)

        // "my" bluetooth service
        mBtService = BluetoothService(this, mHandler)
    }

    override fun onStart() {
        super.onStart()

        if (!mAdapter!!.isEnabled) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
        } else {
            if (mBtService != null) {
                mBtService!!.start()
            }
            getPairedDevices()
        }
    }

    override fun onStop() {
        super.onStop()

        if (mBtService != null) {
            mBtService!!.stop()
        }

        if (mAdapter != null) {
            if (mAdapter!!.isDiscovering) mAdapter!!.cancelDiscovery()
            if (mAdapter!!.isEnabled) mAdapter!!.disable()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        this.unregisterReceiver(mReceiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_ENABLE_BT -> if (resultCode == Activity.RESULT_OK) {
                getPairedDevices()
            } else {
                showToast("Bluetooth not enabled")
                finish()
            }
        }
    }

    private fun findDevices() {
        findViewById(R.id.text_other_devices).visibility = View.VISIBLE
        if (mAdapter!!.isDiscovering) mAdapter!!.cancelDiscovery()

        mOtherAdapter!!.clear()
        mAdapter!!.startDiscovery()
    }

    private fun getPairedDevices() {
        findViewById(R.id.text_paired_devices).visibility = View.VISIBLE
        val pairedDevices = mAdapter!!.bondedDevices

        mPairedAdapter!!.clear()
        if (pairedDevices.size > 0) {
            for (device in pairedDevices) {
                mPairedAdapter!!.add(device.name + "\n" + device.address)
            }
        } else {
            mPairedAdapter!!.add(getString(R.string.no_devices))
        }
    }

    private fun connect(item: String) {
        if (item == getString(R.string.no_devices)) return

        val address = item.substring(item.length - 17)
        val device = mAdapter!!.getRemoteDevice(address)

        mBtService!!.connect(device)

        mProgressDialog = ProgressDialog.show(this, "Connecting",
                                              "Trying to connect to " + device.name, true, false)
    }

    private fun displayAddFriendDialog(userId: String, username: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add friend")
                .setMessage(String.format("Do you want to make %s your friend?", username))
                .setNegativeButton("No") { dialog, which -> mBtService!!.write("no".toByteArray()) }
                .setPositiveButton("Yes") { dialog, which ->
                    mBtService!!.write(mUserId!!.toByteArray())
                    addFriend(userId)
                }
        val dialog = builder.create()
        dialog.show()
    }

    private fun addFriend(friendId: String) {
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/friends/%s", mUserId, friendId))
                .setValue(true)
        showToast("You are now friends")
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private val mListener = AdapterView.OnItemClickListener { parent, view, position, id ->
        if (mAdapter!!.isDiscovering) mAdapter!!.cancelDiscovery()

        val item = parent.getItemAtPosition(position) as String
        if (mOtherAdapter!!.getPosition(item) != -1) mPairingDevice = item
        connect(item)
    }

    private val mHandler = object : Handler() {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                Constants.MESSAGE_TOAST             -> {
                    showToast(msg.obj as String)
                }
                Constants.MESSAGE_USERID_USERNAME   -> {
                    /*
                    * First, one user is sending friend request in form 'userId:username'.
                    * After that, other user accepts request & sends reply with his userId,
                    * or refuses & sends "no".
                    */
                    val userIdUsername = (msg.obj as String).split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    val userId = userIdUsername[0]
                    if (userIdUsername.size == 2) {
                        displayAddFriendDialog(userId, userIdUsername[1])
                    } else {
                        if (userId != "no") {
                            addFriend(userId)
                        } else {
                            showToast("Friendship request declined")
                        }
                    }
                }
                Constants.MESSAGE_CONNECTED         -> {
                    mProgressDialog!!.dismiss()
                    showToast("Wait for user to respond")

                    // send string 'userId:username'
                    mBtService!!.write(String.format("%s:%s", mUserId, mUsername).toByteArray())
                }
                Constants.MESSAGE_CONNECTION_FAILED -> {
                    mProgressDialog!!.dismiss()
                    showToast("connection failed")
                }
            }
        }
    }

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val noDevices = resources.getText(R.string.no_devices).toString()

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Get the BluetoothDevice object from the Intent
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                // If it's already paired, skip it, because it's been listed already
                if (device.bondState != BluetoothDevice.BOND_BONDED) {
                    mOtherAdapter!!.add(device.name + "\n" + device.address)
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                supportActionBar!!.setSubtitle(R.string.finish_scanning)
                if (mOtherAdapter!!.count == 0) {
                    mOtherAdapter!!.add(noDevices)
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED == action) {
                val prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1)
                val currState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1)
                if (prevState == BluetoothDevice.BOND_BONDING && currState == BluetoothDevice.BOND_BONDED) {
                    if (mPairedAdapter!!.count == 1 && mPairedAdapter!!.getItem(0) == noDevices) {
                        mPairedAdapter!!.clear()
                    }
                    mPairedAdapter!!.add(mPairingDevice)
                    mOtherAdapter!!.remove(mPairingDevice)
                    if (mOtherAdapter!!.count == 0) {
                        findViewById(R.id.text_other_devices).visibility = View.GONE
                    }
                }
            }
        }
    }

    companion object {
        private val TAG = "AddFriendActivity"
        private val REQUEST_ENABLE_BT = 1
    }
}
