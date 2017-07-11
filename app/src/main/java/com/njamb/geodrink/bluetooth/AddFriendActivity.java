package com.njamb.geodrink.bluetooth;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.njamb.geodrink.R;

import java.util.Set;

public class AddFriendActivity extends AppCompatActivity {
    private static final String TAG = "AddFriendActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mAdapter = null;
    private BluetoothService mBtService = null;

    private ArrayAdapter<String> mPairedAdapter;
    private ArrayAdapter<String> mOtherAdapter;
    private String mPairingDevice;

    private String mUserId;
    private String mUsername;

    private ProgressDialog mProgressDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            showToast("Device does not support bluetooth");
            finish();
        }

        // Get User ID & User's username
        mUserId = getIntent().getExtras().getString("userId");
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/username", mUserId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUsername = (String) dataSnapshot.getValue();
                    }
                    @Override public void onCancelled(DatabaseError databaseError) {}
                });

        // Button for starting discovery process
        Button btnScan = (Button) findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportActionBar().setSubtitle(R.string.scanning);
                findDevices();
            }
        });

        // Button for enabling discoverability
        Button btnDiscover = (Button) findViewById(R.id.btn_discover);
        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mAdapter.getScanMode() !=
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                    startActivity(discoverableIntent);
                }
            }
        });

        // ArrayAdapter & ListView for paired devices
        mPairedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView paired = (ListView) findViewById(R.id.paired_devices);
        paired.setAdapter(mPairedAdapter);
        paired.setOnItemClickListener(mListener);

        // ArrayAdapter & ListView for other (non-paired) devices
        mOtherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView other = (ListView) findViewById(R.id.other_devices);
        other.setAdapter(mOtherAdapter);
        other.setOnItemClickListener(mListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when a device is paired
        filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        this.registerReceiver(mReceiver, filter);

        // "my" bluetooth service
        mBtService = new BluetoothService(this, mHandler);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else {
            if (mBtService != null) {
                mBtService.start();
            }
            getPairedDevices();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBtService != null) {
            mBtService.stop();
        }

        if (mAdapter != null) {
            if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();
            if (mAdapter.isEnabled()) mAdapter.disable();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        this.unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    getPairedDevices();
                }
                else {
                    showToast("Bluetooth not enabled");
                    finish();
                }
                break;
        }
    }

    private void findDevices() {
        findViewById(R.id.text_other_devices).setVisibility(View.VISIBLE);
        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();

        mOtherAdapter.clear();
        mAdapter.startDiscovery();
    }

    private void getPairedDevices() {
        findViewById(R.id.text_paired_devices).setVisibility(View.VISIBLE);
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();

        mPairedAdapter.clear();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            mPairedAdapter.add(getString(R.string.no_devices));
        }
    }

    private void connect(String item) {
        if (item.equals(getString(R.string.no_devices))) return;

        String address = item.substring(item.length() - 17);
        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        mBtService.connect(device);

        mProgressDialog = ProgressDialog.show(this, "Connecting",
                "Trying to connect to " + device.getName(), true, false);
    }

    private void displayAddFriendDialog(final String userId, final String username) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add friend")
                .setMessage(String.format("Do you want to make %s your friend?", username))
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBtService.write("no".getBytes());
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mBtService.write(mUserId.getBytes());
                        addFriend(userId);
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addFriend(final String friendId) {
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/friends/%s", mUserId, friendId))
                .setValue(true);
        showToast("You are now friends");
        finish();
    }

    private void showToast(final String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private AdapterView.OnItemClickListener mListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();

            String item = (String) parent.getItemAtPosition(position);
            if (mOtherAdapter.getPosition(item) != -1) mPairingDevice = item;
            connect(item);
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_TOAST: {
                    showToast((String)msg.obj);
                    break;
                }
                case Constants.MESSAGE_USERID_USERNAME: {
                    /*
                    * First, one user is sending friend request in form 'userId:username'.
                    * After that, other user accepts request & sends reply with his userId,
                    * or refuses & sends "no".
                    */
                    String[] userIdUsername = ((String)msg.obj).split(":");
                    String userId = userIdUsername[0];
                    if (userIdUsername.length == 2) {
                        displayAddFriendDialog(userId, userIdUsername[1]);
                    }
                    else {
                        if (!userId.equals("no")) {
                            addFriend(userId);
                        }
                        else {
                            showToast("Friendship request declined");
                        }
                    }
                    break;
                }
                case Constants.MESSAGE_CONNECTED: {
                    mProgressDialog.dismiss();
                    showToast("Wait for user to respond");

                    // send string 'userId:username'
                    mBtService.write(String.format("%s:%s", mUserId, mUsername).getBytes());
                    break;
                }
                case Constants.MESSAGE_CONNECTION_FAILED: {
                    mProgressDialog.dismiss();
                    showToast("connection failed");
                }
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String noDevices = getResources().getText(R.string.no_devices).toString();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mOtherAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                getSupportActionBar().setSubtitle(R.string.finish_scanning);
                if (mOtherAdapter.getCount() == 0) {
                    mOtherAdapter.add(noDevices);
                }
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                int prevState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, -1);
                int currState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, -1);
                if (prevState == BluetoothDevice.BOND_BONDING
                        && currState == BluetoothDevice.BOND_BONDED) {
                    if (mPairedAdapter.getCount() == 1
                            && mPairedAdapter.getItem(0).equals(noDevices)) {
                        mPairedAdapter.clear();
                    }
                    mPairedAdapter.add(mPairingDevice);
                    mOtherAdapter.remove(mPairingDevice);
                    if (mOtherAdapter.getCount() == 0) {
                        findViewById(R.id.text_other_devices).setVisibility(View.GONE);
                    }
                }
            }
        }
    };
}
