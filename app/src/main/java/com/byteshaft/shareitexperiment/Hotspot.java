package com.byteshaft.shareitexperiment;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;

public class Hotspot {

    private Context mContext;
    private boolean mWasWifiDisabled;
    private WifiManager mWifiManager;

    public Hotspot(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    private void turnOffWifiIfOn() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            mWasWifiDisabled = true;
        }
    }

    void create(String name) {
        turnOffWifiIfOn();
        WifiConfiguration netConfig = new WifiConfiguration();
        netConfig.SSID = name;
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        try {
            Method setWifiApMethod = mWifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, boolean.class);
            boolean apstatus = (Boolean) setWifiApMethod.invoke(mWifiManager, netConfig, true);
//            Method isWifiApEnabledMethod = mWifiManager.getClass().getMethod("isWifiApEnabled");
//            while(!(Boolean) isWifiApEnabledMethod.invoke(mWifiManager)) {}
//            Method getWifiApStateMethod = mWifiManager.getClass().getMethod("getWifiApState");
//            int apstate = (Integer) getWifiApStateMethod.invoke(mWifiManager);
//            Method getWifiApConfigurationMethod = mWifiManager.getClass().getMethod("getWifiApConfiguration");
//            netConfig = (WifiConfiguration)getWifiApConfigurationMethod.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e("HOTSPOT", "", e);
        }
    }

    void destroy() {
        try {
            Method setWifiApMethod = mWifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, boolean.class);
            setWifiApMethod.invoke(mWifiManager, null, false);
        } catch (Exception e) {
            Log.e("HOTSPOT", "", e);
        }
        if (mWasWifiDisabled) {
            mWifiManager.setWifiEnabled(true);
            mWasWifiDisabled = false;
        }
    }
}
