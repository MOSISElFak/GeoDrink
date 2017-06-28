package com.njamb.geodrink.Activities;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.njamb.geodrink.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class AddFriendActivity extends AppCompatActivity {
    private static final String TAG = "AddFriendActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID MY_UUID = UUID.fromString("bf6f67ba-910c-4e82-b35b-621887fcbf0c");

    private BluetoothAdapter mAdapter = null;
    private ArrayAdapter<String> pairedAdapter;
    private ArrayAdapter<String> otherAdapter;
    private String mUserId;

    private ConnectThread mConnectThread = null;

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

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);
        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!mAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        } else {
            getPairedDevices();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mAdapter != null && mAdapter.isDiscovering()) { // Kotlin - mAdapter?.isDiscovering()
            mAdapter.cancelDiscovery();
        }

        if (mAdapter != null && mAdapter.isEnabled()) {
            mAdapter.disable();
        }

        if (mConnectThread != null) {
            mConnectThread.cancel();
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
        Toast.makeText(this, "connect to " + address, Toast.LENGTH_SHORT).show();
        BluetoothDevice device = mAdapter.getRemoteDevice(address);

        mConnectThread = new ConnectThread(this, mHandler, device, mUserId);
        mConnectThread.start();
    }

    private AdapterView.OnItemClickListener mListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (mAdapter.isDiscovering()) mAdapter.cancelDiscovery();

            String item = (String) parent.getItemAtPosition(position);
            connect(item);
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(AddFriendActivity.this, (String) msg.obj, Toast.LENGTH_SHORT).show();
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


    private class ConnectThread extends Thread {
        Handler mHandler;
        BluetoothDevice mDevice;
        String mMsg;
        BluetoothSocket mSocket = null;

        public ConnectThread(Context context, Handler handler,
                             BluetoothDevice device, String msg) {
            mHandler = handler;
            mDevice = device;
            mMsg = msg;
        }

        @Override
        public void run() {
            InputStream is;
            OutputStream os;
            try {
                mSocket = mDevice.createRfcommSocketToServiceRecord(MY_UUID);
                sendMessage("mSocket created");
            } catch (IOException e) {
                sendMessage("mSocket not created");
                e.printStackTrace();
            }
            try {
                if (mSocket != null) {
                    mSocket.connect();

                    sendMessage("connected");

                    try {
                        // TODO: won't connect with other device
                        // TODO: implement friendship
                        is = mSocket.getInputStream();
                        os = mSocket.getOutputStream();

                        os.write(mMsg.getBytes());
                        sendMessage("Message sent");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    sendMessage("mSocket null");
                }
            } catch (IOException e) {
                sendMessage("not connected: " + e.getMessage());
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                mSocket.close();
                sendMessage("mSocket closed");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMessage(String s) {
            Message msg = new Message();
            msg.obj = s;
            mHandler.sendMessage(msg);
        }
    }
}
