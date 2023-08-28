package com.software.codetime.managers;

import com.intellij.openapi.editor.colors.EditorColorsManager;

public class ThemeModeInfoManager implements ThemeModeInfoHandler {
    @Override
    public boolean isLightMode() {
        return !EditorColorsManager.getInstance().isDarkEditor();
    }
}
