package com.frida.debug;

import com.frida.debug.frida.FridaManager;
import java.io.*;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class ScriptManager {
    private static HashMap<Integer, String> hookCodeList = new HashMap<Integer, String>();
    private static ScriptManager instance = null;
    public static String scriptCode = "";
    public static String utilScript = "";
    public static ScriptManager getInstance(){
        if(instance == null){
            instance = new ScriptManager();
        }
        return instance;
    }
    public static String getUtilScript(){
        return utilScript;
    }
    public static void setUtilScript(String script){
        utilScript = script;
    }
    public static HashMap<Integer, String> getHookCodeList(){
        return hookCodeList;
    }
    public static String get(int index) {
        return hookCodeList.get(index);
    }

    public static void put(int index, String hookCode) {
        hookCodeList.put(index, hookCode);
        reloadScript();
    }
    public static void del(int index) {
        hookCodeList.remove(index);
        reloadScript();
    }
    public static void reloadScript() {
//        String scriptPath = FridaManager.scriptPath;
//        System.out.println(scriptPath);
        scriptCode = "Java.perform(function() {\n\n";
        if(utilScript.length() > 0){
            scriptCode += utilScript + "\n";
        }
        for(Entry<Integer, String>entry : hookCodeList.entrySet()){
            scriptCode += entry.getValue() + "\n";
        }
//        System.out.println(scriptCode);
        scriptCode += "\n});";

        try {
            File file = new File(FridaManager.scriptPath);
            FileOutputStream fio = new FileOutputStream(file);
            fio.write(scriptCode.getBytes());
            fio.close();
        }
        catch (Exception e){
            return;
        }
    }
}
