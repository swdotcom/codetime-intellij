package com.software.codetime.models;

public class UserPreferences {
    public boolean showMusic;
    public boolean disableGitData;
    public UserNotifications notifications = new UserNotifications();
    public FlowMode flowMode = new FlowMode();
}
