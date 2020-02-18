package net.bytebutcher.burpsendtoextension.models;

import burp.BurpExtender;
import burp.IBurpExtenderCallbacks;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import net.bytebutcher.burpsendtoextension.models.placeholder.behaviour.CommandSeparatedPlaceholderBehaviour;
import net.bytebutcher.burpsendtoextension.models.placeholder.behaviour.PlaceholderBehaviour;
import net.bytebutcher.burpsendtoextension.models.placeholder.behaviour.StringSeparatedPlaceholderBehaviour;
import net.bytebutcher.burpsendtoextension.utils.OsUtils;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private final IBurpExtenderCallbacks callbacks;
    private final Gson gson;
    private BurpExtender burpExtender;
    private String version = "1.3";

    public Config(BurpExtender burpExtender) {
        this.burpExtender = burpExtender;
        this.callbacks = BurpExtender.getCallbacks();
        this.gson = initGson();
        refreshVersion();
    }

    private Gson initGson() {
        RuntimeTypeAdapterFactory<PlaceholderBehaviour> placeholderBehaviourAdapterFactory = RuntimeTypeAdapterFactory.of(PlaceholderBehaviour.class, "type")
                .registerSubtype(StringSeparatedPlaceholderBehaviour.class, "StringSeparated")
                .registerSubtype(CommandSeparatedPlaceholderBehaviour.class, "CommandSeparated");
        return new GsonBuilder().registerTypeAdapterFactory(placeholderBehaviourAdapterFactory).create();
    }

    public void saveSendToTableData(List<CommandObject> sendToTableData) {
        this.callbacks.saveExtensionSetting("SendToTableData", gson.toJson(sendToTableData));
    }

    public List<CommandObject> getSendToTableData() {
        List<CommandObject> commandObjectList = new ArrayList<>();
        try {
            String sendToTableData = this.callbacks.loadExtensionSetting("SendToTableData");
            if (sendToTableData == null || sendToTableData.isEmpty() || "[]".equals(sendToTableData)) {
                if (isFirstStart()) {
                    BurpExtender.printOut("Initializing default table data...");
                    commandObjectList = initializeDefaultSendToTableData();
                }
                return commandObjectList;
            }
            return gson.fromJson(sendToTableData, new TypeToken<List<CommandObject>>() {}.getType());
        } catch (Exception e) {
            BurpExtender.printErr("Error retrieving table data!");
            BurpExtender.printErr(e.toString());
            return commandObjectList;
        }
    }

    private List<CommandObject> initializeDefaultSendToTableData() {
        List<CommandObject> commandObjectList = getDefaultSendToTableData();
        saveSendToTableData(commandObjectList);
        unsetFirstStart();
        return commandObjectList;
    }

    public List<CommandObject> getDefaultSendToTableData() {
        String groupFuzz = "fuzz";
        String groupCMS = "cms";
        String groupSQL = "sql";
        String groupSSL = "ssl";
        String groupOther = "other";
        List<PlaceholderBehaviour> defaultPlaceholderBehaviour = Lists.newArrayList();
        return Lists.newArrayList(
                // cms
                new CommandObject("droopescan", "droopescan scan drupal -u %U -t 10", groupCMS, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("mooscan", "mooscan -v --url %U", groupCMS, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("wpscan", "wpscan --url %U --threads 10", groupCMS, true, true, false, defaultPlaceholderBehaviour),
                // fuzz
                new CommandObject("bfac", "bfac --url %U", groupFuzz, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("gobuster", "gobuster -u %U -s 403,404 -w /usr/share/wfuzz/wordlist/general/common.txt", groupFuzz, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("nikto", "nikto %U", groupFuzz, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("wfuzz", "wfuzz -c -w /usr/share/wfuzz/wordlist/general/common.txt --hc 404,403 %U", groupFuzz, true, true, false, defaultPlaceholderBehaviour),
                // sql
                new CommandObject("sqlmap (GET)", "sqlmap -o -u %U --level=5 --risk=3", groupSQL, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("sqlmap (POST)", "sqlmap -r %R  --level=5 --risk=3", groupSQL, true, true, false, defaultPlaceholderBehaviour),
                // ssl
                new CommandObject("sslscan", "sslscan %H:%P", groupSSL, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("sslyze", "sslyze --regular %H:%P", groupSSL, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("testssl", "testssl.sh %H:%P", groupSSL, true, true, false, defaultPlaceholderBehaviour),
                // other
                new CommandObject("Host (%H)", "echo %H", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("Port (%P)", "echo %P", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("Protocol (%T)", "echo %T", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("URL (%U)", "echo %U", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("URL-Path (%A)", "echo %A", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("URL-Query (%Q)", "echo %Q", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("Cookies (%C)", "echo %C", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Method (%M)", "echo %M", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("Selected text (%S)", "echo %S", groupOther, true, true, true, defaultPlaceholderBehaviour),
                new CommandObject("Selected text as file (%F)", "echo %F && cat %F", groupOther, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Request/-Response as file (%R)", "echo %R && cat %R", groupOther, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Headers as file (%E)", "echo %E && cat %E", groupOther, true, true, false, defaultPlaceholderBehaviour),
                new CommandObject("HTTP-Body as file (%B)", "echo %B && cat %B", groupOther, true, true, false, defaultPlaceholderBehaviour)
        );
    }

    private void refreshVersion() {
        if (!this.version.equals(this.callbacks.loadExtensionSetting("version"))) {
            this.callbacks.saveExtensionSetting("version", version);
            setFirstStart();
        }
    }

    private boolean isFirstStart() {
        String isFirstStart = this.callbacks.loadExtensionSetting("isFirstStart");
        return isFirstStart == null || "true".equals(isFirstStart);
    }

    private void setFirstStart() {
        this.callbacks.saveExtensionSetting("isFirstStart", null);
    }

    private void unsetFirstStart() {
        this.callbacks.saveExtensionSetting("isFirstStart", "false");
    }

    public void resetRunInTerminalCommand() {
        this.callbacks.saveExtensionSetting("runInTerminalSettingWindows", "cmd  /c start cmd /K %C");
        this.callbacks.saveExtensionSetting("runInTerminalSettingUnix", "xterm -hold -e %C");

    }

    public String getRunInTerminalCommand() {
        if (OsUtils.isWindows()) {
            return getRunInTerminalCommand("runInTerminalSettingWindows", "cmd  /c start cmd /K %C");
        } else {
            return getRunInTerminalCommand("runInTerminalSettingUnix", "xterm -hold -e %C");
        }
    }

    private String getRunInTerminalCommand(String label, String defaultValue) {
        String runInTerminalSetting = this.callbacks.loadExtensionSetting(label);
        if (runInTerminalSetting == null || runInTerminalSetting.isEmpty()) {
            runInTerminalSetting = defaultValue;
            this.callbacks.saveExtensionSetting(label, runInTerminalSetting);
        }
        return runInTerminalSetting;
    }

    public void setRunInTerminalCommand(String command) {
        if (OsUtils.isWindows()) {
            this.callbacks.saveExtensionSetting("runInTerminalSettingWindows", command);
        } else {
            this.callbacks.saveExtensionSetting("runInTerminalSettingUnix", command);
        }
    }

}
