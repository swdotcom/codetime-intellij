package com.software.codetime.managers;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.software.codetime.models.CodeTime;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class KeystrokeUtilManager {
    public static final Logger LOG = Logger.getLogger("KeystrokeUtilManager");

    public static void processKeystrokes(CodeTime keystrokeCountInfo) {
        try {
            if (keystrokeCountInfo.hasData()) {
                final CodeTime keystrokeData = keystrokeCountInfo.clone();
                // execute async
                AsyncManager.getInstance().executeOnceInSeconds(() -> {
                    processKeystrokesHandler(keystrokeData);
                }, 0);
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error processing keystrokes: " + e.getMessage());
        }

        keystrokeCountInfo.resetData();
    }

    // end un-ended file payloads and add the cumulative editor seconds
    private static void preProcessKeystrokeData(CodeTime keystrokeCountInfo) {
        UtilManager.TimesData timesData = UtilManager.getTimesData();
        Map<String, CodeTime.FileInfo> fileInfoDataSet = keystrokeCountInfo.getSource();
        for ( CodeTime.FileInfo fileInfoData : fileInfoDataSet.values() ) {
            // end the ones that don't have an end time
            if (fileInfoData.end == 0) {
                // set the end time for this file
                fileInfoData.end = timesData.now;
                fileInfoData.local_end = timesData.local_now;
            }
        }
    }

    private static void processKeystrokesHandler(final CodeTime keystrokeData) {
        com.software.codetime.models.Project project = keystrokeData.getProject();

        // check to see if we need to find the main project if we don't have it
        if (project == null || StringUtils.isBlank(project.getDirectory()) ||
                project.getDirectory().equals("Untitled")) {

            Editor[] editors = EditorFactory.getInstance().getAllEditors();
            for (Editor editor : editors) {
                Project editorProject = editor.getProject();
                // update the code time project dir info
                if (editorProject != null && StringUtils.isNotBlank(editorProject.getName())) {

                    String projDir = editorProject.getProjectFilePath();
                    String projName = editorProject.getName();
                    if (project == null) {
                        project = new com.software.codetime.models.Project(projName, projDir);
                    } else {
                        project.setDirectory(projDir);
                        project.setName(projName);
                    }
                    break;
                }
            }
        }

        // end the file end times.
        preProcessKeystrokeData(keystrokeData);

        // send the event to the event tracker
        EventTrackerManager.getInstance().trackCodeTimeEvent(keystrokeData);

        UtilManager.TimesData timesData = UtilManager.getTimesData();
        // set the latest payload timestamp utc so help with session time calculations
        FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
    }
}
