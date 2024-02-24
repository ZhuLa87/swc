package com.zzowo.swc;

import static com.zzowo.swc.BtThread.ConnectThread.bluetoothSocket;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.zzowo.swc.BtThread.ConnectThread;
import com.zzowo.swc.BtThread.ConnectedThread;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ConnectWheelChairActivity extends AppCompatActivity implements ConnectThread.ConnectionListener {
    private static final String TAG = "ConnectWheelChairActivity";
    private static String deviceNameStart = "SWC";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice arduinoBTModule = null;
    private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update

    // Android的SSP（協議棧默認）的UUID：00001101-0000-1000-8000-00805F9B34FB，只有使用該UUID才能正常和外部的SSP串口的藍牙設備去連接。
    public static UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // We declare a default UUID to create the global variable

    ProgressBar progressBar;
    Button connectToDevice;
    ImageView bluetoothImg;
    TextView bluetoothStatus;

    // 自訂藍芽線程的初始化
    public static ConnectThread connectThread;
    public static ConnectedThread connectedThread;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wheel_chair);

        progressBar = findViewById(R.id.progressBar);
        connectToDevice = findViewById(R.id.connect_to_device);
        bluetoothImg = findViewById(R.id.bluetooth_img);
        bluetoothStatus = findViewById(R.id.bluetooth_status);
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseFirestore.getInstance();

        // init
        initStatusBarColor();
        initPreferences();
        initBluetooth();

        // 檢查藍芽狀態
        checkBluetooth();

        // 取得已配對的藍芽裝置
        getPairedDevice();
//        connectToSWC();



        // 返回上一頁
        View backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener(view -> {
            finish();
        });

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothStatus.setTextColor(getResources().getColor(R.color.colorOnBackground));

                checkBluetooth();
                getPairedDevice();
//                connectToSWC();
            }
        });

    }

    private void initStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorSurface));
    }

    private void initPreferences() {
        sp = this.getSharedPreferences("wheelchair", MODE_PRIVATE);
        editor = sp.edit();
    }

    @SuppressLint("MissingPermission")
    private void initBluetooth() {
        Log.d(TAG, "Check Bluetooth support...");
        bluetoothStatus.setText(R.string.check_bluetooth_support);
        //  Bluetooth LE
//        bluetoothManager = getSystemService(BluetoothManager.class);
//        bluetoothAdapter = bluetoothManager.getAdapter();
        // Bluetooth Classic
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 檢查裝置是否支援藍芽
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Your device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @SuppressLint("MissingPermission")
    private void checkBluetooth() {
        Log.d(TAG, "Check bluetooth status");
        bluetoothStatus.setText(R.string.check_bluetooth_enable);

        // 開啟藍芽
        if (!bluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BLUETOOTH);
        }
        Log.d(TAG, "Bluetooth check passed");
    }

    // 這段處理異部取得資料有點亂
    private interface FirestoreCallback {
        void onFirestoreDataLoaded(BluetoothDevice device);
    }

    private void getDeviceFromFirestore(FirestoreCallback callback) {
        DocumentReference docRef = db.collection("users").document(user.getUid());
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Error getting documents.", task.getException());
                    callback.onFirestoreDataLoaded(null);
                    return;
                }
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    Map<String, Object> userData = document.getData();
                    if (userData != null && userData.containsKey("wheelchair")) {
                        Map<String, Object> wheelchair = (Map<String, Object>) userData.get("wheelchair");
                        if (wheelchair != null) {
                            Boolean isBound = (Boolean) wheelchair.get("isBound");
                            String deviceHardwareAddress = (String) wheelchair.get("deviceHardwareAddress");
                            if (isBound != null && isBound && deviceHardwareAddress != null) {
                                Log.d(TAG, "Get deviceHardwareAddress from Firestore: " + deviceHardwareAddress);
                                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceHardwareAddress);
                                if (device != null) {
                                    Log.d(TAG, "Device found in Firestore");
                                    arduinoUUID = device.getUuids()[0].getUuid();
                                    Log.d(TAG, "UUID: " + arduinoUUID);
                                    arduinoBTModule = device;
                                    callback.onFirestoreDataLoaded(device);
                                    return;
                                }
                            }
                        }
                    }
                    Log.d(TAG, "Data in the correct format not found in Firestore document");
                }
                callback.onFirestoreDataLoaded(null);
            }
        });
    }

    @SuppressLint("MissingPermission")
    private void getPairedDevice() {
        // 檢查儲存紀錄中已配對的藍芽裝置
        Boolean isBound = sp.getBoolean("isBound", false);
        if (isBound) {
            String deviceHardwareAddress = sp.getString("deviceHardwareAddress", null);
            if (deviceHardwareAddress != null) {
                Log.d(TAG, "Get deviceHardwareAddress from sharedPreferences: " + deviceHardwareAddress);
                BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceHardwareAddress);
                if (device != null) {
                    Log.d(TAG, "Device found in SharedPreferences");
                    arduinoUUID = device.getUuids()[0].getUuid();
                    Log.d(TAG, "UUID: " + arduinoUUID);
                    arduinoBTModule = device;
                    connectToSWC();
                    return;
                }
            }
        }

        // 檢查線上紀錄中已配對的藍芽裝置
        getDeviceFromFirestore(new FirestoreCallback() {
            @Override
            public void onFirestoreDataLoaded(BluetoothDevice device) {
                if (device != null) {
                    // 找到已配對裝置
                    connectToSWC();
                } else {
                    // 沒有找到已配對裝置
                    getDeviceFromBluetooth();
                    connectToSWC();
                }
            }
        });

    }

    @SuppressLint("MissingPermission")
    private void getDeviceFromBluetooth() {
        Log.d(TAG, "Check paired Bluetooth devices");
        bluetoothStatus.setText(R.string.check_bluetooth_paired);
        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device: pairedDevices) {
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "deviceName: " + deviceName + " || " + deviceHardwareAddress); // 列出所有配對過的藍芽裝置

                // 自動比對裝置名稱
                if (deviceName.startsWith(deviceNameStart)) {
                    Log.d(TAG, deviceName + " found in paired devices");
                    arduinoUUID = device.getUuids()[0].getUuid();
                    Log.d(TAG, "UUID: " + arduinoUUID);
                    arduinoBTModule = device;
                }
            }
        } else {
            bluetoothStatus.setText(R.string.noPairedSWC);
        }
    }

    public void connectToSWC() {
        if (arduinoBTModule != null) {
            Log.d(TAG, "connectToSWC: BT device found");

            Log.d(TAG, "connectToSWC: Try to connect to SWC...");
            bluetoothStatus.setText(R.string.connecting_to_swc);
            // 判斷是否已經有連接，只能存在一個連接
            if (connectThread != null) {
                // 如果已經有連接，則段開該連接
                connectThread.cancel();
                connectThread = null;
            }

            // 開始新的連接
            connectThread = new ConnectThread(arduinoBTModule, this);
            connectThread.start();
        }
    }

    public void onConnectionSuccess(String deviceHardwareAddress) {
        Toast.makeText(this, R.string.successfully_connected_to_swc, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "ConnectionSuccess");

        // 連接成功，儲存連接裝置的MAC地址
        editor.putBoolean("isBound", true);
        editor.putString("deviceHardwareAddress", deviceHardwareAddress);
        editor.apply();

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> wheelchair = new HashMap<>();
        wheelchair.put("isBound", true);
        wheelchair.put("deviceHardwareAddress", deviceHardwareAddress);
        data.put("wheelchair", wheelchair);

        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Bound info added with ID: " + user.getUid());
                    } else {
                        Log.w(TAG, "Error adding document to Firestore", task.getException());
                    }
                })
                .addOnFailureListener(e -> {
                    // 連接Firestore失敗，顯示錯誤訊息
                    Log.w(TAG, "Error adding document to Firestore", e);
                });


        // 連接成功，隱藏進度條
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
//                bluetoothImg.setImageResource(R.drawable.ic_baseline_bluetooth_connected_24);
            }
        });

        // 連接成功，開始藍芽數據線程
        startConnectedThread();
    }

    public void onConnectionError(String errorMessage) {
        Log.d(TAG, "ConnectionError: " + errorMessage);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                bluetoothStatus.setText(R.string.auto_connect_failed);
                bluetoothStatus.setTextColor(getResources().getColor(R.color.colorError));
                progressBar.setVisibility(View.GONE);
//                bluetoothImg.setImageResource(R.drawable.ic_baseline_bluetooth_disabled_24);
                connectToDevice.setEnabled(true);
            }
        });
    }

    private void startConnectedThread() {
        // 判斷是否已經有連接，只能存在一個連接
        if (connectedThread != null) {
            connectedThread.cancel();
        }

        // 開始新的連接
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        Log.d(TAG, "ConnectedThread started");

        // 退出藍芽連接頁面
        finish();
    }
}