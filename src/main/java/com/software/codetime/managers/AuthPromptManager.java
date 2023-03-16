package com.software.codetime.managers;

import org.apache.commons.lang.StringUtils;
import swdc.java.ops.manager.UtilManager;
import swdc.java.ops.snowplow.events.UIInteractionType;

import javax.swing.*;

public class AuthPromptManager {

    public static void initiateSwitchAccountFlow() {
        initiateAuthFlow("Switch account", "Switch to a different account?", false);
    }

    public static void initiateSignupFlow() {
        initiateAuthFlow("Sign up", "Sign up using...", true);
    }

    public static void initiateLoginFlow() {
        initiateAuthFlow("Log in", "Log in using...", false);
    }

    private static void initiateAuthFlow(String title, String message, boolean isSignup) {
        String[] options = new String[]{ "Google", "GitHub", "Email" };
        message = "\n " + message + " \n";
        Icon icon = UtilManager.getResourceIcon("app-icon-blue.png", AuthPromptManager.class.getClassLoader());
        String input = (String) JOptionPane.showInputDialog(
                null,
                message,
                title,
                JOptionPane.OK_CANCEL_OPTION,
                icon,
                options, // Array of choices
                options[0]); // Initial choice
        if (StringUtils.isNotBlank(input)) {
            UserSessionManager.launchLogin(input.toLowerCase(), UIInteractionType.click, isSignup);
        }
    }
}
