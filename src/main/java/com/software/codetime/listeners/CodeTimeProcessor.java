package com.software.codetime.listeners;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.software.codetime.managers.AsyncManager;
import com.software.codetime.managers.EventTrackerManager;
import com.software.codetime.managers.KeystrokeUtilManager;
import com.software.codetime.models.CodeTime;
import com.software.codetime.models.KeystrokeWrapper;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import java.awt.*;
import java.awt.datatransfer.*;

public class CodeTimeProcessor {
    public static final Logger LOG = Logger.getLogger("CodeTimeProcessor");

    private static CodeTimeProcessor instance = null;

    private static final int FOCUS_STATE_INTERVAL_SECONDS = 5;
    private static final int AI_COMPLETION_MIN_CHARS = 3;
    private static final Pattern NEW_LINE_TAB_PATTERN = Pattern.compile("\n\t");
    private static final Pattern TAB_PATTERN = Pattern.compile("\t");

    public static boolean isCurrentlyActive = true;
    private static final long LAST_CLIPBOARD_CHECK_MILLIS = 1000 * 10; // 10 seconds
    private static long lastClipboardCheckTime = 0;

    private final EventTrackerManager tracker;
    private final KeystrokeWrapper keystrokeMgr;

    public static CodeTimeProcessor getInstance() {
        if (instance == null) {
            instance = new CodeTimeProcessor();
        }
        return instance;
    }

    private CodeTimeProcessor() {
        keystrokeMgr = KeystrokeWrapper.getInstance();
        tracker = EventTrackerManager.getInstance();
        AsyncManager asyncManager = AsyncManager.getInstance();

        final Runnable checkFocusStateTimer = () -> checkFocusState();
        asyncManager.scheduleService(
                checkFocusStateTimer, "checkFocusStateTimer", 0, FOCUS_STATE_INTERVAL_SECONDS);
    }

    private void checkFocusState() {
        ApplicationManager.getApplication().invokeLater(() -> {
            boolean isActive = ApplicationManager.getApplication().isActive();
            if (isActive != isCurrentlyActive) {
                if (!isActive) {
                    CodeTime keystrokeCount = keystrokeMgr.getKeystrokeCount();
                    if (keystrokeCount != null) {
                        // set the flag the "unfocusStateChangeHandler" will look for in order to process payloads early
                        KeystrokeUtilManager.processKeystrokes(keystrokeCount);
                    }
                    EventTrackerManager.getInstance().trackEditorAction("editor", "unfocus");
                } else {
                    // just set the process keystrokes payload to false since we're focused again
                    EventTrackerManager.getInstance().trackEditorAction("editor", "focus");
                }

                // update the currently active flag
                isCurrentlyActive = isActive;
            }
        });
    }

    public static int getNewlineCount(String text){
        if (StringUtils.isBlank(text)) {
            return 0;
        }
        return text.split("[\n|\r]").length;
    }

    private boolean hasTabAndWhitespaceBeforeAlphanumeric(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return text.matches(".*[\\t\\s]+[a-zA-Z0-9].*");
    }

    private int getAICharactersDeletedFromContentChange(int rawTextLength, int sanitizedTextLength, int rangeLength) {
        if (sanitizedTextLength > AI_COMPLETION_MIN_CHARS && sanitizedTextLength < rangeLength) {
            return rangeLength - rawTextLength;
        }
        return 0;
    }

    private boolean containsNewline(String text) {
        if (StringUtils.isBlank(text)) {
            return false;
        }
        return text.contains("\n") || text.contains("\r");
    }

    private int getAlphanumericCharacterCount(String text) {
        if (StringUtils.isBlank(text)) {
            return 0;
        }
        return (int) text.chars()
                .filter(Character::isLetterOrDigit)
                .count();
    }

    private boolean isTextInClipboard(String text) {
        if (StringUtils.isBlank(text.trim())) {
            return false;
        }

        long currentTime = new Date().getTime();
        if (lastClipboardCheckTime + LAST_CLIPBOARD_CHECK_MILLIS < currentTime) {
            lastClipboardCheckTime = currentTime;
            try {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable contents = clipboard.getContents(null);

                if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String clipboardText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    return clipboardText.contains(text);
                }
            } catch (Exception e) {
                LOG.warning("Failed to access clipboard: " + e.getMessage());
            }
        }
        return false;
    }

    private CodeTime getCurrentKeystrokeCount(String projectName, String projectDir) {
        CodeTime keystrokeCount = keystrokeMgr.getKeystrokeCount();
        if (keystrokeCount == null) {
            // create one
            projectName = projectName != null && !projectName.equals("") ? projectName : UtilManager.unnamed_project_name;
            projectDir = projectDir != null && !projectDir.equals("") ? projectDir : UtilManager.untitled_file_name;
            // create the keystroke count wrapper
            createKeystrokeCountWrapper(projectName, projectDir);

            // now retrieve it from the mgr
            keystrokeCount = keystrokeMgr.getKeystrokeCount();
        }
        return keystrokeCount;
    }

    private void updateFileInfoMetrics(Document document, DocumentEvent documentEvent, CodeTime.FileInfo fileInfo, CodeTime keystrokeCount) {

        String text = documentEvent.getNewFragment().toString();
        String oldText = documentEvent.getOldFragment().toString();

        int newLineCount = document.getLineCount();
        fileInfo.length = document.getTextLength();

        // this will tell us delete chars
        int alphaNumericCharsDeleted = 0;
        if (documentEvent.getOldLength() > 0 && documentEvent.getNewLength() == 0) {
            alphaNumericCharsDeleted = getAlphanumericCharacterCount(oldText);
        }

        // count the newline chars
        int linesAdded = getNewlineCount(text);
        int linesRemoved = getNewlineCount(oldText);

        boolean hasAutoCompleteCharacters = hasTabAndWhitespaceBeforeAlphanumeric(text);
        boolean isInClipboard = isTextInClipboard(text);
        int alphaNumericCharsAdded = getAlphanumericCharacterCount(text);
        int rangeLength = (alphaNumericCharsDeleted > 0 && alphaNumericCharsAdded > 0) ? alphaNumericCharsDeleted : 0;
        int aiCharactersDeleted = getAICharactersDeletedFromContentChange(text.length(), alphaNumericCharsAdded, rangeLength);
        boolean isAiCompletion = hasAutoCompleteCharacters && !isInClipboard && alphaNumericCharsAdded > AI_COMPLETION_MIN_CHARS;

        if (text.matches(".*[\\t]+") && !isAiCompletion) {
            fileInfo.auto_indents += 1;
        }

        // deletes
        updateLinesAndCharactersRemoved(alphaNumericCharsDeleted, alphaNumericCharsAdded, linesRemoved, aiCharactersDeleted, fileInfo);

        // adds
        updateLinesAndCharactersAdded(isAiCompletion, isInClipboard, linesAdded, alphaNumericCharsAdded, fileInfo);

        fileInfo.lines = newLineCount;
        fileInfo.keystrokes += 1;
        keystrokeCount.keystrokes += 1;
    }

    private void updateLinesAndCharactersAdded(boolean isAiCompletion, boolean isInClipboard, int linesAdded, int alphaNumericCharsAdded, CodeTime.FileInfo fileInfo) {
        if (isAiCompletion) {
            fileInfo.ai_lines_added += linesAdded;
            fileInfo.ai_characters_added += alphaNumericCharsAdded;
        } else {
            fileInfo.lines_added += linesAdded;
            fileInfo.characters_added += alphaNumericCharsAdded;
        }

        if (isInClipboard) {
            fileInfo.paste += 1;
        }

        if (alphaNumericCharsAdded == 1) {
            // it's a single keystroke action (single_adds)
            fileInfo.add += 1;
            fileInfo.single_adds += 1;
        } else if (linesAdded == 1) {
            fileInfo.single_adds += 1;
        } else if (linesAdded > 1) {
            fileInfo.multi_adds += 1;
            fileInfo.is_net_change = true;
        }
    }

    private void updateLinesAndCharactersRemoved(int alphaNumericCharsDeleted, int alphaNumericCharsAdded, int linesRemoved, int aiCharactersDeleted, CodeTime.FileInfo fileInfo) {
        if (aiCharactersDeleted > 0) {
            fileInfo.ai_characters_reverted += aiCharactersDeleted;
            fileInfo.ai_lines_reverted += linesRemoved;
        } else {
            fileInfo.characters_deleted += alphaNumericCharsDeleted;
            fileInfo.lines_removed += linesRemoved;
        }
        if (alphaNumericCharsDeleted > 0 && alphaNumericCharsAdded > 0) {
            // it's a replacement
            fileInfo.replacements += 1;
        } else if (linesRemoved == 1) {
            // it's a single line deletion
            fileInfo.lines_removed += 1;
            fileInfo.single_deletes += 1;
        } else if (linesRemoved > 1) {
            // it's a multi line deletion and may contain characters
            fileInfo.multi_deletes += 1;
            fileInfo.is_net_change = true;
        } else if (alphaNumericCharsDeleted == 1) {
            // it's a single character deletion action
            fileInfo.delete += 1;
            fileInfo.single_deletes += 1;
        } else if (alphaNumericCharsDeleted > 1) {
            // it's a multi character deletion action
            fileInfo.multi_deletes += 1;
            fileInfo.is_net_change = true;
        }
    }

    // this is used to close unended files
    public void handleSelectionChangedEvents(String fileName, Project project) {
        CodeTime keystrokeCount =
                getCurrentKeystrokeCount(project.getName(), project.getProjectFilePath());

        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        keystrokeCount.endPreviousModifiedFiles(fileName);
    }

    public void handleFileOpenedEvents(String fileName, Project project) {
        CodeTime keystrokeCount =
                getCurrentKeystrokeCount(project.getName(), project.getProjectFilePath());

        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        keystrokeCount.endPreviousModifiedFiles(fileName);
        fileInfo.open = fileInfo.open + 1;
        LOG.info("Code Time: file opened: " + fileName);
    }

    public void handleFileClosedEvents(String fileName, Project project) {
        CodeTime keystrokeCount =
                getCurrentKeystrokeCount(project.getName(), project.getProjectFilePath());
        CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
        if (fileInfo == null) {
            return;
        }
        fileInfo.close = fileInfo.close + 1;
        LOG.info("Code Time: file closed: " + fileName);
        if (tracker != null) {
            tracker.trackEditorAction("file", "close", fileName);
        }
    }

    /**
     * Handles character change events in a file
     * @param document
     * @param documentEvent
     */
    public void handleChangeEvents(Document document, DocumentEvent documentEvent) {

        if (document == null) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {

            FileDocumentManager instance = FileDocumentManager.getInstance();

            VirtualFile file = instance.getFile(document);
            if (file == null) {
                return;
            }

            if (!file.isDirectory()) {
                Editor[] editors = EditorFactory.getInstance().getEditors(document);
                if (editors.length > 0) {
                    String fileName = file.getPath();
                    Project project = editors[0].getProject();

                    if (project != null) {

                        // get the current keystroke count obj
                        CodeTime keystrokeCount = getCurrentKeystrokeCount(project.getName(), project.getProjectFilePath());

                        // check whether it's a code time file or not
                        // .*\.software.*(data\.json|session\.json|latestKeystrokes\.json|ProjectContributorCodeSummary\.txt|CodeTime\.txt|SummaryInfo\.txt|events\.json|fileChangeSummary\.json)
                        boolean skip = fileName.matches(".*\\.software.*(data\\.json|session\\.json|latestKeystrokes\\.json|ProjectContributorCodeSummary\\.txt|CodeTime\\.txt|SummaryInfo\\.txt|events\\.json|fileChangeSummary\\.json)");

                        if (!skip && keystrokeCount != null) {

                            CodeTime.FileInfo fileInfo = keystrokeCount.getSourceByFileName(fileName);
                            if (StringUtils.isBlank(fileInfo.syntax)) {
                                // get the grammar
                                try {
                                    String fileType = file.getFileType().getName();
                                    if (!fileType.isEmpty()) {
                                        fileInfo.syntax = fileType;
                                    }
                                } catch (Exception e) {}
                            }

                            updateFileInfoMetrics(document, documentEvent, fileInfo, keystrokeCount);
                        }
                    }
                }

            }
        });
    }

    public void createKeystrokeCountWrapper(String projectName, String projectFilepath) {
        //
        // Create one since it hasn't been created yet
        // and set the start time (in seconds)
        //
        CodeTime keystrokeCount = new CodeTime();

        com.software.codetime.models.Project keystrokeProject = new com.software.codetime.models.Project( projectName, projectFilepath );
        keystrokeCount.setProject( keystrokeProject );

        //
        // Update the manager with the newly created KeystrokeCount object
        //
        keystrokeMgr.setKeystrokeCount(projectName, keystrokeCount);
    }
}
