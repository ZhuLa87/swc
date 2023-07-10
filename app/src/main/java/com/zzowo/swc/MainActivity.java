package com.zzowo.swc;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.zzowo.swc.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    FirebaseAuth mAuth;
    FirebaseUser user;

    private ActivityMainBinding binding;
    private NotificationManager notificationManager;
    private Notification notification;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); // Night mode is enable by default
        super.onCreate(savedInstanceState);

//        init
        autoAuth();
        bottomNavSetup();
        notificationSetup(); // 通知管理
    }

    private void autoAuth() {
        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent(getApplicationContext(), LoginPage.class);
            startActivity(intent);
            finish();
        } else {
            // 'FirebaseAuth.getInstance().getCurrentUser().getProviderId()' always return 'firebase'
            // Solution: https://www.qiniu.com/qfans/qnso-49484003#comments
            String providerId = user.getProviderData().get(1).getProviderId();
            Log.d("zhu", "ProviderId: " + providerId);
            Log.d("zhu", "UserName: " + user.getDisplayName());
            Log.d("zhu", "UserEmail: " + user.getEmail());
            Log.d("zhu", "UserUID: " + user.getUid());
        }
    }

    private void bottomNavSetup() {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomeFragment());

//        底部導覽列
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.bottom_home) {
                replaceFragment(new HomeFragment());
            } else if (itemId == R.id.bottom_map) {
                replaceFragment(new MapFragment());
            } else if (itemId == R.id.bottom_notifications) {
                replaceFragment(new NotificationsFragment());
            } else if (itemId == R.id.bottom_settings) {
                replaceFragment(new SettingsFragment());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void notificationSetup() {
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        NotificationChannel notificationChannel = new NotificationChannel("alert", "通知LaGan", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(notificationChannel);

        Intent intent = new  Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        int color = ContextCompat.getColor(this, R.color.green_300);
        notification = new NotificationCompat.Builder(this, "alert")
                .setContentTitle("早安!")
                .setContentText("世界那麼大, 想去走走嗎")
                .setSmallIcon(R.drawable.wheelchair_100)
                .setColor(color)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
    }

    public void sendNotification(View view) {
        notificationManager.notify(0, notification);
    }

    public void cancelNotification(View view) {
        notificationManager.cancel(0);
    }
}