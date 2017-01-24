package com.byteshaft.shareitexperiment;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Method;

class Hotspot {

    private boolean mCreated;
    private Context mContext;
    private boolean mWasWifiDisabled;
    private WifiManager mWifiManager;
    private String mName;

    Hotspot(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    }

    private void turnOffWifiIfOn() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
            mWasWifiDisabled = true;
        }
    }

    boolean isCreated() {
        try {
            Method getWifiApConfigurationMethod = mWifiManager.getClass().getMethod("getWifiApConfiguration");
            WifiConfiguration netConfig = (WifiConfiguration)getWifiApConfigurationMethod.invoke(mWifiManager);
            return netConfig.SSID.equals(mName);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isAPCreated() {
        try {
            Method isWifiApEnabledMethod = mWifiManager.getClass().getMethod("isWifiApEnabled");
            return (boolean) (Boolean) isWifiApEnabledMethod.invoke(mWifiManager);
        } catch (Exception e) {
            return false;
        }
    }

    void create(String name) {
        mName = name;
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
            mCreated = (boolean) (Boolean) setWifiApMethod.invoke(mWifiManager, netConfig, true);
            while (!isAPCreated()) {}
//            Method getWifiApStateMethod = mWifiManager.getClass().getMethod("getWifiApState");
//            int apstate = (Integer) getWifiApStateMethod.invoke(mWifiManager);
        } catch (Exception e) {
            Log.e("HOTSPOT", "", e);
        }
    }

    void destroy() {
        try {
            Method setWifiApMethod = mWifiManager.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, boolean.class);
            mCreated = (boolean) setWifiApMethod.invoke(mWifiManager, null, false);
        } catch (Exception e) {
            Log.e("HOTSPOT", "", e);
        }
        if (mWasWifiDisabled) {
            mWifiManager.setWifiEnabled(true);
            mWasWifiDisabled = false;
        }
    }
}
