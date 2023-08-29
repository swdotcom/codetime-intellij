package com.software.codetime.websockets.handlers;

import com.software.codetime.events.UserStateChangeModel;
import com.software.codetime.managers.AccountManager;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.models.User;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import com.software.codetime.websockets.WebsocketClient;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.util.logging.Logger;

public class AuthenticatedPluginUser {
    public static final Logger LOG = Logger.getLogger("AuthenticatedPluginUser");

    public static void handleAuthenticatedPluginUser(User user) {
        if (user.registered == 1) {
            String existingJwt = FileUtilManager.getItem("jwt");
            if (StringUtils.isNotBlank(user.plugin_jwt)) {
                FileUtilManager.setItem("jwt", user.plugin_jwt);
            }
            FileUtilManager.setItem("name", user.email);

            // prompt they've completed the setup
            SwingUtilities.invokeLater(() -> {
                String infoMsg = "\n Successfully logged onto CodeTime \n";
                if (ConfigManager.plugin_name.toLowerCase().indexOf("code") == -1) {
                    infoMsg = "\n Successfully logged onto EditorOps \n";
                }
                Object[] options = {"OK"};
                JTextPane jtp = new JTextPane();
                Document doc = jtp.getDocument();
                try {
                    doc.insertString(doc.getLength(), infoMsg, new SimpleAttributeSet());
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                jtp.setSize(new Dimension(500, 225));
                JOptionPane.showOptionDialog(
                        null, jtp, "Authentication", JOptionPane.OK_OPTION,
                        JOptionPane.INFORMATION_MESSAGE, UtilManager.getResourceIcon("app-icon-blue.png", null), options, options[0]);
            });

            // clear the auth callback state
            FileUtilManager.setBooleanItem("switching_account", false);
            FileUtilManager.setAuthCallbackState(null);

            // send the user change event
            UserStateChangeModel changeModel = new UserStateChangeModel();
            changeModel.dispatchAuthenticationCompletion();

            AccountManager.getUser();

            if (ConfigManager.tree_refresh_runnable != null) {
                SwingUtilities.invokeLater(() -> {
                    ConfigManager.tree_refresh_runnable.run();
                });
            }

            if (ConfigManager.ws_msg_handler != null) {
                SwingUtilities.invokeLater(() -> {
                    ConfigManager.ws_msg_handler.handlePostAuthenticatedPluginUser(user);
                });
            }

            // re-initialize the websocket since it's a successful auth completion to ensure
            // the publisher knows about the ID correctly
            if (!existingJwt.equals(user.plugin_jwt)) {
                try {
                    WebsocketClient.reConnect();
                } catch (Exception e) {
                    LOG.warning("Websocket connect error: " + e.getMessage());
                }
            }
        }
    }
}
