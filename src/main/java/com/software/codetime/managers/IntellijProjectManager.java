package com.software.codetime.managers;

import com.intellij.openapi.project.Project;
import com.software.codetime.models.IntellijProject;

import java.io.File;

public class IntellijProjectManager {
    private static final IntellijProject intellijProject = new IntellijProject();

    public static Project getOpenProject() {
        com.software.codetime.models.Project ctProject = intellijProject.getOpenProject();
        return (ctProject != null && ctProject.getIdeProject() != null)
                ? (Project) ctProject.getIdeProject()
                : null;
    }

    public static Project getFirstActiveProject() {
        com.software.codetime.models.Project ctProject = intellijProject.getFirstActiveProject();
        return (ctProject != null && ctProject.getIdeProject() != null)
                ? (Project) ctProject.getIdeProject()
                : null;
    }

    public static com.software.codetime.models.Project getFirstActiveCodeTimeProject() {
        return intellijProject.getFirstActiveProject();
    }

    public static Project getProjectForPath(String path) {
        com.software.codetime.models.Project ctProject = intellijProject.getProjectForPath(path);
        return (ctProject != null && ctProject.getIdeProject() != null)
                ? (Project) ctProject.getIdeProject()
                : null;
    }

    public static String getFileSyntax(File f) {
        return intellijProject.getFileSyntax(f);
    }
}
