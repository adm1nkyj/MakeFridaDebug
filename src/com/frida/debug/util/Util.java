package com.frida.debug.util;

import com.pnfsoftware.jeb.core.units.code.android.adb.*;
import com.pnfsoftware.jeb.rcpclient.RcpClientContext;

import java.io.IOException;
import java.util.List;
import java.util.Random;

public class Util {
    static public List<AdbDevice> getAdbDeviceList() {
        AdbWrapperFactory adbWrapperFactory = null;
        try {
            adbWrapperFactory = new AdbWrapperFactory();
            adbWrapperFactory.initialize();
            return adbWrapperFactory.listDevices();
        }
        catch(IOException e){
            return null;
        }
    }
    public static String randStr(int length){
        int leftLimit = 97; // numeral 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = length;
        Random random = new Random();

        String generatedString = random.ints(leftLimit,rightLimit + 1)
          .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
          .limit(targetStringLength)
          .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
          .toString();

        return generatedString;
    }
    static public String getCurrentPackageName(RcpClientContext rcpClientContext){
        try {
            String packageName = rcpClientContext.getMainProject().getLiveArtifact(0).getMainUnit().getName();
            return packageName;
        }
        catch(Exception e){
            return "";
        }
    }
    static public List<AdbProcess> getProcessList(AdbDevice adbDevice){
        if(adbDevice == null){
            return null;
        }
        try{
            AdbWrapper adbWrapper = (new AdbWrapperFactory()).createWrapper(adbDevice.getSerial());
            return adbWrapper.listProcesses();
        }
        catch(IOException e){
            return null;
        }

    }
}
