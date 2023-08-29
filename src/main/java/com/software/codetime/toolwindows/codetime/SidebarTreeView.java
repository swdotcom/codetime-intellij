package com.software.codetime.toolwindows.codetime;

import com.software.codetime.main.StatusBarManager;
import com.software.codetime.managers.ConfigManager;
import com.software.codetime.managers.IntegrationManager;
import com.software.codetime.models.AuthPromptManager;
import com.software.codetime.models.FlowManager;
import com.software.codetime.snowplow.events.UIInteractionType;
import com.software.codetime.utils.FileUtilManager;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SidebarTreeView {

    private JPanel windowContent;
    private JPanel viewPanel;
    private JProgressBar progressBar;
    private JButton registerYourAccountButton;
    private JButton logInButton;
    private JButton readMoreOnGitHubButton;
    private JLabel stepOfLabel;
    private JLabel alreadyLabel;
    private JScrollPane onboardScrollPane;
    private JLabel gettingStartedLabel;
    private JLabel featuresTitleLabel;
    private JLabel featuresListLabel;
    private JScrollPane connectedScrollPane;
    private JButton enterFlowModeButton;
    private JLabel blockOutDistractionsLabel;
    private JLabel flowModeTitleLabel;
    private JLabel connectSlackToMuteLinkLabel;
    private JLabel dashboardLinkLabel;
    private JLabel projectReportLinkLabel;
    private JLabel moreAtSoftwareLinkLabel;
    private JLabel emailAccountLabel;
    private JLabel settingsLinkLabel;
    private JLabel toggleStatusLinkLabel;
    private JLabel documentationLinkLabel;
    private JLabel submitIssueLinkLabel;
    private JLabel manageSlackLinkLabel;
    private JLabel switchAccountsLinkLabel;

    private TreeViewState state = TreeViewState.REGISTER;

    private final Object lock = new Object();

    public enum TreeViewState {
        REGISTER, CONNECT_SLACK, CONNECTED
    }

    public SidebarTreeView() {
        synchronized (lock) {
            windowContent.setFocusable(true);
            this.init();
            this.showView();
            windowContent.setVisible(true);
        }
    }

    public JPanel getContent() {
        return windowContent;
    }

    public void refresh() {
        synchronized (lock) {
            this.showView();
        }
    }

    public void showView() {
        onboardScrollPane.setVisible(false);
        connectedScrollPane.setVisible(false);
        if (StringUtils.isBlank(FileUtilManager.getItem("name"))) {
            this.showStepOneView();
        } else if (StringUtils.isBlank(FileUtilManager.getItem("intellij_CtskipSlackConnect"))) {
            this.showStepTwoView();
        } else {
            this.showConnectedView();
        }
        windowContent.updateUI();
        windowContent.validate();
    }

    private void init() {
        this.addListeners();
    }

    private void showStepOneView() {
        state = TreeViewState.REGISTER;
        progressBar.setValue(40);
        stepOfLabel.setText("1 of 2");
        registerYourAccountButton.setText("Register your account");
        alreadyLabel.setText("Already have an account?");
        logInButton.setText("Log in");
        onboardScrollPane.setVisible(true);
    }

    private void showStepTwoView() {
        state = TreeViewState.CONNECT_SLACK;
        progressBar.setValue(70);
        stepOfLabel.setText("2 of 2");
        registerYourAccountButton.setText("Connect a Slack workspace");
        alreadyLabel.setText("Not using Slack?");
        logInButton.setText("Skip this step");
        onboardScrollPane.setVisible(true);
    }

    private void showConnectedView() {
        state = TreeViewState.CONNECTED;
        emailAccountLabel.setText(FileUtilManager.getItem("name"));
        if (FileUtilManager.getFlowChangeState()) {
            enterFlowModeButton.setText("Exit Flow Mode");
        } else {
            enterFlowModeButton.setText("Enter Flow Mode");
        }
        String txt = "See how Slack helps your flow";
        List integrations = IntegrationManager.getSlackIntegrations();
        if (CollectionUtils.isNotEmpty(integrations)) {
            connectSlackToMuteLinkLabel.setVisible(false);
            txt = integrations.size() + "";
            txt += integrations.size() == 1 ? " workspace connected" : " workspaces connected";
        } else {
            connectSlackToMuteLinkLabel.setVisible(true);
        }
        manageSlackLinkLabel.setText("<html><a href='#' style='padding-left: 2px'>Manage Slack</a><br><span style='padding-left: 4px'>"+txt+"</span></html>");
        connectedScrollPane.setVisible(true);
    }

    private void addListeners() {
        SidebarTreeView treeView = this;
        registerYourAccountButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (state == TreeViewState.CONNECT_SLACK) {
                    // connect a slack workspace
                    UtilManager.launchUrl(ConfigManager.app_url + "/data_sources/integration_types/slack");
                } else {
                    AuthPromptManager.initiateSignupFlow();
                }
            }
        });

        logInButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (state == TreeViewState.CONNECT_SLACK) {
                    // skip this step action
                    FileUtilManager.setBooleanItem("intellij_CtskipSlackConnect", true);
                    treeView.refresh();
                } else {
                    AuthPromptManager.initiateLoginFlow();
                }
            }
        });

        readMoreOnGitHubButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl("https://www.software.com/src/auto-flow-mode");
            }
        });

        enterFlowModeButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                FlowManager.toggleFlowMode(false);
            }
        });

        dashboardLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl(ConfigManager.app_url + "/dashboard/code_time");
            }
        });

        projectReportLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl(ConfigManager.app_url + "/reports");
            }
        });

        moreAtSoftwareLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl(ConfigManager.app_url);
            }
        });

        settingsLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl(ConfigManager.app_url + "/preferences");
            }
        });

        toggleStatusLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                StatusBarManager.toggleStatusBar(UIInteractionType.click);
            }
        });

        documentationLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl("https://github.com/swdotcom/intellij-codetime");
            }
        });

        submitIssueLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.submitIntellijIssue();
            }
        });

        manageSlackLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                UtilManager.launchUrl(ConfigManager.app_url + "/data_sources/integration_types/slack");
            }
        });
        switchAccountsLinkLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                AuthPromptManager.initiateSwitchAccountFlow();
            }
        });
    }
}
