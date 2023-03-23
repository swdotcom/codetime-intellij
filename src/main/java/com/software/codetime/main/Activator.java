package com.software.codetime.main;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.wm.ToolWindowManager;
import com.software.codetime.listeners.*;
import com.software.codetime.managers.*;
import com.software.codetime.models.IntellijProject;
import com.software.codetime.models.KeystrokeWrapper;
import com.software.codetime.toolwindows.codetime.SidebarToolWindow;
import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.*;
import swdc.java.ops.model.ConfigOptions;
import swdc.java.ops.snowplow.events.UIInteractionType;
import swdc.java.ops.websockets.WebsocketClient;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Activator {

    public static final Logger log = Logger.getLogger("Activator");

    private static Activator instance = null;
    private final AsyncManager asyncManager = AsyncManager.getInstance();

    public static Activator getInstance() {
        if (instance == null) {
            instance = new Activator();
        }
        return instance;
    }

    private Activator() {
        init();
    }

    private void init() {
        String appName = ApplicationInfo.getInstance().getFullApplicationName();
        log.log(Level.INFO, ConfigManager.plugin_name + ": Loaded v" + PluginInfo.getPluginId() + " - " + appName);

        ConfigOptions options = new ConfigOptions();
        options.ideName = PluginInfo.IDE_NAME;
        options.pluginType = "codetime";
        options.pluginEditor = "intellij";
        options.appUrl = PluginInfo.app_url;
        options.ideVersion = PluginInfo.IDE_VERSION;
        options.metricsEndpoint = PluginInfo.metrics_endpoint;
        options.pluginId = PluginInfo.getPluginId();
        options.pluginName = PluginInfo.getPluginName();
        options.pluginVersion = PluginInfo.getVersion();
        options.softwareDir = PluginInfo.software_dir;
        options.pluginId = PluginInfo.getPluginId();
        ConfigManager.init(
                options,
                () -> refreshSidebar(),
                new WebsocketMessageManager(),
                new SessionStatusUpdateManager(),
                new ThemeModeInfoManager(),
                ConfigManager.IdeType.intellij);

        // create anon user if no user exists
        anonCheck();

        Application app = ApplicationManager.getApplication();

        // fetch the user and preferences
        app.invokeLater(() -> {
            AccountManager.getUser();
        });

        app.invokeLater(() -> {
            // update the session summary and status bar
            SessionSummaryManager.updateSessionSummaryFromServer();
        });

        // connect the websocket
        app.invokeLater(() -> {
            try {
                WebsocketClient.connect();
            } catch (Exception e) {
                log.warning("Websocket connect error: " + e.getMessage());
            }
        });

        app.invokeLater(() -> {
            // initialize the tracker
            EventTrackerManager.getInstance().init(new IntellijProject());

            // send the activate event
            EventTrackerManager.getInstance().trackEditorAction("editor", "activate");
        });

        app.invokeLater(() -> {
            // set the end of the day notification
            EndOfDayManager.setEndOfDayNotification();
        });

        app.invokeLater(() -> {
            FlowManager.initFlowStatus();
        });

        app.invokeLater(() -> {
            // add the editor listeners
            setupEditorListeners();
        });

        // show the readme/sidebar on install
        AsyncManager.getInstance().executeOnceInSeconds(() -> {
            readmeCheck();
        }, 5);
    }

    private void refreshSidebar() {
        ApplicationManager.getApplication().invokeLater(() -> {
            SidebarToolWindow.refresh(false);
        });
    }

    private void anonCheck() {
        // create anon user if no user exists
        String jwt = FileUtilManager.getItem("jwt");
        if (StringUtils.isBlank(jwt)) {
            AccountManager.createAnonymousUser(false);
        }
    }

    private void readmeCheck() {
        String readmeDisplayed = FileUtilManager.getItem("intellij_CtReadme");
        if (readmeDisplayed == null || Boolean.valueOf(readmeDisplayed) == false) {
            ApplicationManager.getApplication().invokeLater(() -> {
                // send an initial plugin payload
                ReadmeManager.openReadmeFile(UIInteractionType.keyboard);
                FileUtilManager.setItem("intellij_CtReadme", "true");

                com.intellij.openapi.wm.ToolWindow toolWindow = ToolWindowManager.getInstance(
                        ProjectActivateListener.getCurrentProject()
                ).getToolWindow("Code Time");
                if (toolWindow != null) {
                    toolWindow.show();
                }
            });
        }
    }

    private void setupEditorListeners() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                // edit document
                EditorFactory.getInstance().getEventMulticaster().addDocumentListener(
                        new DocumentChangeListener(), this::disposeComponent);
            } catch (Exception e) {
                log.log(Level.WARNING, "Error initializing document listener: " + e.getMessage());
            }

            Project p = ProjectActivateListener.getCurrentProject();
            if (p == null) {
                p = IntellijProjectManager.getFirstActiveProject();
            }
            if (p != null) {
                try {
                    // file open,close,selection listener
                    p.getMessageBus().connect().subscribe(
                            FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorListener());
                } catch (Exception e) {
                    // error trying to subscribe to the message bus
                    log.log(Level.WARNING, "Error initializing file editor listener: " + e.getMessage());
                }

                try {
                    // file save
                    p.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES, new DocumentSaveListener());
                } catch (Exception e) {
                    // error trying to subscribe to the message bus
                    log.log(Level.WARNING, "Error initializing file save listener: " + e.getMessage());
                }

                try {
                    p.getMessageBus().connect().subscribe(EditorColorsManager.TOPIC, new ThemeColorChangeListener());
                } catch (Exception e) {
                    // error trying to subscribe to the message bus
                    log.log(Level.WARNING, "Error initializing editor color listener: " + e.getMessage());
                }

                GitEventsManager gitEvtMgr = new GitEventsManager();
                try {
                    gitEvtMgr.setUpGitFileListener(p.getBasePath());
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error initializing git file listener: " + e.getMessage());
                }

                SessionSummaryManager sessionSummaryMgr = new SessionSummaryManager();
                try {
                    sessionSummaryMgr.setSessionSummaryChangeListener();
                } catch (Exception e) {
                    log.log(Level.WARNING, "Error initializing session summary change listener: " + e.getMessage());
                }
            }
        });
    }

    public void disposeComponent() {
        // process any remaining updates
        KeystrokeWrapper keystrokeManager = KeystrokeWrapper.getInstance();
        if (keystrokeManager.getKeystrokeCount() != null) {
            KeystrokeUtilManager.processKeystrokes(keystrokeManager.getKeystrokeCount());
        }

        // store the activate event
        EventTrackerManager.getInstance().trackEditorAction("editor", "deactivate");

        asyncManager.destroyServices();
    }
}
