package com.frida.debug.ui;

import com.frida.debug.MakeFridaDebug;
import com.pnfsoftware.jeb.rcpclient.extensions.app.*;

public class MainView implements Runnable {
    MakeFridaDebug context = null;
    Part fridaConsole = null;
    private MainViewManager mainViewManager = null;
    public MainView(MakeFridaDebug context){
        this.context = context;
    }

    private void initConsoleView() {
        this.fridaConsole = this.context.getJebApp().folderConsoles.addPart();
        this.fridaConsole.setElementId("frida.console.debug");
        this.fridaConsole.setManager(this.context.getTerminalPartManager());
        this.fridaConsole.setLabel("Frida Console");
        mainViewManager = new MainViewManager(this.context.getRcpContext(), this.context);
        this.fridaConsole.setManager(mainViewManager);
        Folder folder = (Folder) this.fridaConsole.getParentElement();
        folder.showPart(this.fridaConsole);
    }
    public MainViewManager getManager(){
        return this.mainViewManager;
    }
    @Override
    public void run() {
        this.initConsoleView();
    }

}

