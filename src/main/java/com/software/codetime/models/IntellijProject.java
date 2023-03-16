package com.software.codetime.models;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.model.Project;
import swdc.java.ops.providers.IdeProject;

import java.io.File;

public class IntellijProject implements IdeProject {
    @Override
    public Project getProjectForPath(String path) {
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        if (editors != null && editors.length > 0) {
            for (Editor editor : editors) {
                if (editor != null && editor.getProject() != null) {
                    String basePath = editor.getProject().getBasePath();
                    if (path.indexOf(basePath) != -1) {
                        return buildKeystrokeProject(editor.getProject());
                    }
                }
            }
        } else {
            com.intellij.openapi.project.Project[] projects = ProjectManager.getInstance().getOpenProjects();
            if (projects != null && projects.length > 0) {
                return buildKeystrokeProject(projects[0]);
            }
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

    @Override
    public Project getFirstActiveProject() {
        com.intellij.openapi.project.Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects != null && projects.length > 0) {
            return buildKeystrokeProject(projects[0]);
        }
        Editor[] editors = EditorFactory.getInstance().getAllEditors();
        if (editors != null && editors.length > 0) {
            return buildKeystrokeProject(editors[0].getProject());
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

    @Override
    public Project getOpenProject() {
        ProjectManager projMgr = ProjectManager.getInstance();
        com.intellij.openapi.project.Project[] projects = projMgr.getOpenProjects();
        if (projects != null && projects.length > 0) {
            return buildKeystrokeProject(projects[0]);
        }
        return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
    }

    @Override
    public Project buildKeystrokeProject(Object p) {
        if (p == null) {
            return new Project(UtilManager.unnamed_project_name, UtilManager.untitled_file_name);
        }
        com.intellij.openapi.project.Project intellijProj = (com.intellij.openapi.project.Project)p;
        Project keystrokeProject = new Project(intellijProj.getName(), intellijProj.getBasePath());
        keystrokeProject.setIdeProject(p);
        return keystrokeProject;
    }

    public String getFileSyntax(File f) {
        VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(f);
        if (vFile != null) {
            return vFile.getFileType().getName();
        }
        String fullFileName = f.getAbsolutePath();
        return (fullFileName.contains(".")) ? fullFileName.substring(fullFileName.lastIndexOf(".") + 1) : "";
    }
}

