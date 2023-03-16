package com.software.codetime.listeners;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.software.codetime.main.Activator;
import org.jetbrains.annotations.NotNull;

public class ProjectActivateListener implements ProjectManagerListener {
    private static Project currentProject;

    @Override
    public void projectOpened(@NotNull Project project) {
        currentProject = project;
        ProjectManagerListener.super.projectOpened(project);
        Activator.getInstance();
    }

    public static Project getCurrentProject() {
        return currentProject;
    }
}
