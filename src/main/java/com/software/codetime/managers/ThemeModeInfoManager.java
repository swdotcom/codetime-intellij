package com.software.codetime.managers;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import swdc.java.ops.manager.ThemeModeInfoHandler;

public class ThemeModeInfoManager implements ThemeModeInfoHandler {
    @Override
    public boolean isLightMode() {
        return !EditorColorsManager.getInstance().isDarkEditor();
    }
}
