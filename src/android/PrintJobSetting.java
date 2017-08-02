package com.momzor.cordova.plugin.brotherPrinter;


import com.brother.ptouch.sdk.PrinterInfo;

public class PrintJobSetting {
    String modelName;
    PrinterInfo.Model model;
    int numberOfCopies;

    PrinterInfo.Port port;

    String ipAddress = null;
    String macAddress = null;

    int labelNameIndex;
    PrinterInfo.Orientation orientation;

    PrintJobSetting(String modelName, PrinterInfo.Model model, int labelNameIndex, PrinterInfo.Orientation orientation) {
        this.modelName = modelName;
        this.model = model;
        this.labelNameIndex = labelNameIndex;
        this.orientation= orientation;
    }
}
