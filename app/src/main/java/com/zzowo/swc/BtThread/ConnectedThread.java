package com.zzowo.swc.BtThread;


import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//Class that given an open BT Socket will
//Open, manage and close the data Stream from the Arduino BT device
public class ConnectedThread extends Thread {

    private static final String TAG = "CONNECTED_THREAD";
    BluetoothSocket bluetoothSocket;
    InputStream inputStream; // 輸入串流
    OutputStream outputStream; // 輸出串流
//    int[] lastData = new int[] {0, 0};

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
    }

    public void btWriteString(String stringData) {
        for (byte sendData: stringData.getBytes()) {
            try {
                outputStream.write(sendData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

//        try {
//            outputStream.write(stringData.getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void cancel() {
        try {
            bluetoothSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the client socket", e);
        }
    }

//    public void run() {
//        byte[] buffer = new byte[1024];
//        int bytes = 0; // bytes returned from read()
//        int numberOfReadings = 0; //to control the number of readings from the Arduino
//
//        // Keep listening to the InputStream until an exception occurs.
//        //We just want to get 1 temperature readings from the Arduino
//        while (numberOfReadings < 1) {
//            try {
//
//                buffer[bytes] = (byte) mmInStream.read();
//                String readMessage;
//                // If I detect a "\n" means I already read a full measurement
//                if (buffer[bytes] == '\n') {
//                    readMessage = new String(buffer, 0, bytes);
//                    Log.e(TAG, readMessage);
//                    //Value to be read by the Observer streamed by the Obervable
//                    valueRead=readMessage;
//                    bytes = 0;
//                    numberOfReadings++;
//                } else {
//                    bytes++;
//                }
//d
//            } catch (IOException e) {
//                Log.d(TAG, "Input stream was disconnected", e);
//                break;
//            }
//        }
//
//    }
}