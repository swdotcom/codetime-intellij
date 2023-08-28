package com.software.codetime.managers;

import com.software.codetime.models.IntegrationConnection;
import com.software.codetime.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class IntegrationManager {
    public static final Logger LOG = Logger.getLogger("IntegrationManager");

    private static List<IntegrationConnection> integrations = null;

    public static List<IntegrationConnection> getSlackIntegrations() {
        List<IntegrationConnection> slackIntegrations = new ArrayList<>();
        User user = AccountManager.getCachedUser();
        if (user != null) {
            integrations = user.integration_connections;
            if (integrations != null && integrations.size() > 0) {
                for (IntegrationConnection integration : integrations) {
                    if (integration.status.equalsIgnoreCase("active") && integration.integration_type_id == 14) {
                        slackIntegrations.add(integration);
                    }
                }
            }
        }
        return slackIntegrations;
    }
}
