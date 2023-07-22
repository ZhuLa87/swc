package com.zzowo.swc;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.DexterBuilder;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

public class AddWheelChairActivity extends AppCompatActivity {
    private final static String TAG = "AddSWC";
    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wheel_chair);

        // init
        initStatusBarColor();
        initBluetooth();
        initPermission();

        View backBtn = findViewById(R.id.btn_back);
        backBtn.setOnClickListener((view -> {
            finish();
        }));
    }

    private void initStatusBarColor() {
        Window window = this.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(this.getResources().getColor(R.color.colorSurface));
    }

    private void initBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
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