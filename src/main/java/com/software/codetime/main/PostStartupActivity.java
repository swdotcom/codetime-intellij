package com.software.codetime.main;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginInstaller;
import com.intellij.ide.plugins.PluginStateListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class PostStartupActivity implements StartupActivity {

    public static final Logger log = Logger.getLogger("PluginPostStartupActivity");

    @Override
    public void runActivity(@NotNull Project project) {

        PluginInstaller.addStateListener(new PluginStateListener() {

            @Override
            public void install(@NotNull IdeaPluginDescriptor ideaPluginDescriptor) {
                log.info("Installing Code Time");
            }

            @Override
            public void uninstall(@NotNull IdeaPluginDescriptor ideaPluginDescriptor) {
                // send a quick update to the app to delete the integration
                log.info("Uninstalling Code Time");
            }
        });
    }
}
