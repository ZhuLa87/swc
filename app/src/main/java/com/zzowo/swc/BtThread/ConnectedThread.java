package com.zzowo.swc.BtThread;


import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

public class ConnectedThread extends Thread {

    private static final String TAG = "CONNECTED_THREAD";
    private BluetoothSocket bluetoothSocket;
    private final InputStream inputStream; // 輸入串流
    private final OutputStream outputStream; // 輸出串流
    private byte[] buffer; // 緩衝區
    private int bytes; // 讀取的位元組數

    public ConnectedThread(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;

        // 建立暫時的輸入串流和輸出串流物件
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = this.bluetoothSocket.getInputStream();
            tmpOut = this.bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            try {
                this.bluetoothSocket.close();
            } catch (IOException ioException) {
                Log.e(TAG, "Could not close the client socket", ioException);
            }
        }
        inputStream = tmpIn;
        outputStream = tmpOut;
    }

    @Override
    public void run() {
        super.run();
        // TODO: 發送/接收數據

        // 初始化緩衝區
        buffer = new byte[1024];
        // 持續監聽InputStream
        while (true) {
            try {
                // 從輸入串流讀取數據
                bytes = inputStream.read(buffer);

                String receivedMessage = new String(buffer, 0, bytes);
                handleMessages(receivedMessage);

            } catch (IOException e) {
                // 發生錯誤或斷線，結束監聽
                Log.e(TAG, "Input stream was disconnected", e);
                Toast.makeText(null, "Input stream was disconnected", Toast.LENGTH_SHORT).show();
                break;
            }
        }
    }

    private void handleMessages(String message) {
        String[] parts = message.split(":");

        if (parts.length == 3) {
            String label = parts[0];
            String controlCode = parts[1];
            String content = parts[2];

            // Process the message based on the label, control code, and content
            // Add your logic here
            Log.d(TAG, "Received - Label: " + label + ", Control Code: " + controlCode + ", Content: " + content);

            handler.obtainMessage(0, message.length(), -1, message.getBytes()).sendToTarget();
        }
    }

    public void btWriteString(String stringData) {
        try {
            outputStream.write(stringData.getBytes());
        } catch (IOException e) {
            Log.e(TAG, "Error writing to OutputStream", e);
        }

//        for (byte sendData : stringData.getBytes()) {
//            try {
//                outputStream.write(sendData);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {
            byte[] buffer = (byte[]) message.obj;
            int bytes = message.arg1;
            String receiveMessage = new String(buffer, 0, bytes);
            Log.d(TAG, "Received: " + receiveMessage);
            return false;
        }
    });
}