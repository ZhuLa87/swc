package com.zzowo.swc;

import static com.zzowo.swc.BtThread.ConnectThread.bluetoothSocket;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.os.Build;
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

import androidx.appcompat.app.AppCompatActivity;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.zzowo.swc.BtThread.ConnectThread;
import com.zzowo.swc.BtThread.ConnectedThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AddWheelChairActivity extends AppCompatActivity implements ConnectThread.ConnectionListener {
    private static final String TAG = "ADD_SWC";
    private static String deviceNameStart = "SWC";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothManager bluetoothManager;
    public BluetoothAdapter bluetoothAdapter;
    BluetoothDevice arduinoBTModule = null;
    private final static int ERROR_READ = 0; // used in bluetooth handler to identify message update

    // Android的SSP（協議棧默認）的UUID：00001101-0000-1000-8000-00805F9B34FB，只有使用該UUID才能正常和外部的SSP串口的藍牙設備去連接。
    public static UUID arduinoUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // We declare a default UUID to create the global variable

    ProgressBar progressBar;
    Button connectToDevice;
    ImageView bluetoothImg;
    TextView bluetoothStatus;

    // 自訂藍芽線程的初始化
    public static ConnectThread connectThread = null;
    public static ConnectedThread connectedThread = null;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wheel_chair);

        progressBar = findViewById(R.id.progressBar);
        connectToDevice = findViewById(R.id.connect_to_device);
        bluetoothImg = findViewById(R.id.bluetooth_img);
        bluetoothStatus = findViewById(R.id.bluetooth_status);

        // init
        initStatusBarColor();
        initBluetooth();
        initPermission();

        // 返回上一頁
        View backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener((view -> {
            // TODO: 返回操作
//            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
//                startConnectedThread();
//            }
            finish();
        }));

        connectToDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothStatus.setTextColor(getResources().getColor(R.color.colorOnBackground));

                checkBluetooth();
                arduinoBTModule = getPairedDevice();
                if (arduinoBTModule != null) {
                    Log.d(TAG, "BT device found");
                    // BT Found, enabling the button to read results

                    // 連接藍芽設備
                    connectToSWC();
                }
            }
        });

    }

    private void initStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorSurface));
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

    private void initPermission() {
        Log.d(TAG, "Checking permissions...");
        bluetoothStatus.setText(R.string.checking_permission);
        // 權限列表
        List<String> neededPermission = new ArrayList<>();

        // Check the Bluetooth version to decide which permissions to request
        // > https://developer.android.com/develop/connectivity/bluetooth/bt-permissions?hl=zh-tw
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            neededPermission.add(Manifest.permission.ACCESS_FINE_LOCATION);
            // Android 12 (API 31)及以上版本，需要請求掃描、廣播、連接權限
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
                            // 取得所有權限
                            Log.d(TAG, "Bluetooth permission granted");
                            bluetoothStatus.setText(R.string.bluetooth_permission_granted);

                            checkBluetooth();
                            arduinoBTModule = getPairedDevice();
                            if (arduinoBTModule != null) {
                                Log.d(TAG, "BT device found");
                                // BT Found, enabling the button to read results

                                // 連接藍芽設備
                                connectToSWC();
                            }
                        } else if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                            // 僅取得部分權限
                            Log.d(TAG, multiplePermissionsReport.getDeniedPermissionResponses().get(0).getPermissionName());
                            Toast.makeText(AddWheelChairActivity.this, "Missing permission", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                        // 拒絕權限
                        Toast.makeText(AddWheelChairActivity.this, "Missing permission", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                }).check();
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

    @SuppressLint("MissingPermission")
    private BluetoothDevice getPairedDevice() {
        Log.d(TAG, "Check paired Bluetooth devices");
        bluetoothStatus.setText(R.string.check_bluetooth_paired);
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null && pairedDevices.size() > 0) {
            for (BluetoothDevice device: pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(TAG, "deviceName: " + deviceName + " || " + deviceHardwareAddress); // 列出所有配對過的藍芽裝置
                if (deviceName.startsWith(deviceNameStart)) {
                    // 自動比對裝置名稱
                    Log.d(TAG, deviceName + " found");
                    arduinoUUID = device.getUuids()[0].getUuid();
                    Log.d(TAG, "UUID: " + arduinoUUID);
                    return device;
                }
            }
        } else {
            bluetoothStatus.setText(R.string.noPairedSWC);
//            Toast.makeText(this, R.string.noPairedSWC, Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    public void connectToSWC() {
        Log.d(TAG, "Try to connect to SWC...");
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

    public void onConnectionSuccess() {
        Toast.makeText(this, R.string.successfully_connected_to_swc, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "ConnectionSuccess");
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
            connectedThread = null;
        }

        // 開始新的連接
        connectedThread = new ConnectedThread(bluetoothSocket);
        connectedThread.start();
        Log.d(TAG, "ConnectedThread started");

        // 退出藍芽連接頁面
        finish();
    }
}