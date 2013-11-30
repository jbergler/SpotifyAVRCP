package com.jonasbergler.xposed.spotify;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Bitmap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

/**
 * Created by jbergler on 29/11/2013.
 */
public class Spotify implements IXposedHookLoadPackage {
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.spotify.mobile.android.ui"))
            return;

        Object activityThread = callStaticMethod(findClass("android.app.ActivityThread", null), "currentActivityThread");
        Context context = (Context) callMethod(activityThread, "getSystemContext");
        PackageInfo pi = context.getPackageManager().getPackageInfo(lpparam.packageName, 0);

        String versionName = pi.versionName;

        XposedBridge.log("SpotifyAVRCP: We're in Spotify [" + versionName + "]");

        // Define a big list of know function names for versions here
        Map<String, String[]> versionToFunction = new HashMap<String, String[]>();
        versionToFunction.put("0.7.3.636.ga3aacdd3", new String[]{"c", "b"});

        // Lookup the function names for current version
        String methodName = "c";
        if (versionToFunction.containsKey(versionName))
            methodName = versionToFunction.get(versionName)[0];

        findAndHookMethod("com.spotify.mobile.android.service.v", lpparam.classLoader, methodName, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                // this will be called before the clock was updated by the original method
            }
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Object paramx = getObjectField(param.thisObject, "b");
                Service service = (Service) getObjectField(param.thisObject, "a");

                String track = (String) callMethod(paramx, "d");
                String artist = (String) callMethod(paramx, "f");
                String album = (String) callMethod(paramx, "e");

                //XposedBridge.log("Spotify track: " + track + " artist: " + artist + " album: " + album);

                Intent localIntent = new Intent("com.android.music.metachanged");
                localIntent.putExtra("track", track);
                localIntent.putExtra("artist", artist);
                localIntent.putExtra("album", album);
                localIntent.putExtra("playing", true);
                service.sendBroadcast(localIntent);
            }
        });

//        findAndHookMethod("com.spotify.mobile.android.service.v", lpparam.classLoader, "b", new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                Service service = (Service) getObjectField(param.thisObject, "a");
//
//                Intent localIntent = new Intent("com.android.music.metachanged");
//                localIntent.putExtra("track", "");
//                localIntent.putExtra("artist", "");
//                localIntent.putExtra("album", "");
//                localIntent.putExtra("playing", false);
//                service.sendBroadcast(localIntent);
//            }
//        });
    }
}
