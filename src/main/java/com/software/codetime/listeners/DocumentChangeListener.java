package com.software.codetime.listeners;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;

public class DocumentChangeListener implements DocumentListener {

    private final CodeTimeProcessor eventMgr = CodeTimeProcessor.getInstance();

    @Override
    public void beforeDocumentChange(DocumentEvent documentEvent) {
        //
    }

    @Override
    public void documentChanged(DocumentEvent documentEvent) {
        Document document = documentEvent.getDocument();
        eventMgr.handleChangeEvents(document, documentEvent);
    }
}
