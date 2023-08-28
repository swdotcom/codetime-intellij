package com.software.codetime.models;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class StatusBarFlowIconWidgetFactory implements StatusBarWidgetFactory {
    private StatusBarKpmIconWidget widget;

    @Override
    public @NonNls
    @NotNull String getId() {
        return StatusBarKpmIconWidget.FLOW_ICON_ID;
    }

    @Override
    public @Nls
    @NotNull String getDisplayName() {
        return "";
    }

    @Override
    public boolean isAvailable(@NotNull com.intellij.openapi.project.Project project) {
        return true;
    }

    @Override
    public @NotNull StatusBarKpmIconWidget createWidget(@NotNull Project project) {
        this.widget = new StatusBarKpmIconWidget(StatusBarKpmIconWidget.FLOW_ICON_ID, "open-circle.png", () -> {
            FlowManager.toggleFlowMode(false);
        });
        return this.widget;
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
