package com.zzowo.swc;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.ArrayList;
import java.util.List;

public class requestPermissionActivity extends AppCompatActivity {
    private static final String TAG = "requestPermissionActivity";
    ProgressBar progressBar;
    List<String> neededPermission = new ArrayList<>();
    Button next_btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permission);

        // Permission list

        // Check the Bluetooth version to decide which permissions to request
        // > https://developer.android.com/develop/connectivity/bluetooth/bt-permissions?hl=zh-tw
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 (API 31)及以上版本，需要請求掃描、廣播、連接權限
            neededPermission.add(android.Manifest.permission.BLUETOOTH_SCAN);
            neededPermission.add(android.Manifest.permission.BLUETOOTH_ADVERTISE);
            neededPermission.add(android.Manifest.permission.BLUETOOTH_CONNECT);
        } else {
            // 其他版本需要請求藍芽、管理藍芽權限
            neededPermission.add(android.Manifest.permission.BLUETOOTH);
            neededPermission.add(Manifest.permission.BLUETOOTH_ADMIN);
        }

        // 請求位置權限
        neededPermission.add(android.Manifest.permission.ACCESS_FINE_LOCATION);

        // 請求通知權限
        neededPermission.add(android.Manifest.permission.POST_NOTIFICATIONS);

        checkPermission();

        progressBar = findViewById(R.id.progressBar);
        next_btn = findViewById(R.id.next_btn);

        next_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progressBar.setVisibility(View.VISIBLE);
                checkPermission();
                grantPermission();
            }
        });
    }

    private void checkPermission() {

        for (String permission : neededPermission) {
            int permissionCheck = this.checkSelfPermission(permission);
            if (permissionCheck == -1) {
                // 沒有權限
                return;
            }
        }

        Log.d(TAG, "All permissions are granted!");
        Intent intent = new Intent(requestPermissionActivity.this, LoginPage.class);
        startActivity(intent);
        finish();
    }

    private void grantPermission() {
        Log.d(TAG, "grantPermission: " + neededPermission.toString());
        // 請求權限
        Dexter.withContext(this)
            .withPermissions(
                    neededPermission
            ).withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport multiplePermissionsReport) {
                    if (multiplePermissionsReport.areAllPermissionsGranted()) {
                        // 取得所有權限
                        progressBar.setVisibility(View.GONE);

                        Intent intent = new Intent(requestPermissionActivity.this, LoginPage.class);
                        startActivity(intent);
                        finish();
                    } else if (multiplePermissionsReport.isAnyPermissionPermanentlyDenied()) {
                        // 僅取得部分權限
                        progressBar.setVisibility(View.GONE);
//                        Log.d(TAG, multiplePermissionsReport.getDeniedPermissionResponses().get(0).getPermissionName());
                        showSettingsDialog();
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> list, PermissionToken permissionToken) {
                    // 拒絕權限
                    progressBar.setVisibility(View.GONE);
                    Log.d(TAG, "Permission denied");
                    showSettingsDialog();
                }
            }).check();
    }

    private void showSettingsDialog() {
        // we are displaying an alert dialog for permissions
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // below line is the title for our alert dialog.
        builder.setTitle("Need Permissions");

        // below line is our message for our dialog
        builder.setMessage("This app needs all permissions to work properly. You can grant them in app settings.");
        builder.setPositiveButton("SETTINGS", (dialog, which) -> {
            // this method is called on click on positive button and on clicking shit button
            // we are redirecting our user from our app to the settings page of our app.
            dialog.cancel();
            // below is the intent from which we are redirecting our user.
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", this.getPackageName(), null);
            intent.setData(uri);
            startActivityForResult(intent, 101);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            // this method is called when user click on negative button.
            dialog.cancel();
        });
        // below line is used to display our dialog
        builder.show();
    }
}