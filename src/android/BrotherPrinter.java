package com.momzor.cordova.plugin.brotherPrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.brother.ptouch.sdk.connection.BluetoothConnectionSetting;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.Manifest;
import android.os.Build;
import android.content.pm.PackageManager;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import java.util.Set;

public class BrotherPrinter extends CordovaPlugin {

    String modelName = "QL-820NWB";

    public static final int PERMISSION_WRITE_EXTERNAL_STORAGE = 1;
    private NetPrinter[] netPrinters;
    private NetPrinter[] mBluetoothPrinter; // array of storing Printer

    private String ipAddress   = null;
    private String macAddress  = null;
    private Boolean searched   = false;
    private Boolean wifiSearched = false;
    private Boolean found      = false;
    private Boolean wifiFound = false;

    private ArrayList<String> mItems = null; // List of storing the printer's

    //token to make it easy to grep logcat
    private static final String TAG = "print";

    private CallbackContext callbackctx;



    public void pluginInitialize() {
                            Log.d(TAG, "OMG");

        super.pluginInitialize();
        if (!isPermitWriteStorage()) {
            cordova.requestPermission(this,PERMISSION_WRITE_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
     }


    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("findNetworkPrinters".equals(action)) {
            findNetworkPrinters(callbackContext);
            return true;
        }

        if ("printViaSDK".equals(action)) {
            printViaSDK(args, callbackContext);
            return true;
        }

        if ("printViaWifiInfra".equals(action)) {
            printViaWifiInfra(args, callbackContext);
            return true;
        }

        if ("findBluetoothPairedPrinters".equals(action)) {
            findBluetoothPairedPrinters(callbackContext);
            return true;
        }

        return false;
    }

    private NetPrinter[] enumerateNetPrinters() {
        Printer myPrinter = new Printer();
        PrinterInfo myPrinterInfo = new PrinterInfo();
        netPrinters = myPrinter.getNetPrinters(modelName);
        return netPrinters;
    }


    private boolean isPermitWriteStorage() {
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
             if (cordova.getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                     != PackageManager.PERMISSION_GRANTED) {
                 return false;
             }
         }
         return true;
     }


    private void findNetworkPrinters(final CallbackContext callbackctx) {

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    wifiSearched = true;

                    NetPrinter[] netPrinters = enumerateNetPrinters();
                    int netPrinterCount = netPrinters.length;

                    ArrayList<Map> netPrintersList = null;
                    if(netPrintersList != null) netPrintersList.clear();
                    netPrintersList = new ArrayList<Map>();

                    if (netPrinterCount > 0) {
                        wifiFound = true;
                        Log.d(TAG, "---- network printers found! ----");

                        for (int i = 0; i < netPrinterCount; i++) {
                            Map<String, String> netPrinter = new HashMap<String, String>();

                            ipAddress = netPrinters[i].ipAddress;
                            macAddress = netPrinters[i].macAddress;

                            netPrinter.put("ipAddress", netPrinters[i].ipAddress);
                            netPrinter.put("macAddress", netPrinters[i].macAddress);
                            netPrinter.put("serNo", netPrinters[i].serNo);
                            netPrinter.put("nodeName", netPrinters[i].nodeName);

                            netPrintersList.add(netPrinter);

                            Log.d(TAG,
                                    " idx:    " + Integer.toString(i)
                                            + "\n model:  " + netPrinters[i].modelName
                                            + "\n ip:     " + netPrinters[i].ipAddress
                                            + "\n mac:    " + netPrinters[i].macAddress
                                            + "\n serial: " + netPrinters[i].serNo
                                            + "\n name:   " + netPrinters[i].nodeName
                            );
                        }

                        Log.d(TAG, "---- /network printers found! ----");

                    }else if (netPrinterCount == 0 ) {
                        wifiFound = false;
                        Log.d(TAG, "!!!! No network printers found !!!!");
                    }

                    JSONArray args = new JSONArray();
                    PluginResult result;

                    Boolean available = netPrinterCount > 0;

                    args.put(available);
                    args.put(netPrintersList);

                    result = new PluginResult(PluginResult.Status.OK, args);

                    callbackctx.sendPluginResult(result);

                }catch(Exception e){
                    e.printStackTrace();
                }

            }

        });

    }

    public static Bitmap bmpFromBase64(String base64, final CallbackContext callbackctx){
        try{
            byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private void printViaSDK(final JSONArray args, final CallbackContext callbackctx) {

        final Bitmap bitmap = bmpFromBase64(args.optString(0, null), callbackctx);
        final int numberOfCopies = args.optInt(1, 1);

        if(!searched){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "You must first run findNetworkPrinters() to search the network.");
            callbackctx.sendPluginResult(result);
        }

        if(!found){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "No printer was found. Aborting.");
            callbackctx.sendPluginResult(result);
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    Printer myPrinter = new Printer();
                    PrinterInfo myPrinterInfo = new PrinterInfo();
                    myPrinterInfo = myPrinter.getPrinterInfo();
                    PluginResult result;
                    myPrinterInfo.printerModel  = PrinterInfo.Model.QL_820NWB;
                    myPrinterInfo.port          = PrinterInfo.Port.BLUETOOTH;;
                    myPrinterInfo.printMode     = PrinterInfo.PrintMode.ORIGINAL;
                    myPrinterInfo.orientation   = PrinterInfo.Orientation.PORTRAIT;
                    myPrinterInfo.paperSize     = PrinterInfo.PaperSize.CUSTOM;

                    myPrinterInfo.labelNameIndex =  LabelInfo.QL700.valueOf("W62RB").ordinal();
                    myPrinterInfo.isAutoCut=true;
                    myPrinterInfo.isCutAtEnd=true;
                    myPrinterInfo.isHalfCut=true;
                    myPrinterInfo.isSpecialTape= false;
                    myPrinterInfo.numberOfCopies = numberOfCopies;

                    myPrinterInfo.macAddress= macAddress;

                    boolean isSet;
                    isSet = myPrinter.setPrinterInfo(myPrinterInfo);

                    if(bitmap == null){
                        result = new PluginResult(PluginResult.Status.ERROR, " Bitmap creation failed");
                        callbackctx.sendPluginResult(result);
                    }

                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothConnectionSetting.setBluetoothAdapter(bluetoothAdapter);

                    PrinterStatus status = myPrinter.printImage(bitmap);

                    String status_code = ""+status.errorCode;

                    result = new PluginResult(PluginResult.Status.OK, status_code);

                    callbackctx.sendPluginResult(result);

                }catch(Exception e){

                    PluginResult result;
                    e.printStackTrace();

                    result = new PluginResult(PluginResult.Status.ERROR, "Failed to print with bluetooth");
                    callbackctx.sendPluginResult(result);

                }
            }
        });
    }


    /**
     * get paired printers
     */
    private void findBluetoothPairedPrinters(final CallbackContext callbackctx) {

        // get the BluetoothAdapter


        searched = true;

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                /** startActivity(enableBtIntent);**/
            }
        }else{
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "No BluetoothAdapter found");
            callbackctx.sendPluginResult(result);

            return;
        }

        try {

            mItems = new ArrayList<String>();
            JSONArray args = new JSONArray();
			/*
             * if the paired devices exist, set the paired devices else set the
			 * string of "No Bluetooth Printer."
			 */
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter != null ? bluetoothAdapter.getBondedDevices() : null;
            if ((pairedDevices != null ? pairedDevices.size() : 0) > 0) {
                found = true;

                args.put(true);

                mBluetoothPrinter = new NetPrinter[pairedDevices.size()];
                int i = 0;
                String strDev = "";

                for (BluetoothDevice device : pairedDevices) {
                    strDev = "";
                    strDev += device.getAddress();
                    macAddress = device.getAddress();
                    mItems.add(strDev);
                    Log.d(TAG, strDev);

                    i++;
                }

                args.put(mItems);

                Log.d(TAG, "---- /bluetooth printers found! ----");

            } else {
                found = false;

                args.put(false);
                Log.d(TAG, "---- /NO bluetooth printers found! ----");

            }
            // plugin result;

            PluginResult result;
            result = new PluginResult(PluginResult.Status.OK, args);

            callbackctx.sendPluginResult(result);
        } catch (Exception e) {
            PluginResult result;
            e.printStackTrace();

            result = new PluginResult(PluginResult.Status.ERROR, "Can't find bluetooth paired devices");
            callbackctx.sendPluginResult(result);

        }
    }



    private void printViaWifiInfra(final JSONArray args, final CallbackContext callbackctx) {

        final Bitmap bitmap = bmpFromBase64(args.optString(0, null), callbackctx);
        final int numberOfCopies = args.optInt(1, 1);

        if(!wifiSearched){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "You must first run findNetworkPrinters() to search the network.");
            callbackctx.sendPluginResult(result);
        }

        if(!wifiFound){
            PluginResult result;
            result = new PluginResult(PluginResult.Status.ERROR, "No printer was found. Aborting.");
            callbackctx.sendPluginResult(result);
        }

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try{

                    Printer myPrinter = new Printer();
                    PrinterInfo myPrinterInfo = new PrinterInfo();
                    myPrinterInfo = myPrinter.getPrinterInfo();
                    PluginResult result;

                    myPrinterInfo.printerModel  = PrinterInfo.Model.QL_820NWB;
                    myPrinterInfo.port          = PrinterInfo.Port.NET;
                    myPrinterInfo.printMode     = PrinterInfo.PrintMode.ORIGINAL;
                    myPrinterInfo.orientation   = PrinterInfo.Orientation.PORTRAIT;
                    myPrinterInfo.paperSize     = PrinterInfo.PaperSize.CUSTOM;

                    myPrinterInfo.labelNameIndex =  LabelInfo.QL700.valueOf("W62RB").ordinal();;
                    myPrinterInfo.isAutoCut=true;
                    myPrinterInfo.isCutAtEnd=true;
                    myPrinterInfo.isHalfCut=true;
                    myPrinterInfo.isSpecialTape= false;
                    myPrinterInfo.numberOfCopies = numberOfCopies;

                    myPrinterInfo.ipAddress     = ipAddress;
                    myPrinterInfo.macAddress    = macAddress;


                    boolean isSet;
                    isSet = myPrinter.setPrinterInfo(myPrinterInfo);

                    if(bitmap == null){
                        result = new PluginResult(PluginResult.Status.ERROR, " Bitmap creation failed");
                        callbackctx.sendPluginResult(result);
                    }

                    PrinterStatus status = myPrinter.printImage(bitmap);
                    result = new PluginResult(PluginResult.Status.OK, ""+status.errorCode);
                    callbackctx.sendPluginResult(result);

                }catch(Exception e){
                    PluginResult result;
                    e.printStackTrace();
                    result = new PluginResult(PluginResult.Status.ERROR,  "FAILED");
                    callbackctx.sendPluginResult(result);

                }
            }
        });
    }

}
