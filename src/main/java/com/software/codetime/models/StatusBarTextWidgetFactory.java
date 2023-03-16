package com.software.codetime.models;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.software.codetime.toolwindows.codetime.SidebarToolWindow;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class StatusBarTextWidgetFactory  implements StatusBarWidgetFactory {
    private StatusBarKpmTextWidget widget;

    @Override
    public @NonNls
    @NotNull
    String getId() {
        return StatusBarKpmTextWidget.KPM_TEXT_ID;
    }

    @Override
    public @Nls
    @NotNull String getDisplayName() {
        return "";
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarKpmTextWidget createWidget(@NotNull Project project) {
        this.widget = new StatusBarKpmTextWidget(StatusBarKpmTextWidget.KPM_TEXT_ID, () -> {
            SidebarToolWindow.openToolWindow();
        });
        return widget;
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        if (this.widget != null) {
            this.widget.dispose();
        }
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return false;
    }
}
