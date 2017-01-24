package com.byteshaft.shareitexperiment;

import android.util.Base64;

import java.io.UnsupportedEncodingException;

class Helpers {
    static String generateSSID(String identifier, String port) {
        try {
            String output = String.format(
                    "SH-%s-%s",
                    Base64.encodeToString(identifier.getBytes("UTF-8"), Base64.DEFAULT).trim(),
                    Base64.encodeToString(port.getBytes("UTF-8"), Base64.DEFAULT).trim()
            );
            System.out.println(output);
            return output;
        } catch (UnsupportedEncodingException e) {
            return "UNABLE";
        }
    }
}
