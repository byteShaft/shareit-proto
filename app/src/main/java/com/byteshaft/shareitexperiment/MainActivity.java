package com.byteshaft.shareitexperiment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        NsdManager.RegistrationListener, NsdManager.DiscoveryListener {

    private Hotspot mHotspot;
    private boolean mCreated;
    private String mServiceName;
    private NsdManager mNsdManager;
    private static final String TAG = "OK";
    public static final String SERVICE_TYPE = "_fileshare._tcp";
    public static final String SERVICE_NAME = "fileshare";
    private boolean mWaitingForConnection;
    private final int RESULT_LOAD_IMAGE = 101;
    private String mImagePath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button buttonSend = (Button) findViewById(R.id.button_send);
        Button buttonReceive = (Button) findViewById(R.id.button_receive);
        buttonSend.setOnClickListener(this);
        buttonReceive.setOnClickListener(this);
        mHotspot = new Hotspot(getApplicationContext());
        mNsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            mImagePath = getImgPath(selectedImage);
            String networkSSID = "1234567890abcdef";
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + networkSSID + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
            wifiManager.addNetwork(conf);
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for( WifiConfiguration i : list ) {
                if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    break;
                }
            }
            System.out.println("Reached discovery start code");
            mNsdManager.discoverServices(
                    SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, MainActivity.this);
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
                if (mCreated) {
                    mHotspot.destroy();
                    mNsdManager.unregisterService(this);
                    mCreated = false;
                } else {
                    mHotspot.create("1234567890abcdef");
                    try {
                        ServerSocket socket = new ServerSocket(0);
                        int port = socket.getLocalPort();
                        registerService(port);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    mCreated = true;
                }
                break;
        }
    }

    public void registerService(int port) {
        NsdServiceInfo serviceInfo  = new NsdServiceInfo();
        serviceInfo.setServiceName(SERVICE_NAME);
        serviceInfo.setServiceType(SERVICE_TYPE);
        serviceInfo.setPort(port);
        mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, this);
    }

    @Override
    public void onRegistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {
        System.out.println("Registration failed " + i);
    }

    @Override
    public void onUnregistrationFailed(NsdServiceInfo nsdServiceInfo, int i) {

    }

    @Override
    public void onServiceRegistered(final NsdServiceInfo nsdServiceInfo) {
        mServiceName = nsdServiceInfo.getServiceName();
        System.out.println("Registered: " + mServiceName);
        mWaitingForConnection = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(nsdServiceInfo.getPort());
                    while (mWaitingForConnection) {
                        Socket socket = serverSocket.accept();
                        final File f = new File(Environment.getExternalStorageDirectory() + "/"
                                + getPackageName() + "/shareitexperiment-" + System.currentTimeMillis()
                                + ".jpg");
                        File dirs = new File(f.getParent());
                        if (!dirs.exists())
                            dirs.mkdirs();
                        f.createNewFile();
                        InputStream inputstream = socket.getInputStream();
                        copyFile(inputstream, new FileOutputStream(f));
                        serverSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static boolean copyFile(InputStream inputStream, OutputStream out) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public void onServiceUnregistered(NsdServiceInfo nsdServiceInfo) {
        mWaitingForConnection = false;
    }

    @Override
    public void onDiscoveryStarted(String regType) {
        Log.d(TAG, "Service discovery started " + regType);
    }

    @Override
    public void onServiceFound(NsdServiceInfo service) {
        // A service was found!  Do something with it.
        Log.d(TAG, "Service discovery success " + service);
        if (!service.getServiceType().equals(SERVICE_TYPE)) {
            // Service type is the string containing the protocol and
            // transport layer for this service.
            Log.d(TAG, "Unknown Service Type: " + service.getServiceType());
        } else if (service.getServiceName().equals(mServiceName)) {
            // The name of the service tells the user what they'd be
            // connecting to. It could be "Bob's Chat App".
            Log.d(TAG, "Same machine: " + mServiceName);
        } else if (service.getServiceName().contains(SERVICE_NAME)){
            mNsdManager.resolveService(service, new NsdManager.ResolveListener() {
                @Override
                public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                    System.out.println("Resolve failed");
                }

                @Override
                public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                    System.out.println("Resolve successful");
                    System.out.println(nsdServiceInfo.getHost().toString());
                    sendFileOverNetwork(nsdServiceInfo, mImagePath);
                }
            });
        }
    }

    @Override
    public void onServiceLost(NsdServiceInfo service) {
        // When the network service is no longer available.
        // Internal bookkeeping code goes here.
        Log.e(TAG, "service lost" + service);
    }

    @Override
    public void onDiscoveryStopped(String serviceType) {
        Log.i(TAG, "Discovery stopped: " + serviceType);
    }

    @Override
    public void onStartDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
    }

    @Override
    public void onStopDiscoveryFailed(String serviceType, int errorCode) {
        Log.e(TAG, "Discovery failed: Error code:" + errorCode);
        mNsdManager.stopServiceDiscovery(this);
    }

    private void sendFileOverNetwork(NsdServiceInfo nsdServiceInfo, String filePath) {
        int len;
        Socket socket = new Socket();
        byte buf[]  = new byte[8000];
        try {
            socket.bind(null);
            socket.connect((new InetSocketAddress(
                    nsdServiceInfo.getHost().toString(), nsdServiceInfo.getPort())), 500);
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = getContentResolver().openInputStream(Uri.parse(filePath));
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket.isConnected()) {
                try {
                    socket.close();
                } catch (IOException e) {
                    //catch logic
                }
            }
        }
    }
}
