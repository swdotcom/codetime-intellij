package com.software.codetime.models;

import com.software.codetime.snowplow.events.UIInteractionType;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang.StringUtils;

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
