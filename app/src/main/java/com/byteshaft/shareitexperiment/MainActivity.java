package com.byteshaft.shareitexperiment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int RESULT_LOAD_IMAGE = 101;
    private String mImagePath;
    private WifiManager mWifiManager;
    private boolean mScanRequested;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonSend = (Button) findViewById(R.id.button_send);
        Button buttonReceive = (Button) findViewById(R.id.button_receive);
        buttonSend.setOnClickListener(this);
        buttonReceive.setOnClickListener(this);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        registerReceiver(mWifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) && mScanRequested) {
                mScanRequested = false;
                List<ScanResult> mScanResults = mWifiManager.getScanResults();
                for (ScanResult result : mScanResults) {
                    if (result.SSID.startsWith("SH-")) {
                        String[] ssidData = result.SSID.split("-");
                        String identifier = decodeString(ssidData[1]);
                        final String port = decodeString(ssidData[2]);
                        if (!mWifiManager.getConnectionInfo().getSSID().contains(result.SSID)) {
                            WifiConfiguration conf = new WifiConfiguration();
                            conf.SSID = "\"" + result.SSID + "\"";
                            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                            final WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
                            wifiManager.addNetwork(conf);
                            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
                            for( WifiConfiguration i : list ) {
                                if(i.SSID != null && i.SSID.equals("\"" + result.SSID + "\"")) {
                                    wifiManager.disconnect();
                                    wifiManager.enableNetwork(i.networkId, true);
                                    wifiManager.reconnect();
                                }
                            }
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String hostIP = intToInetAddress(mWifiManager.getDhcpInfo().serverAddress).toString().replace("/", "");
                                sendFileOverNetwork(hostIP, port, mImagePath);
                            }
                        }).start();
                        break;
                    }
                }
            }
        }
    };

    private String decodeString(String base64String) {
        byte[] data = Base64.decode(base64String, Base64.DEFAULT);
        String text = null;
        try {
            text = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return text;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            mImagePath = getImgPath(selectedImage);
            mScanRequested = true;
            mWifiManager.startScan();
        }
    }
    private InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            throw new AssertionError();
        }
    }

    public String getImgPath(Uri uri) {
        String[] filePathColumn = { MediaStore.Images.Media.DATA };
        Cursor cursor = getContentResolver().query(uri,filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        return cursor.getString(columnIndex);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_send:
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RESULT_LOAD_IMAGE);
                break;
            case R.id.button_receive:
                startActivity(new Intent(getApplicationContext(), ReceiveActivity.class));
                break;
        }
    }

    private void sendFileOverNetwork(String hostIP, String port, String filePath) {
        try {
            Socket sock = new Socket(hostIP, Integer.valueOf(port));
            File myFile = new File(filePath);
            byte[] mybytearray = new byte[(int) myFile.length()];
            FileInputStream fis = new FileInputStream(myFile);
            BufferedInputStream bis = new BufferedInputStream(fis);
            DataInputStream dis = new DataInputStream(bis);
            dis.readFully(mybytearray, 0, mybytearray.length);
            OutputStream os = sock.getOutputStream();

            //Sending file name and file size to the server
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeUTF(myFile.getName());
            dos.writeLong(mybytearray.length);
            dos.write(mybytearray, 0, mybytearray.length);
            dos.flush();

            //Closing socket
            sock.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
