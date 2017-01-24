package com.byteshaft.shareitexperiment;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveActivity extends AppCompatActivity {

    private Hotspot mHotspot;
    private final String SSID = "1234567890";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive);
        mHotspot = new Hotspot(getApplicationContext());
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (Settings.System.canWrite(getApplicationContext())) {
//                mHotspot.create(Helpers.generateSSID(SSID, getSparePort()));
//            } else {
//                startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS));
//            }
//        } else {
//            mHotspot.create(Helpers.generateSSID(SSID, getSparePort()));
//        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int bytesRead;
                    ServerSocket serverSocket = new ServerSocket(0);
                    mHotspot.create(Helpers.generateSSID(SSID, String.valueOf(serverSocket.getLocalPort())));
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        InputStream in = clientSocket.getInputStream();
                        DataInputStream clientData = new DataInputStream(in);
                        String fileName = clientData.readUTF();
                        final File f = new File(Environment.getExternalStorageDirectory() + "/" + fileName);
                        OutputStream output = new FileOutputStream(f);
                        long size = clientData.readLong();
                        byte[] buffer = new byte[1024];
                        while (size > 0 && (bytesRead = clientData.read(buffer, 0, (int) Math.min(buffer.length, size))) != -1) {
                            output.write(buffer, 0, bytesRead);
                            size -= bytesRead;
                        }
                        output.close();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), f.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHotspot.destroy();
    }
}
