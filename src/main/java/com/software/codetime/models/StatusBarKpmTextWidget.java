package com.software.codetime.models;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import swdc.java.ops.manager.FileUtilManager;

import java.awt.event.MouseEvent;

public class StatusBarKpmTextWidget implements StatusBarWidget {

    public static final String KPM_TEXT_ID = "software.kpm.text";
    public static final String FLOW_TEXT_ID = "software.flow.text";

    private String msg = "";
    private String tooltip = "";
    private final String id;

    private final Consumer<MouseEvent> eventHandler;

    private final TextPresentation presentation = new StatusPresentation();

    public StatusBarKpmTextWidget(String id, final Runnable callback) {
        this.id = id;
        eventHandler = new Consumer<MouseEvent>() {
            @Override
            public void consume(MouseEvent mouseEvent) {
                if (callback != null) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        callback.run();
                    });
                }
            }
        };
    }

    public void setText(String msg) {
        this.msg = msg;
    }

    public void setTooltip(String tooltip) {
        String name = FileUtilManager.getItem("name");

        if (tooltip == null) {
            tooltip = "Code time today. Click to see more from Code Time.";
        }

        if (tooltip.lastIndexOf(".") != tooltip.length() - 1) {
            tooltip += ".";
        }

        if (name != null) {
            tooltip += " Logged in as " + name;
        }

        this.tooltip = tooltip;
    }

    class StatusPresentation implements StatusBarWidget.TextPresentation {

        @NotNull
        @Override
        public String getText() {
            return StatusBarKpmTextWidget.this.msg;
        }

        @Override
        public float getAlignment() {
            return 0;
        }

        @Nullable
        @Override
        public String getTooltipText() {
            return StatusBarKpmTextWidget.this.tooltip;
        }

        @Nullable
        @Override
        public Consumer<MouseEvent> getClickConsumer() {
            return eventHandler;
        }
    }

    @Nullable
    @Override
    public WidgetPresentation getPresentation() {
        return presentation;
    }

    @NotNull
    @Override
    public String ID() {
        return id;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
    }

    @Override
    public void dispose() {
    }
}
