package com.software.codetime.managers;

import com.intellij.openapi.project.Project;
import com.software.codetime.models.IntellijProject;

import java.io.File;

public class IntellijProjectManager {

    private static final IntellijProject intellijProject = new IntellijProject();

    public static Project getOpenProject() {
        swdc.java.ops.model.Project ctProject = intellijProject.getOpenProject();
        return (ctProject != null && ctProject.getIdeProject() != null)
                ? (Project) ctProject.getIdeProject()
                : null;
    }

    public static Project getFirstActiveProject() {
        swdc.java.ops.model.Project ctProject = intellijProject.getFirstActiveProject();
        return (ctProject != null && ctProject.getIdeProject() != null)
                ? (Project) ctProject.getIdeProject()
                : null;
    }

    public static swdc.java.ops.model.Project getFirstActiveCodeTimeProject() {
        return intellijProject.getFirstActiveProject();
    }

    public static Project getProjectForPath(String path) {
        swdc.java.ops.model.Project ctProject = intellijProject.getProjectForPath(path);
        return (ctProject != null && ctProject.getIdeProject() != null)
                ? (Project) ctProject.getIdeProject()
                : null;
    }

    public static String getFileSyntax(File f) {
        return intellijProject.getFileSyntax(f);
    }
}
