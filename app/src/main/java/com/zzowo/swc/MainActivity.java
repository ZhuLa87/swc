package com.zzowo.swc;

import static com.zzowo.swc.BtThread.ConnectThread.bluetoothSocket;
import static com.zzowo.swc.ConnectWheelChairActivity.connectedThread;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zzowo.swc.databinding.ActivityMainBinding;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LocationThread.LocationListener, SettingsFragment.OnIdentitySelectedListener{
    private static final String TAG = "MainActivity";
    public static Handler BThandler;
    private FirebaseUser user;
    private FirebaseFirestore db;
    private SharedPreferences sp;
    private SharedPreferences.Editor editor;
    static public ActivityMainBinding binding;
    private static LocationThread locationThread;
    private Double lastLatitude = null;
    private Double lastLongitude = null;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Night mode is enable by default
        super.onCreate(savedInstanceState);

        // Initialize Firebase Firestore
        db = FirebaseFirestore.getInstance();

        initPreferences();
        initAuth();
        initFirebaseMessaging();
        updateFCMToken();
        bottomNavSetup();

        // check is selected identity
        Boolean isSharedPreferencesContainPrimaryUser = getUserIdentityFromSharedPreferences();
        if (!isSharedPreferencesContainPrimaryUser) {
            getUserIdentityFromFirestore();
        }

        // 開始位置線程
        startLocationThread();

        BThandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message message) {
                byte[] buffer = (byte[]) message.obj;
                int bytes = message.arg1;
                String receiveMessage = new String(buffer, 0, bytes);

                String[] parts = receiveMessage.split(":");

                if (parts.length == 3) {
                    String label = parts[0];
                    String controlCode = parts[1];
                    String content = parts[2];

                    // 根據標籤、控制代碼和內容處理訊息
                    Log.d(TAG, "handlerReceived - Label: " + label + ", Control Code: " + controlCode + ", Content: " + content);

                    if (label.equals("fullDown")) {
                        if (controlCode.equals("99")) {
                            if (content.equals("emergency")) {
                                sendAlert();
                            }
                        }
                    } else if (label.equals("beep")) {
                        if (controlCode.equals("0")) {
                            // TODO: 處理接收到目前蜂鳴器的狀態
                        }
                    }
                }

                return true;
            }
        });
    }

    public void sendAlert() {
        if (connectedThread != null) {
            connectedThread.btWriteString("alarm", "99", "Alert");
        } else {
            String provider = user.getProviderData().get(1).getProviderId();
            String userDisplayName = "";
            if (provider.contains("google.com")) {
                userDisplayName = user.getDisplayName();
            }

            Map<String, Object> notification = new HashMap<>();
            notification.put("from", user.getUid());
            notification.put("title", "緊急通知!");
            notification.put("body", userDisplayName.isEmpty()?"輪椅使用者":userDisplayName + "發出警報訊號，請立即前往查看。");
            notification.put("tag", "alert");
            notification.put("timestamp", new Timestamp(new Date()));

            db.collection("Notifications")
                    .add(notification)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Alert sent to server", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to send alert", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateFCMToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }
                        // Get new FCM registration token
                        String token = task.getResult();
                        Map<String, Object> tokenData = new HashMap<>();
                        tokenData.put("fcmToken", token);

                        db.collection("users").document(user.getUid())
                                .set(tokenData, SetOptions.merge())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG, "Successfully updated FCM token in Firestore");
                                        } else {
                                            Log.w(TAG, "Error updating FCM token in Firestore", task.getException());
                                        }
                                    }
                                });
                    }
                });
    }

    private void initFirebaseMessaging() {
        FirebaseMessaging firebaseMessaging = FirebaseMessaging.getInstance();
        firebaseMessaging.subscribeToTopic("all_notifications");
    }

    private Boolean getUserIdentityFromSharedPreferences() {
        Log.d(TAG, "Getting primaryUser from SharedPreferences");
        Boolean isContainPrimaryUser = sp.contains("primaryUser");
        return isContainPrimaryUser;
    }

    private void getUserIdentityFromFirestore() {
        Log.d(TAG, "Getting primaryUser from Firestore");
        db.collection("users").document(user.getUid())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Boolean primaryUser = task.getResult().getBoolean("primaryUser");
                        if (primaryUser != null) {
                            Log.d(TAG, "Successfully obtained primaryUser from Firestore: " + primaryUser);
                            // 如果已經選擇過身份，則將primaryUser寫入SharedPreferences
                            editor.putBoolean("primaryUser", primaryUser).commit();
                            return;
                        }
                    }
                    // 如果沒有選擇過身份，則會在首次登入時選擇
                    showIdentitySelectionDialog();
                });
    }

    private void initPreferences() {
        sp = this.getSharedPreferences("data", MODE_PRIVATE);
        editor = sp.edit();
    }

    private void initAuth() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LoginPage.class);
            startActivity(intent);
            finish();
        }

        // 'FirebaseAuth.getInstance().getCurrentUser().getProviderId()' always return 'firebase'
        // Solution: https://www.qiniu.com/qfans/qnso-49484003#comments
        String providerId = user.getProviderData().get(1).getProviderId();
        Log.d(TAG, "ProviderId: " + providerId);
        Log.d(TAG, "UserName: " + user.getDisplayName());
        Log.d(TAG, "UserEmail: " + user.getEmail());
        Log.d(TAG, "UserUID: " + user.getUid());
    }

    public void bottomNavSetup() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 預設首頁
        Boolean primaryUser = sp.getBoolean("primaryUser", true);
        if (primaryUser) {
            currentFragment = new HomeFragment();
            replaceFragment(currentFragment);
        } else {
            currentFragment = new MonitorMapFragment();
            replaceFragment(currentFragment);
            binding.bottomNavigationView.setSelectedItemId(R.id.bottom_map);
            binding.bottomNavigationView.getMenu().removeItem(R.id.bottom_home);
        }

        // 底部導覽列
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.bottom_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.bottom_map) {
                if (primaryUser) {
                    selectedFragment = new MapFragment();
                } else {
                    selectedFragment = new MonitorMapFragment();
                }
            } else if (itemId == R.id.bottom_binding) {
                selectedFragment = new BindingFragment();
            } else if (itemId == R.id.bottom_settings) {
                selectedFragment = new SettingsFragment();
            }

            // 如果選擇的頁面與當前頁面不同，則進行切換
            if (selectedFragment != null && !selectedFragment.getClass().equals(currentFragment.getClass())) {
                currentFragment = selectedFragment;
                replaceFragment(selectedFragment);
            }

            return true;
        });
    }

    private void showIdentitySelectionDialog() {
        Log.d(TAG, "showIdentitySelectionDialog");
        // 建立一個AlertDialog.Builder物件
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.select_identity);
        builder.setIcon(R.drawable.baseline_person_24);

        final CharSequence[] options = {getString(R.string.option_wheelchair_user), getString(R.string.option_caregiver)};

        builder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selectedOption = options[which].toString();

                handleUserSelection(selectedOption);

                // 成功判斷位於 storeUserIdentityInFirestore() 中
                dialog.dismiss();
            }
        });

        builder.setCancelable(false); // 不可取消對話框
        builder.show();
    }

    private void handleUserSelection(String selectedOption) {
        Boolean primaryUser = null;

        Log.d(TAG, "Selected: " + selectedOption);
        if (selectedOption.equals(getString(R.string.option_wheelchair_user))) {
            editor.putBoolean("primaryUser", true).commit();
            primaryUser = true;
        } else if (selectedOption.equals(getString(R.string.option_caregiver))) {
            editor.putBoolean("primaryUser", false).commit();
            primaryUser = false;

            // 切換至綁定頁面
            binding.bottomNavigationView.setSelectedItemId(R.id.bottom_binding);
            binding.bottomNavigationView.getMenu().removeItem(R.id.bottom_home);
        }
        storeUserIdentityInFirestore(primaryUser);
    }

    private void storeUserIdentityInFirestore(Boolean primaryUser) {
        Map<String, Object> data = new HashMap<>();
        data.put("primaryUser", primaryUser);
        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "primaryUser added with ID: " + user.getUid());

                            editor.putBoolean("isFirstTimeLogin", false).apply(); // 設定為非首次登入
                        } else {
                            Log.w(TAG, "Error adding document to Firestore", task.getException());

                            Toast.makeText(getApplicationContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                            showIdentitySelectionDialog();
                    }
                }})
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(), R.string.operation_failed_please_try_again, Toast.LENGTH_SHORT).show();
                        showIdentitySelectionDialog();
                    }
                });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void startLocationThread() {
        if (locationThread == null) {
            locationThread = new LocationThread(this, this);
            locationThread.start();
        }
    }

    public void onLocationSuccess(Double latitude, Double longitude) {

        // 如果經緯度與上次相同，則不更新
        if (latitude.equals(lastLatitude) && longitude.equals(lastLongitude)) {
            return;
        }

        lastLatitude = latitude;
        lastLongitude = longitude;

        Boolean allowStoreLocation = sp.getBoolean("allowStoreLocation", true);
        if (!allowStoreLocation) {
            return;
        }

        GeoPoint geoPoint = new GeoPoint(latitude, longitude); // Double latitude, Double longitude
        storeLastLocationInFirestore(geoPoint);
    }

    private void storeLastLocationInFirestore(GeoPoint geoPoint) {
        Boolean primaryUser = sp.getBoolean("primaryUser", true);
        if (!primaryUser) return;

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> lastLocation = new HashMap<>();
        lastLocation.put("point", geoPoint);
        lastLocation.put("timestamp", new Timestamp(new Date()));
        data.put("lastLocation", lastLocation);

        db.collection("users").document(user.getUid())
                .set(data, SetOptions.merge())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Successfully updated lastLocation in Firestore
                        Log.d(TAG, "Successfully updated lastLocation in Firestore");
                    } else {
                        Log.w(TAG, "Error updating lastLocation in Firestore", task.getException());
                    }
                });
    }

    @Override
    public void onIdentitySelected(Boolean primaryUser) {
        if (primaryUser) {
            bottomNavSetup();
        } else {
            bottomNavSetup();
            binding.bottomNavigationView.getMenu().removeItem(R.id.bottom_home);
        }
    }

    public Double getLastLatitude() {
        return lastLatitude;
    }

    public Double getLastLongitude() {
        return lastLongitude;
    }


    // 取消Handler註冊
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException ioException) {
                Log.e(TAG, "Could not close the client socket", ioException);
                bluetoothSocket = null;
            }
        }
        BThandler.removeCallbacksAndMessages(null);

        // 清理其他資源
        if (locationThread != null) {
            locationThread.stopThread();
        }
    }

}