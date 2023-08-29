package com.software.codetime.managers;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.software.codetime.models.UserSessionManager;
import com.software.codetime.snowplow.events.UIInteractionType;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class ReadmeManager {
    public static void openReadmeFile(UIInteractionType interactionType) {
        ApplicationManager.getApplication().invokeLater(() -> {
            Project p = IntellijProjectManager.getOpenProject();
            if (p == null) {
                return;
            }

            ClassLoader classLoader = ReadmeManager.class.getClassLoader();
            String readmeFile = UserSessionManager.getReadmeFile();
            try (InputStream inputStream = classLoader.getResourceAsStream("assets/README.md")) {

                String fileContent = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
                File f = new File(readmeFile);
                if (!f.exists()) {
                    Writer writer = null;
                    // write the summary content
                    try {
                        writer = new BufferedWriter(new OutputStreamWriter(
                                new FileOutputStream(readmeFile), StandardCharsets.UTF_8));
                        writer.write(fileContent);
                    } catch (IOException ex) {
                        // Report
                    } finally {
                        try {
                            writer.close();
                        } catch (Exception ex) {/*ignore*/}
                    }
                }
                UtilManager.launchFile(readmeFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
