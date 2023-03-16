package com.software.codetime.listeners;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.vfs.VirtualFile;

public class FileEditorListener implements FileEditorManagerListener {

    private final CodeTimeProcessor eventMgr = CodeTimeProcessor.getInstance();

    @Override
    public void fileOpened(FileEditorManager manager, VirtualFile file) {
        if (file == null || file.getPath() == null || manager.getProject() == null) {
            return;
        }

        eventMgr.handleFileOpenedEvents(file.getPath(), manager.getProject());
    }

    @Override
    public void fileClosed(FileEditorManager manager, VirtualFile file) {
        if (file == null || file.getPath() == null || manager.getProject() == null) {
            return;
        }

        eventMgr.handleFileClosedEvents(file.getPath(), manager.getProject());
    }

    @Override
    public void selectionChanged(FileEditorManagerEvent event) {
        if (event.getNewFile() != null && event.getManager() != null) {
            eventMgr.handleSelectionChangedEvents(event.getNewFile().getPath(), event.getManager().getProject());
        }
    }
}
