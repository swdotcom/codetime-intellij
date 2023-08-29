package com.software.codetime.managers;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.software.codetime.models.CodeTime;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class KeystrokeUtilManager {
    public static void processKeystrokes(CodeTime keystrokeCountInfo) {
        try {
            if (keystrokeCountInfo.hasData()) {

                com.software.codetime.models.Project project = keystrokeCountInfo.getProject();

                // check to see if we need to find the main project if we don't have it
                if (project == null || StringUtils.isBlank(project.getDirectory()) ||
                        project.getDirectory().equals("Untitled")) {

                    Editor[] editors = EditorFactory.getInstance().getAllEditors();
                    if (editors != null) {
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
                }

                // end the file end times.
                preProcessKeystrokeData(keystrokeCountInfo);

                // send the event to the event tracker
                EventTrackerManager.getInstance().trackCodeTimeEvent(keystrokeCountInfo);

                UtilManager.TimesData timesData = UtilManager.getTimesData();
                // set the latest payload timestamp utc so help with session time calculations
                FileUtilManager.setNumericItem("latestPayloadTimestampEndUtc", timesData.now);
            }
        } catch (Exception e) {
        }

        keystrokeCountInfo.resetData();
    }

    // end un-ended file payloads and add the cumulative editor seconds
    public static void preProcessKeystrokeData(CodeTime keystrokeCountInfo) {

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
}
