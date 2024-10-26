package com.software.codetime.listeners;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.startup.ProjectActivity;
import com.software.codetime.main.Activator;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectActivateListener implements ProjectActivity {
    private static Project currentProject;

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        Activator.getInstance();
        currentProject = project;
        return currentProject;
    }

    public static Project getCurrentProject() {
        if (currentProject == null) {
            Editor[] editors = EditorFactory.getInstance().getAllEditors();
            if (editors != null && editors.length > 0) {
                currentProject = editors[0].getProject();
            }
        }
        return currentProject;
    }
}
