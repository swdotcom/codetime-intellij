package com.software.codetime.models;

public class FlowModeEditorSettings {
    public boolean autoEnterFlowMode = false;
    public boolean screenMode;
    public FlowModeScreenSettings vscode = new FlowModeScreenSettings();
    public FlowModeScreenSettings intellij = new FlowModeScreenSettings();
}
