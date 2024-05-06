package com.zzowo.swc;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class PushNotificationService extends FirebaseMessagingService {
    private static final String TAG = "PushNotificationService";
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Update server with new token
        Log.d(TAG, "Refreshed token: " + token);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // handle data payload
        Map<String, String> data =  remoteMessage.getData();
        String msgTag = data.get("tag");
        String msgTimestamp = data.get("timestamp");

        // handle notification
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // 檢查是否允許通知
        if (isNotificationEnabled()) {
            showNotification(remoteMessage.getNotification());
        }
    }

    private void showNotification(RemoteMessage.Notification message) {
        // Show notification
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "com.zzowo.swc";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0 or higher
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Notification", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("SWC Notification");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(R.color.colorPrimary);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);

        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(message.getTitle())
                .setContentText(message.getBody())
                .setContentInfo("Info");

        notificationManager.notify(1, notificationBuilder.build());
    }

    private boolean isNotificationEnabled() {
        SharedPreferences sp = getSharedPreferences("data", MODE_PRIVATE);
        boolean isNotificationEnabled = sp.getBoolean("switch_state_notification", true); // default: true
        return isNotificationEnabled;
    }
}
