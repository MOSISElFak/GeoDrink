package com.njamb.geodrink.Activities;

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
import com.njamb.geodrink.Classes.BluetoothService;
import com.njamb.geodrink.Classes.Constants;
import com.njamb.geodrink.R;

import java.util.Set;

public class AddFriendActivity extends AppCompatActivity {
    private static final String TAG = "AddFriendActivity";
    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mAdapter = null;
    private ArrayAdapter<String> pairedAdapter;
    private ArrayAdapter<String> otherAdapter;
    private String mUserId;
    private String mUsername;
    private ProgressDialog mProgressDialog;
    private BluetoothService mBtService = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_friend);

        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            Toast.makeText(this, "Device does not support bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
        mUserId = getIntent().getExtras().getString("userId");
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/username", mUserId))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        mUsername = (String) dataSnapshot.getValue();
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

        Button btnScan = (Button) findViewById(R.id.btn_scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSupportActionBar().setSubtitle(R.string.scanning);
                findDevices();
            }
        });

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

        pairedAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView paired = (ListView) findViewById(R.id.paired_devices);
        paired.setAdapter(pairedAdapter);
        paired.setOnItemClickListener(mListener);

        otherAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        ListView other = (ListView) findViewById(R.id.other_devices);
        other.setAdapter(otherAdapter);
        other.setOnItemClickListener(mListener);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        mBtService = new BluetoothService(this, mHandler);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            if (mBtService != null) mBtService.start();
            getPairedDevices();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mBtService != null) mBtService.stop();

        if (mAdapter != null && mAdapter.isDiscovering()) { // Kotlin - mAdapter?.isDiscovering()
            mAdapter.cancelDiscovery();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAdapter != null && mAdapter.isEnabled()) {
            mAdapter.disable();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK) {
                    getPairedDevices();
                } else {
                    Toast.makeText(this, "Bluetooth not enabled", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void findDevices() {
        findViewById(R.id.text_other_devices).setVisibility(View.VISIBLE);
        if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();

        otherAdapter.clear();
        mAdapter.startDiscovery();
    }

    private void getPairedDevices() {
        findViewById(R.id.text_paired_devices).setVisibility(View.VISIBLE);
        Set<BluetoothDevice> pairedDevices = mAdapter.getBondedDevices();
        pairedAdapter.clear();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                pairedAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        } else {
            pairedAdapter.add(getString(R.string.no_devices));
        }
    }

    private void connect(String item) {
        if (item.equals(getString(R.string.no_devices))) return;

        String address = item.substring(item.length() - 17);
        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        mBtService.connect(device);

        mProgressDialog = ProgressDialog.show(this, "Connecting",
                "Trying to connect to " + device.getName(), true, false,
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Toast.makeText(AddFriendActivity.this, "Cancel", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private AdapterView.OnItemClickListener mListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();

            String item = (String) parent.getItemAtPosition(position);
            connect(item);
        }
    };

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

    private void addFriend(String friendId) {
        FirebaseDatabase.getInstance()
                .getReference(String.format("users/%s/friends", mUserId))
                .setValue(friendId);
        Toast.makeText(this, "You are now friends", Toast.LENGTH_SHORT).show();
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_TOAST: {
                    Toast.makeText(AddFriendActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
                    break;
                }
                case Constants.MESSAGE_USERID_USERNAME: {
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
                            Toast.makeText(AddFriendActivity.this, "Friendship request declined",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                }
                case Constants.MESSAGE_CONNECTED: {
                    mProgressDialog.dismiss();
                    Toast.makeText(AddFriendActivity.this, "Wait for user to respond", Toast.LENGTH_SHORT).show();
                    // send string 'userId:username'
                    mBtService.write(String.format("%s:%s", mUserId, mUsername).getBytes());
                    break;
                }
                case Constants.MESSAGE_CONNECTION_FAILED: {
                    mProgressDialog.dismiss();
                    Toast.makeText(AddFriendActivity.this, "connection failed", Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // If it's already paired, skip it, because it's been listed already
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    otherAdapter.add(device.getName() + "\n" + device.getAddress());
                }
                // When discovery is finished, change the Activity title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                getSupportActionBar().setSubtitle(R.string.finish_scanning);
                if (otherAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.no_devices).toString();
                    otherAdapter.add(noDevices);
                }
            }
        }
    };
}
