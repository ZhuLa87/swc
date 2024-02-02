package com.zzowo.swc.BtThread;

import static com.zzowo.swc.AddWheelChairActivity.arduinoUUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.IOException;


// 連接藍芽設備的操作線程類
public class ConnectThread extends Thread {
    private static final String TAG = "CONNECT_THREAD";
    private BluetoothDevice bluetoothDevice;
    public static BluetoothSocket bluetoothSocket;
    private Handler handler;
    private ConnectionListener connectionListener;

    public interface ConnectionListener  {
        void onConnectionSuccess();
        void onConnectionError(String errorMessage);
    }

    public ConnectThread(BluetoothDevice bluetoothDevice, ConnectionListener listener) {
        this.bluetoothDevice = bluetoothDevice;
        this.connectionListener = listener;
        this.handler = new Handler(Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    @Override
    public void run() {
        super.run();
        try {
            bluetoothSocket = this.bluetoothDevice.createRfcommSocketToServiceRecord(arduinoUUID); // 使用 RFCOMM 協定(藍牙串口通信的協定)建立連結
            bluetoothSocket.connect(); // 建立與SWC的連結

            // 連結成功，在主線程中通知回調
            handler.post(new Runnable() {
                @Override
                public void run() {
                    connectionListener.onConnectionSuccess();
                }
            });
        } catch (IOException e) {
            // 錯誤，通知回掉
            Log.d(TAG, "Could not connect to client socket", e);
            handler.post(new Runnable() {
                @Override
                public void run() {
                    connectionListener.onConnectionError(e.getMessage());
                }
            });

            // 嘗試段開連結
            try {
                bluetoothSocket.close();
            } catch (IOException ioException) {
                Log.e(TAG, "Could not close the client socket", ioException);
            }
        }
    }

    // 斷開連結
    public void cancel() {
        if (bluetoothSocket != null)
            try {
                bluetoothSocket.close();
            } catch (IOException ioException) {
                Log.e(TAG, "Could not close the client socket", ioException);
                bluetoothSocket = null;
            }
    }


}

