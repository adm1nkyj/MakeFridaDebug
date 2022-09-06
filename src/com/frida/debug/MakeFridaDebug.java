package com.frida.debug;

import com.frida.debug.ui.*;
import com.pnfsoftware.jeb.core.*;
import com.pnfsoftware.jeb.core.units.code.android.IDexDecompilerUnit;
import com.pnfsoftware.jeb.core.units.code.android.IDexUnit;
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexClass;
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexMethod;
import com.pnfsoftware.jeb.core.units.code.android.dex.IDexType;
import com.pnfsoftware.jeb.core.units.codeobject.ICodeObjectUnit;
import com.pnfsoftware.jeb.rcpclient.JebApp;
import com.pnfsoftware.jeb.rcpclient.PublicContext;
import com.pnfsoftware.jeb.rcpclient.RcpClientContext;
import com.pnfsoftware.jeb.rcpclient.extensions.ConsoleViewer;
import com.pnfsoftware.jeb.rcpclient.extensions.app.model.IMPartManager;
import com.pnfsoftware.jeb.rcpclient.handlers.JebBaseHandler;
import com.pnfsoftware.jeb.rcpclient.parts.*;
import com.pnfsoftware.jeb.util.logging.GlobalLog;
import com.pnfsoftware.jeb.util.logging.ILogger;

import java.util.*;
import java.util.List;

import com.pnfsoftware.jeb.rcpclient.extensions.app.*;
import org.eclipse.swt.widgets.*;

import com.pnfsoftware.jeb.rcpclient.extensions.binding.KeyShortcutsManager;

public class MakeFridaDebug extends AbstractEnginesPlugin {
    private static final ILogger logger = GlobalLog.getLogger(MakeFridaDebug.class);
    private JebApp appContext = null;
    private TerminalPartManager terminalPartManager = null;
    private RcpClientContext rcpContext = null;
    private IEnginesContext enginesContext = null;
    private MainView mainView = null;
    @Override
    public IPluginInformation getPluginInformation() {
        return new PluginInformation("Make Frida Debug",
                "make help to frida debug", "yongjin kim",
                new Version(1,0));
    }
    @Override
    public List<? extends IOptionDefinition> getExecutionOptionDefinitions() {
        return null;
    }
    public IEnginesContext getEnginesContext(){
        return this.enginesContext;
    }
    public RcpClientContext getRcpContext(){
        return this.rcpContext;
    }
    public JebApp getJebApp(){
        return this.appContext;
    }
    public TerminalPartManager getTerminalPartManager(){
        return this.terminalPartManager;
    }

    @Override
    public void dispose() {
    }
    public MainView getMainView(){
        return this.mainView;
    }
    @Override
    public void load(IEnginesContext context) {
        this.enginesContext = context;
        this.rcpContext = RcpClientContext.getInstance();
        this.appContext = (JebApp)rcpContext.getApp();

        mainView = new MainView(this);
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                rcpContext.getKeyAccelaratorManager().registerHandler(new ShortCutHandler(MakeFridaDebug.this, "noid", "noname", "notooltip", "noicon"));
            }
        });

        Display.getDefault().asyncExec(mainView);

        System.out.println("loaded plugin");
    }

    @Override
    public void execute(IEnginesContext context) {
        PublicContext ctx = new PublicContext(this.rcpContext);

        String current_addr = ctx.getFocusedAddress();
        IDexUnit codeUnit = ((IDexDecompilerUnit)ctx.getFocusedUnit().getParent()).getCodeUnit();
        IDexClass iClass = codeUnit.getClass(current_addr.split("->")[0]);
        IDexMethod iMethod = codeUnit.getMethod(current_addr);

        String currentClassName = iClass.getSignature(true);
        String originalClassName = iClass.getSignature(false);

        String currentMethodName = iMethod.getName(true);
        String originalMethodName = iMethod.getName(false);

        List<IDexType> argument_list = iMethod.getParameterTypes();

        if(originalMethodName.equals("<init>")) {
            originalMethodName = "$init";
        }

        String args = "";
        String args_var = "";
        int i = 1;
        if(argument_list.size() > 0) {
            for (IDexType argument : argument_list) {
                String arg = this.toCanonicalName(argument.getSignature(false));
                args += String.format("'%s',", arg);
                args_var += String.format("arg%d,", i);
                i += 1;
            }
            args = args.substring(0, args.length()-1);
            args_var = args_var.substring(0, args_var.length()-1);

        }
        Boolean isVoidMethod = iMethod.getReturnType().getSignature().equals("V");
        Boolean isOverloadMethod = this.isOverloadMethod(iClass, iMethod);

        String overload = "";
        String hookCode = "";
        String var = randStr(8);
        if(isOverloadMethod){
            overload = String.format(".overload(%s)", args);
        }

        String defaultCode = String.format("\tconsole.log('%s / %s hooked');", toCanonicalName(currentClassName), currentMethodName);

        defaultCode += String.format("\n\t%s%s.%s.call(this%s);", isVoidMethod ? "" : "return ", var, originalMethodName, args_var.length()>0 ? String.format(", %s", args_var) : "");
        String template =  String.format("var %s = Java.use('%s');\n", var, toCanonicalName(originalClassName));
        template += String.format("%s.%s%s.implementation = function(%s){\n", var, originalMethodName, overload, args_var);
        template += defaultCode + "\n}";

        this.mainView.getManager().addToHookTable(String.format("%s / %s hooked", toCanonicalName(currentClassName), currentMethodName), iMethod.getAddress(), template);

    }
    public Boolean isOverloadMethod(IDexClass iDexClass, IDexMethod iMethod) {
        List<IDexMethod> methodList = (List<IDexMethod>) iDexClass.getMethods();
        for(IDexMethod method : methodList){
            if(method.getName(false).equals(iMethod.getName(false))){
                return true;
            }
        }
        return false;
    }
    public String randStr(int length){
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
    public String toCanonicalName(String mname) {
        mname = mname.replace('/', '.');
        HashMap<String, String> table = new HashMap<String, String>();
        table.put("C", "char");
        table.put("I", "int");
        table.put("B", "byte");
        table.put("Z", "boolean");
        table.put("F", "float");
        table.put("D", "double");
        table.put("S", "short");
        table.put("J", "long");
        table.put("V", "void");
        table.put("L", mname.length() > 2 ? mname.substring(1,mname.length()-1) : "none");
        table.put("[", mname);

        String result = table.get(mname.substring(0,1));
        return result;
    }
    @Override
    public void execute(IEnginesContext iEnginesContext, Map<String, String> map) {
        System.out.println("execute2");
    }

    public static void main(String[] args){
        System.out.println("frida debug load");
    }
}
class ShortCutHandler extends JebBaseHandler{
    MakeFridaDebug context = null;
    public ShortCutHandler(MakeFridaDebug context, String id, String name, String tooltip, String icon) {
        super(id, name, tooltip, icon);
        setAccelerator(393286); // ctrl+shift+f
        this.context = context;
    }

    @Override // com.pnfsoftware.jeb.rcpclient.handlers.JebBaseHandler, com.pnfsoftware.jeb.rcpclient.extensions.binding.ActionEx
    public boolean canExecute() {
        return true;
    }

    @Override // com.pnfsoftware.jeb.rcpclient.handlers.JebBaseHandler, com.pnfsoftware.jeb.rcpclient.extensions.binding.ActionEx
    public void execute() {
        RcpClientContext context = RcpClientContext.getInstance();
        if (context == null) {
            throw new RuntimeException("The UI context cannot be retrieved");
        }
        this.context.execute(context.getEnginesContext());
    }
}

