package com.software.codetime.listeners;

import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;
import swdc.java.ops.manager.EventTrackerManager;
import swdc.java.ops.manager.GitEventsManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentSaveListener implements BulkFileListener {
    private static final GitEventsManager gitEvtMgr = new GitEventsManager();
    private static final Map<String, Long> lastChangeMap = new HashMap<>();

    @Override
    public void before(@NotNull List<? extends VFileEvent > events) {

    }

    @Override
    public void after(@NotNull List<? extends VFileEvent> events) {
        for (VFileEvent e : events) {
            if (e.isFromSave() && e instanceof VFileContentChangeEvent) {
                VFileContentChangeEvent event = (VFileContentChangeEvent)e;

                String filePath = e.getPath();
                Long lastTimestamp = lastChangeMap.get(filePath);
                if (lastTimestamp == null || lastTimestamp != event.getModificationStamp()) {
                    // update the map and process it
                    lastChangeMap.put(filePath, event.getModificationStamp());

                    EventTrackerManager.getInstance().trackEditorAction("file", "save", filePath);
                    gitEvtMgr.trackUncommittedChanges(filePath);
                }
            }
        }
    }
}
