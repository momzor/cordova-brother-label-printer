package com.momzor.cordova.plugin.brotherPrinter;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.brother.ptouch.sdk.LabelInfo;
import com.brother.ptouch.sdk.NetPrinter;
import com.brother.ptouch.sdk.Printer;
import com.brother.ptouch.sdk.PrinterInfo;
import com.brother.ptouch.sdk.PrinterStatus;
import com.brother.ptouch.sdk.connection.BluetoothConnectionSetting;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Set;

import static com.momzor.cordova.plugin.brotherPrinter.PrinterUtil.LOG_TAG;

public class BrotherPrinter extends CordovaPlugin {

    private PrintJobSetting printJobSetting = new PrintJobSetting("QL-710W", PrinterInfo.Model.QL_710W, LabelInfo.QL700.W62.ordinal(), PrinterInfo.Orientation.LANDSCAPE);

    private Boolean blueToothPrinterFound = false;
    private Boolean networkPrinterFound = false;


    public void pluginInitialize() {
        Log.d(LOG_TAG, "Initializing " + getClass().getSimpleName());
        super.pluginInitialize();
        PrinterUtil.requestStoragePermission(this);
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if ("printViaBluetooth".equals(action)) {
            printViaBluetooth(args, callbackContext);
            return true;
        }

        if ("printViaNetwork".equals(action)) {
            printViaNetwork(args, callbackContext);
            return true;
        }

        return false;
    }


    private void findNetworkPrinters() {

        NetPrinter[] netPrinters = PrinterUtil.enumerateNetPrinters(printJobSetting.modelName);

        int netPrinterCount = netPrinters.length;

        if (netPrinterCount > 0) {

            Log.d(LOG_TAG, "---- network printers found! ----");

            for (int i = 0; i < netPrinterCount; i++) {

                printJobSetting.ipAddress = netPrinters[i].ipAddress;
                printJobSetting.macAddress = netPrinters[i].macAddress;


                Log.d(LOG_TAG,
                        " idx:    " + Integer.toString(i)
                                + "\n model:  " + netPrinters[i].modelName
                                + "\n ip:     " + netPrinters[i].ipAddress
                                + "\n mac:    " + netPrinters[i].macAddress
                                + "\n serial: " + netPrinters[i].serNo
                                + "\n name:   " + netPrinters[i].nodeName
                );
            }

            networkPrinterFound = true;
            Log.d(LOG_TAG, "---- /network printers found! ----");

        } else if (netPrinterCount == 0) {
            networkPrinterFound = false;
            Log.d(LOG_TAG, "!!!! No network printers found !!!!");
        }
    }

    private void printViaBluetooth(final JSONArray args, final CallbackContext callbackctx) {

        printJobSetting.port = PrinterInfo.Port.BLUETOOTH;
        printJobSetting.numberOfCopies = args.optInt(1, 1);

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                try {

                    final Bitmap bitmap = PrinterUtil.toBitmap(args.optString(0, null), callbackctx);
                    if (bitmap == null) {
                        return;
                    }

                    findBluetoothPairedPrinters(callbackctx);

                    if (!blueToothPrinterFound) {
                        PluginResult result;
                        result = new PluginResult(PluginResult.Status.ERROR, "No printer was found. Aborting.");
                        callbackctx.sendPluginResult(result);
                    }

                    Printer myPrinter = PrinterUtil.initPrinterProperties(printJobSetting);

                    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                    BluetoothConnectionSetting.setBluetoothAdapter(bluetoothAdapter);

                    PrinterStatus status = myPrinter.printImage(bitmap);

                    String status_code = "" + status.errorCode;

                    PluginResult result = new PluginResult(PluginResult.Status.OK, status_code);

                    callbackctx.sendPluginResult(result);

                } catch (Exception e) {

                    PluginResult result;
                    Log.e(LOG_TAG, "!!!! Exception !!!!=== ", e);
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

        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (bluetoothAdapter != null) {
                if (!bluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(
                            BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    /** startActivity(enableBtIntent);**/
                }
            } else {
                blueToothPrinterFound = false;
                PluginResult result;
                result = new PluginResult(PluginResult.Status.ERROR, "No BluetoothAdapter was found");
                callbackctx.sendPluginResult(result);
                return;
            }

            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            if (pairedDevices != null && pairedDevices.size() > 0) {
                blueToothPrinterFound = true;

                for (BluetoothDevice device : pairedDevices) {
                    printJobSetting.macAddress = device.getAddress();
                    Log.d(LOG_TAG, device.getAddress());

                }

                Log.d(LOG_TAG, "---- /bluetooth printers found! ----");

            } else {
                blueToothPrinterFound = false;
                Log.d(LOG_TAG, "---- /NO bluetooth printers found! ----");
            }

        } catch (Exception e) {
            blueToothPrinterFound = false;
            PluginResult result;
            Log.e(LOG_TAG, "!!!! Exception !!!!=== ", e);
            result = new PluginResult(PluginResult.Status.ERROR, "Can't find bluetooth paired devices: " + e);
            callbackctx.sendPluginResult(result);
        }
    }


    private void printViaNetwork(final JSONArray args, final CallbackContext callbackctx) {

        printJobSetting.numberOfCopies = args.optInt(1, 1);
        printJobSetting.port = PrinterInfo.Port.NET;

        cordova.getThreadPool().execute(new Runnable() {
            public void run() {

                try {

                    final Bitmap bitmap = PrinterUtil.toBitmap(args.optString(0, null), callbackctx);
                    if (bitmap == null) {
                        return;
                    }

                    //TODO: integrate retry N times(configurable) before giving up
                    findNetworkPrinters();

                    if (!networkPrinterFound) {
                        PluginResult result = new PluginResult(PluginResult.Status.ERROR, "No Network printer was found. Aborting.");
                        callbackctx.sendPluginResult(result);
                        return;
                    }

                    Printer myPrinter = PrinterUtil.initPrinterProperties(printJobSetting);

                    PrinterStatus status = myPrinter.printImage(bitmap);
                    PluginResult result = new PluginResult(PluginResult.Status.OK, String.valueOf(status.errorCode));
                    callbackctx.sendPluginResult(result);

                } catch (Exception e) {
                    PluginResult result;
                    Log.e(LOG_TAG, "!!!! Exception while printing !!!!=== ", e);
                    result = new PluginResult(PluginResult.Status.ERROR, "FAILED : " + e);
                    callbackctx.sendPluginResult(result);

                }
            }
        });
    }


}
