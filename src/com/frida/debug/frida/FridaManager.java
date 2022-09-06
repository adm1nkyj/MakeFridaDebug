package com.frida.debug.frida;

import com.frida.debug.util.Util;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;

import java.io.*;

public class FridaManager extends Thread{
    public static FridaManager fridaManager = null;
    public  String attachPid = "";
    public  static String scriptPath = "";
    public Text resultTextBox = null;
    private  StdOutManager stdOutManager = null;
    public static Boolean stopFlag = false;

    @Override
    public State getState(){
        return State.NEW;
    }
    public FridaManager(){
        scriptPath = String.format("%s/%s", System.getProperty("java.io.tmpdir"), "fridacode_"+ Util.randStr(8)+".js");
        System.out.println(scriptPath);
        try {
            File file = new File(scriptPath);
            FileOutputStream fio = new FileOutputStream(file);
            fio.write("Java.perform(function(){})".getBytes());
            fio.close();
        }
        catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
    public void setAttachPid(String pid){
        this.attachPid = pid;
    }
    public void setResultTextBox(Text textBox){
        this.resultTextBox = textBox;
    }

    @Override
    public void run(){
        this.stopFlag = false;
        System.out.println("attach to " + this.attachPid);
        String[] cmd = {"frida", "-U", "-p", this.attachPid, "-l", scriptPath};
        ProcessBuilder builder = null;
        Process process = null;
        try {
            builder = new ProcessBuilder(cmd);
            process = builder.start();
//            OutputStream stdin = process.getOutputStream();
            InputStream stdout = process.getInputStream();
//            InputStream stderr = process.getErrorStream();
//            this.stdin = stdin;
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            stdOutManager = new StdOutManager(reader, resultTextBox);
            stdOutManager.setDaemon(true);
            stdOutManager.start();
            process.waitFor();
        }
        catch(IOException e) {
            System.out.println(e.getMessage());
        } catch (InterruptedException e) {
            System.out.println("stop");
            process.destroy();
            this.stopFlag = true;

            return;
        }
    }


}
class StdOutManager extends Thread{
    private BufferedReader reader = null;
    private Text resultTextBox = null;
    public StdOutManager(BufferedReader reader, Text resultTextBox){
        this.reader= reader;
        this.resultTextBox = resultTextBox;
    }
    @Override
    public void run() {
        String result = "";
        while (true) {
            try {
                if(FridaManager.stopFlag){
                    return;
                }
                if (this.reader.ready()) {
                    result += (char) this.reader.read();
                } else {
                    if (result.length() > 0) {
                        String finalResult = result;
                        result = "";
                        Display.getDefault().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                String prevText = resultTextBox.getText();

                                String tmp = prevText + finalResult;
                                resultTextBox.setText(tmp);
                            }
                        });

                    }
                }
            } catch (Exception e) {
                break;
            }
        }
        return;


    }
}