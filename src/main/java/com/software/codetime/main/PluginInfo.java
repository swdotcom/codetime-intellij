package com.software.codetime.main;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.application.ApplicationInfo;

import java.util.Arrays;

public class PluginInfo {
    public static String metrics_endpoint = "https://api.software.com";
    public static String app_url = "https://app.software.com";
    public static String software_dir = ".software";

    public static String IDE_NAME = "";
    public static String IDE_VERSION = "";

    // sublime = 1, vs code = 2, eclipse = 3, intellij = 4, visual studio = 6, atom = 7
    private static final int pluginId = 4;
    public static String VERSION = null;
    public static String pluginName = null;

    private static final int APPCODE_ID = 22;
    private static final int CLION_ID = 24;
    private static final int DATAGRIP_ID = 26;
    private static final int GOLAND_ID = 28;
    private static final int PHPSTORM_ID = 30;
    private static final int PYCHARM_ID = 32;
    private static final int RIDER_ID = 34;
    private static final int RUBYMINE_ID = 36;
    private static final int WEBSTORM_ID = 38;

    static {
        try {
            IDE_NAME = ApplicationInfo.getInstance().getFullApplicationName();
            IDE_VERSION = ApplicationInfo.getInstance().getFullVersion();
        } catch (Exception e) {
            System.out.println("Unable to retrieve IDE name and version info: " + e.getMessage());
        }
    }

    public static int getPluginId() {
        return pluginId;
    }

    public static String getVersion() {
        if (VERSION == null) {
            IdeaPluginDescriptor pluginDescriptor = getIdeaPluginDescriptor();
            if (pluginDescriptor != null) {
                VERSION = pluginDescriptor.getVersion();
            } else {
                return "latest";
            }
        }
        return VERSION;
    }

    public static String getPluginName() {
        if (pluginName == null) {
            IdeaPluginDescriptor pluginDescriptor = getIdeaPluginDescriptor();
            if (pluginDescriptor != null) {
                pluginName = pluginDescriptor.getName();
            } else {
                return "CodeTime";
            }
        }
        return pluginName;
    }

    public static boolean isEditorOpsInstalled() {
        IdeaPluginDescriptor[] descriptors = PluginManager.getPlugins();
        if (descriptors != null && descriptors.length > 0) {
            IdeaPluginDescriptor editorOps = Arrays.asList(descriptors).stream()
                    .filter(d -> d.getPluginId().getIdString().equals("com.softwareco.intellij.ops.plugin"))
                    .findAny()
                    .orElse(null);
            return editorOps != null;
        }
        return false;
    }

    private static IdeaPluginDescriptor getIdeaPluginDescriptor() {
        IdeaPluginDescriptor[] descriptors = PluginManager.getPlugins();
        if (descriptors != null) {
            for (IdeaPluginDescriptor descriptor : descriptors) {
                if (descriptor.getPluginId().getIdString().equals("com.softwareco.intellij.plugin")) {
                    return descriptor;
                }
            }
        }
        return null;
    }
}
