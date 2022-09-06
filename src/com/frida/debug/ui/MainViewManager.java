package com.frida.debug.ui;

import com.frida.debug.MakeFridaDebug;
import com.frida.debug.ScriptManager;
import com.frida.debug.frida.FridaManager;
import com.frida.debug.util.Util;
import com.pnfsoftware.jeb.client.api.ButtonGroupType;
import com.pnfsoftware.jeb.client.api.IconType;
import com.pnfsoftware.jeb.core.units.code.ICodeUnit;
import com.pnfsoftware.jeb.core.units.code.android.adb.*;
import com.pnfsoftware.jeb.rcpclient.PublicContext;
import com.pnfsoftware.jeb.rcpclient.RcpClientContext;
import com.pnfsoftware.jeb.rcpclient.actions.GraphicalActionExecutor;
import com.pnfsoftware.jeb.rcpclient.extensions.app.model.IMPart;
import com.pnfsoftware.jeb.rcpclient.parts.AbstractPartManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
public class MainViewManager extends AbstractPartManager {
    public Table hookTable = null;
    public Text codeTextBox = null;
    public Text resultTextBox = null;
    public MakeFridaDebug mainContext = null;
    public RcpClientContext rcpClientContext = null;
    public Combo deviceCombo = null;
    public Combo processListCombo = null;
    public List<AdbDevice> adbDeviceList = null;
    public List<AdbProcess> processList = null;
    public static AdbDevice currentDevice = null;
    public static AdbProcess currentProcess = null;
    public FridaManager fridaManager = null;
    public MainViewManager(RcpClientContext context, MakeFridaDebug mainContext) {
        super(context);
        this.rcpClientContext = context;
        this.mainContext = mainContext;
    }

    @Override
    public boolean createView(Composite composite, IMPart imPart) {
        Composite mainComposite = composite;
        GridLayout fill = new GridLayout(1, false);
        fill.marginTop = 10;
        mainComposite.setLayout(fill);

        Composite headComposite = new Composite(mainComposite, 0);
//        headComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        headComposite.setLayout(new GridLayout(10, false));

        Label label1 = new Label(headComposite, 0);
        label1.setText("Device : ");
        this.adbDeviceList = Util.getAdbDeviceList();
        String items[] = new String[this.adbDeviceList.size()];
        for(int i=0; i<this.adbDeviceList.size(); i++){
                items[i] = this.adbDeviceList.get(i).getSerial();
        }
        deviceCombo = new Combo(headComposite, SWT.READ_ONLY);
        if(adbDeviceList.size() == 0) {
            deviceCombo.setText("not connected");
        }
        else {
            deviceCombo.setItems(items);
        }
        deviceCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int index = deviceCombo.getSelectionIndex();
                currentDevice = adbDeviceList.get(index);
                if(currentDevice != null){
                    processList = Util.getProcessList(currentDevice);
                    if(processList != null){
                        String[] processes = new String[processList.size()];
                        String currentPackage = Util.getCurrentPackageName(rcpClientContext);
                        int a = 0;
                        for(int i=0; i<processList.size(); i++){
                            processes[i] = processList.get(i).getName();
                        }
                        Arrays.sort(processes);
                        processListCombo.setItems(processes);
                        if(processListCombo.indexOf(currentPackage) != -1){
                            a = processListCombo.indexOf(currentPackage);
                        }
                        processListCombo.select(a);
                    }
                }
                super.widgetSelected(e);
            }
        });
        Label label2 = new Label(headComposite, 0);
        label2.setText("Processes : ");
        processListCombo = new Combo(headComposite, SWT.NONE);
        GridData comboGrid = new GridData();
        comboGrid.widthHint = 350;
        processListCombo.setLayoutData(comboGrid);
        if(this.currentDevice == null || processList == null){
            processListCombo.setText("not connected");
        }
        processListCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
//                int index = processListCombo.getSelectionIndex();
                String selectedProcessName = processListCombo.getText(); // select process by process name
                for(AdbProcess process : processList){
                    if(selectedProcessName.equals(process.getName())){
                        currentProcess = process;
                        processListCombo.select(processListCombo.getSelectionIndex());
                        return;
                    }
                }
//                currentProcess = processList.get(index);
//                super.widgetSelected(e);
            }
        });


        Button reloadButton = new Button(headComposite, SWT.PUSH);
        reloadButton.setText("Reload");

        Button attachButton = new Button(headComposite, SWT.PUSH);
        attachButton.setText("Attach");

        Button dettachButton = new Button(headComposite, SWT.PUSH);
        dettachButton.setEnabled(false);
        dettachButton.setText("Dettach");

        reloadButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                adbDeviceList = Util.getAdbDeviceList();
                String items[] = new String[adbDeviceList.size()];
                for(int i=0; i< adbDeviceList.size(); i++){
                        items[i] = adbDeviceList.get(i).getSerial();
                }
                if(adbDeviceList.size() == 0) {
                    deviceCombo.setText("not connected");
                }
                else {
                    deviceCombo.setItems(items);
                }
            }
        });
        
        attachButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                 for(AdbProcess process : processList){
                    if(processListCombo.getText().equals(process.getName())){
                        currentProcess = process;
                        processListCombo.select(processListCombo.getSelectionIndex());
                    }
                }
                fridaManager = new FridaManager();
                fridaManager.setAttachPid(Integer.toString(currentProcess.getPid()));
                fridaManager.setResultTextBox(resultTextBox);
                fridaManager.setDaemon(true);
                fridaManager.start();
                deviceCombo.setEnabled(false);
                processListCombo.setEnabled(false);
                attachButton.setEnabled(false);
                dettachButton.setEnabled(true);
                super.widgetSelected(e);
            }
        });

        dettachButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                fridaManager.interrupt();
                deviceCombo.setEnabled(true);
                processListCombo.setEnabled(true);
                attachButton.setEnabled(true);
                dettachButton.setEnabled(false);
                super.widgetSelected(e);
            }
        });
        Button utilFunctions = new Button(headComposite, SWT.PUSH);
        utilFunctions.setText("Util functions");
        utilFunctions.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PublicContext pc = new PublicContext(rcpClientContext);
                String utilScript = ScriptManager.getInstance().getUtilScript();
                String changedScript = pc.displayText("Util Functions", utilScript, true);
                if(changedScript != null && !utilScript.equals(changedScript)){
                    ScriptManager.getInstance().setUtilScript(changedScript);
                    pc.displayMessageBox("Saved", "Saved Script", IconType.INFORMATION, ButtonGroupType.OK);
                }
                super.widgetSelected(e);
            }
        });

        Button clearText = new Button(headComposite, SWT.PUSH);
        clearText.setText("Clear");
        clearText.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                resultTextBox.setText("");
                super.widgetSelected(e);
            }
        });
//        Label status = ;
//        showFullSource.setText("Get Full Script");

        SashForm rightForm = new SashForm(mainComposite, SWT.HORIZONTAL);
        GridData gridData2 = new GridData(GridData.FILL_BOTH);
        rightForm.setLayoutData(gridData2);
        GridLayout rightFormLayout = new GridLayout(1, false);
        rightForm.setLayout(rightFormLayout);


        SashForm leftForm = new SashForm(rightForm, SWT.VERTICAL);
        GridData gridData = new GridData(GridData.FILL_BOTH);
        leftForm.setLayoutData(gridData);
        GridLayout leftformLayout = new GridLayout(1, false);
        leftForm.setLayout(leftformLayout);

        hookTable = new Table(leftForm, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION);
        hookTable.setHeaderVisible(true);
        hookTable.setLinesVisible(true);
        hookTable.addSelectionListener(new SelectionAdapter() {
            // when the TableEditor is over a cell, select the corresponding row in
            // the table
            @Override
            public void widgetSelected(SelectionEvent e) {
                TableItem[] items = hookTable.getSelection();
                String code = ScriptManager.getInstance().get(Integer.parseInt(items[0].getText()));
                codeTextBox.setText(code);
            }
        });
        hookTable.addMenuDetectListener(new MenuDetectListener() {
            @Override
            public void menuDetected( MenuDetectEvent event ) {
                Menu menu = new Menu( hookTable );
                MenuItem gotoAddress = new MenuItem( menu, SWT.POP_UP );
                gotoAddress.setText( "Go to Address" );
                gotoAddress.addSelectionListener( new SelectionAdapter() {
                    @Override
                    public void widgetSelected( SelectionEvent arg0 ) {
                        TableItem[] items = hookTable.getSelection();
                        PublicContext ctx = new PublicContext(rcpClientContext);
                        ICodeUnit iUnit = rcpClientContext.getMainProject().findUnit(ICodeUnit.class);
                        GraphicalActionExecutor.gotoAddress(rcpClientContext, iUnit, items[0].getText(2));
                    }
                });
                MenuItem removeItem = new MenuItem( menu, SWT.POP_UP );
                removeItem.setText( "Remove element" );
                removeItem.addSelectionListener( new SelectionAdapter() {
                    @Override
                    public void widgetSelected( SelectionEvent arg0 ) {
                        TableItem[] items = hookTable.getSelection();
                        System.out.println(items[0].getText(0));
                        System.out.println(ScriptManager.getInstance().getHookCodeList().size());
                        ScriptManager.getInstance().del(Integer.parseInt(items[0].getText(0)));
                        hookTable.remove(hookTable.getSelectionIndex());
                        if(ScriptManager.getInstance().getHookCodeList().size() == 0){
                            codeTextBox.setText("");
                        }
                    }
                });
                hookTable.setMenu( menu );
                menu.setVisible( true );
            }
        });
        TableColumn hookTableIdxColumn = new TableColumn(hookTable, SWT.NONE);
        hookTableIdxColumn.setText("#");
        hookTableIdxColumn.setWidth(30);
        TableColumn hookTableInfoColumn = new TableColumn(hookTable, SWT.NONE);
        hookTableInfoColumn.setWidth(400);
        hookTableInfoColumn.setText("information");
        TableColumn hookTableLocationColumn = new TableColumn(hookTable, SWT.NONE);
        hookTableLocationColumn.setWidth(500);
        hookTableLocationColumn.setText("Location");


        codeTextBox = new Text(leftForm, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
        codeTextBox.setLayoutData(new GridData(GridData.FILL_BOTH));
        codeTextBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.keyCode == 's' && ((e.stateMask & SWT.CONTROL) != 0)){
                    int index = hookTable.getSelectionIndex();
                    if(index == -1) {
                        PublicContext pc = new PublicContext(rcpClientContext);
                        pc.displayMessageBox("Error", "plz select code", IconType.ERROR, ButtonGroupType.OK);
                        return;
                    }
                    String modifyCode = codeTextBox.getText();
                    int scriptIndex = Integer.parseInt(hookTable.getSelection()[0].getText(0));

                    ScriptManager.getInstance().put(scriptIndex, modifyCode);
                    PublicContext pc = new PublicContext(rcpClientContext);
                    pc.displayMessageBox("Modify", "Saved", IconType.INFORMATION, ButtonGroupType.OK);
                }
                super.keyPressed(e);
            }
        });


        resultTextBox = new Text(rightForm, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        resultTextBox.setLayoutData(new GridData(GridData.FILL_BOTH));
        resultTextBox.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.keyCode == 'k' && ((e.stateMask & SWT.CONTROL) != 0)) {
                    resultTextBox.setText("");
                }
                super.keyPressed(e);
            }
        });

        return true;
    }
    public void addToHookTable(String hookInfo, String location, String hookCode){
        int index = this.hookTable.getItemCount()+1;
        TableItem tableItem = new TableItem(this.hookTable, SWT.NONE);
        tableItem.setText(new String[] { Integer.toString(index), hookInfo, location});

        ScriptManager.getInstance().put(index, hookCode);
    }
}