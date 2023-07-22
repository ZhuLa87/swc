package com.zzowo.swc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AddWheelChairActivity extends AppCompatActivity {
    private static final String TAG = "ADD_SWC";
    private static String deviceNameStart = "SWC";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_ENABLE_BT = 500;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    BluetoothDevice arduinoBTModule = null;
    // We will use a Handler to get the BT Connection status
    public static Handler handler;
    private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update

    // Android的SSP（協議棧默認）的UUID：00001101-0000-1000-8000-00805F9B34FB，只有使用該UUID才能正常和外部的，也是SSP串口的藍牙設備去連接。
    UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // We declare a default UUID to create the global variable
    Button connectToDevice;
    ImageView bluetoothImg;
    TextView myDevices;
    TextView btReadings;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wheel_chair);

        progressBar = findViewById(R.id.progressBar);
        connectToDevice = findViewById(R.id.connect_to_device);
        bluetoothImg = findViewById(R.id.bluetooth_img);
        myDevices = findViewById(R.id.my_devices);
        btReadings = findViewById(R.id.btReadings);

        // init
        initStatusBarColor();
        initBluetooth();
        initPermission();

        showHandler();

        // Create an Observable from RxAndroid
        //The code will be executed when an Observer subscribes to the the Observable
        final Observable<String> connectToBTObservable = Observable.create(emitter -> {
            Log.d(TAG, "Calling connectThread class");
            //Call the constructor of the ConnectThread class
            //Passing the Arguments: an Object that represents the BT device,
            // the UUID and then the handler to update the UI
            ConnectThread connectThread = new ConnectThread(arduinoBTModule, arduinoUUID, handler);
            connectThread.run();
            //Check if Socket connected
            if (connectThread.getMmSocket().isConnected()) {
                Log.d(TAG, "Calling ConnectedThread class");
                //The pass the Open socket as arguments to call the constructor of ConnectedThread
                ConnectedThread connectedThread = new ConnectedThread(connectThread.getMmSocket());
                connectedThread.run();
                if(connectedThread.getValueRead()!=null)
                {
                    // If we have read a value from the Arduino
                    // we call the onNext() function
                    //This value will be observed by the observer
                    emitter.onNext(connectedThread.getValueRead());
                }
                //We just want to stream 1 value, so we close the BT stream
                connectedThread.cancel();
            }
            // SystemClock.sleep(5000); // simulate delay
            //Then we close the socket connection
            connectThread.cancel();
            //We could Override the onComplete function
            emitter.onComplete();
        });

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                progressBar.setVisibility(View.VISIBLE);
                btReadings.setText("");
                if (arduinoBTModule != null) {
                    //We subscribe to the observable until the onComplete() is called
                    //We also define control the thread management with
                    // subscribeOn:  the thread in which you want to execute the action
                    // observeOn: the thread in which you want to get the response
                    connectToBTObservable.
                            observeOn(AndroidSchedulers.mainThread()).
                            subscribeOn(Schedulers.io()).
                            subscribe(valueRead -> {
                                //valueRead returned by the onNext() from the Observable
                                btReadings.setText(valueRead);
                                //We just scratched the surface with RxAndroid
                            });
                }
            }
        });

        bluetoothImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Check if the phone supports BT
                if (bluetoothAdapter == null) {
                    // Device doesn't support Bluetooth
                    Log.d(TAG, "Device doesn't support Bluetooth");
                } else {
                    Log.d(TAG, "Device support Bluetooth");
                    //Check BT enabled. If disabled, we ask the user to enable BT
                    if (!bluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "Bluetooth is disabled");
                        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            Log.d(TAG, "We don't BT Permissions");
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                            Log.d(TAG, "Bluetooth is enabled now");
                        } else {
                            Log.d(TAG, "We have BT Permissions");
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                            Log.d(TAG, "Bluetooth is enabled now");
                        }

                    } else {
                        Log.d(TAG, "Bluetooth is enabled");
                    }
                    String btDevicesString="";
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

                    if (pairedDevices.size() > 0) {
                        // There are paired devices. Get the name and address of each paired device.
                        for (BluetoothDevice device: pairedDevices) {
                            String deviceName = device.getName();
                            String deviceHardwareAddress = device.getAddress(); // MAC address
                            Log.d(TAG, "deviceName:" + deviceName);
                            Log.d(TAG, "deviceHardwareAddress:" + deviceHardwareAddress);
                            //We append all devices to a String that we will display in the UI
                            btDevicesString=btDevicesString+deviceName+" || "+deviceHardwareAddress+"\n";
                            //If we find the HC 05 device (the Arduino BT module)
                            //We assign the device value to the Global variable BluetoothDevice
                            //We enable the button "Connect to HC 05 device"
                            if (deviceName.startsWith(deviceNameStart)) {
                                Log.d(TAG, deviceName + " found");
                                arduinoUUID = device.getUuids()[0].getUuid();
                                arduinoBTModule = device;
                                //HC -05 Found, enabling the button to read results
                                connectToDevice.setEnabled(true);
                            }
//                            if (deviceName.equals(swcDeviceName)) {
//                                Log.d(TAG, swcDeviceName + " found");
//                                arduinoUUID = device.getUuids()[0].getUuid();
//                                arduinoBTModule = device;
//                                //HC -05 Found, enabling the button to read results
//                                connectToDevice.setEnabled(true);
//                            }
                            myDevices.setText(btDevicesString);
                        }
                    }
                }
            }
        });

        View backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener((view -> {
            finish();
        }));
    }

    private void showHandler() {
        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case ERROR_READ:
//                        progressBar.setVisibility(View.GONE);
                        String arduinoMsg = msg.obj.toString(); // Read message from Arduino
                        btReadings.setText(arduinoMsg);
                        break;
                }
            }
        };
    }

    private void initStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorSurface));
    }

    @SuppressLint("MissingPermission")
    private void initBluetooth() {
        bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initPermission() {
        List<String> neededPermission = new ArrayList<>();
        // Check the Bluetooth version to decide which permissions to request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            neededPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
            // Android 10 (API 29)及以上版本，需要請求掃描、廣播、連接權限
            neededPermission.add(Manifest.permission.BLUETOOTH_SCAN);
            neededPermission.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            neededPermission.add(Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            neededPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
            // 其他版本需要請求藍芽、管理藍芽權限
            neededPermission.add(Manifest.permission.BLUETOOTH);
            neededPermission.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        Dexter.withContext(this)
                .withPermissions(
                        neededPermission
                ).withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted()) {
                            Log.d(TAG, "Bluetooth permission granted");
                        } else if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            Log.d(TAG, multiplePermissionsReport.getDeniedPermissionResponses().get(0).getPermissionName());
                            Toast.makeText(AddWheelChairActivity.this, "Missing permission", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        Toast.makeText(AddWheelChairActivity.this, "Missing permission", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).check();
    }
}