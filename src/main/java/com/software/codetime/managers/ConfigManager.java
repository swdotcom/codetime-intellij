package com.software.codetime.managers;

import com.software.codetime.models.ConfigOptions;
import com.software.codetime.websockets.WebsocketMessageHandler;

import java.util.logging.Logger;

public class ConfigManager {
    public static final Logger log = Logger.getLogger("ConfigManager");

    public static String metrics_endpoint = "https://api.software.com";
    public static String app_url = "https://app.software.com";
    public static String intellij_issues_url = "https://github.com/swdotcom/codetime-intellij/issues";
    public static String WS_URL = "wss://api.software.com/websockets";
    public static String create_org_url = app_url + "/github_onboard";
    public static String software_dir = ".software";
    public static long DEFAULT_SESSION_THRESHOLD_SECONDS = 60 * 5;
    public static int plugin_id;
    public static String plugin_name = "Code Time";
    public static String plugin_version = "";
    public static String ide_name = "";
    public static String ide_version = "";
    public static String plugin_type = "codetime";
    public static String plugin_editor = "intellij";
    public static Runnable tree_refresh_runnable = null;
    public static SessionStatusUpdateManager status_bar_update_handler = null;
    public static IdeType ide_type = null;
    public static WebsocketMessageHandler ws_msg_handler = null;
    public static ThemeModeInfoHandler theme_mode_handler = null;

    public static void init(
            ConfigOptions options,
            Runnable treeRefreshRunnable,
            WebsocketMessageHandler wsMsgHandler,
            SessionStatusUpdateManager statusBarUpdateHandler,
            ThemeModeInfoHandler themeModeHandler,
            IdeType ideType) {
        metrics_endpoint = options.metricsEndpoint;
        app_url = options.appUrl;
        software_dir = options.softwareDir;
        plugin_type = options.pluginType;
        plugin_editor = options.pluginEditor;
        create_org_url = app_url + "/github_onboard";
        plugin_id = options.pluginId;
        plugin_name = options.pluginName;
        plugin_version = options.pluginVersion;
        ide_name = options.ideName;
        ide_version = options.ideVersion;
        tree_refresh_runnable = treeRefreshRunnable;
        ws_msg_handler = wsMsgHandler;
        status_bar_update_handler = statusBarUpdateHandler;
        theme_mode_handler = themeModeHandler;
        ide_type = ideType;
    }

    public enum IdeType {
        intellij
    }
}
