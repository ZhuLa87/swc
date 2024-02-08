package com.zzowo.swc.BtThread;

import static com.zzowo.swc.MainActivity.BThandler;
import static com.zzowo.swc.ConnectWheelChairActivity.connectedThread;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
                // 檢查若連線存在則處裡數據
                if (bluetoothSocket.isConnected()) {
                    // 從輸入串流讀取數據
                    bytes = inputStream.read(buffer);

                    String receivedMessage = new String(buffer, 0, bytes);
                    handleMessages(receivedMessage);
                }
            } catch (IOException e) {
                // 發生錯誤或斷線，結束監聽
                Log.e(TAG, "Input stream was disconnected", e);
                cancel();
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

            // TODO: 根據標籤、控制代碼和內容處理訊息
//            Log.d(TAG, "BT Received - Label: " + label + ", Control Code: " + controlCode + ", Content: " + content);

            // 通知主執行續的Handler，可將接收的字串用於UI更新
            BThandler.obtainMessage(0, message.length(), -1, message.getBytes()).sendToTarget();
        }
    }

    public void btWriteString(String label, String controlCode, String content) {
        if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
            String stringData = label + ":" + controlCode + ":" + content; //  + "\n"
            try {
                outputStream.write(stringData.getBytes());
                Log.d(TAG, "btWriteString: " + stringData);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to OutputStream", e);
            }
        }
    }

    public void cancel() {
        try {
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing sockets", e);
        } finally {
            // 將數據傳輸執行緒設為空
            connectedThread = null;
        }
    }
}