package com.zzowo.swc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class LocationThread extends Thread{
    private static final String TAG = "LOCATION_THREAD";
    private LocationListener locationListener;
    private Handler handler;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private Location lastKnownLocation;
    private Boolean isAllowLocationUpdates = true;

    public interface LocationListener {
        void onLocationSuccess(Double latitude, Double longitude);
    }

    public LocationThread(Context context, LocationListener listener) {
        this.locationListener = listener;
        handler = new Handler(Looper.getMainLooper());
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @Override
    public void run() {
        super.run();

        handler.postDelayed(new Runnable() {
            @SuppressLint("MissingPermission")
            @Override
            public void run() {
                if (isAllowLocationUpdates) {
                    getLocation();
                    handler.postDelayed(this, 10000); // 10秒重複
                }
            }
        }, 0);
    }

    @SuppressLint("MissingPermission")
    private void getLocation() {
        Task<Location> locationResult = fusedLocationProviderClient.getLastLocation();
        locationResult.addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
                if (task.isSuccessful()) {
                    lastKnownLocation = task.getResult();
                    if (lastKnownLocation == null) return;
                    double latitude = lastKnownLocation.getLatitude();
                    double longitude = lastKnownLocation.getLongitude();
                    Log.d(TAG, "Latitude: " + latitude + ", Longitude: " + longitude);
                    locationListener.onLocationSuccess(latitude, longitude);
                } else {
                    Log.e(TAG, "Failed to get location.");
                }
            }
        });
    }

    public Boolean toggleLocationUpdates() {
        isAllowLocationUpdates = !isAllowLocationUpdates;
        return isAllowLocationUpdates;
    }

    public void stopThread() {
        try {
            this.interrupt();
        } catch (Exception e) {
        }
    }
}
