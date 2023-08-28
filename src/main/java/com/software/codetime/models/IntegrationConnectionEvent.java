package com.software.codetime.models;

public class IntegrationConnectionEvent {
    private int integration_type_id;
    private String integration_type = "";
    private String action = "";

    public int getIntegration_type_id() {
        return integration_type_id;
    }

    public void setIntegration_type_id(int integration_type_id) {
        this.integration_type_id = integration_type_id;
    }

    public String getIntegration_type() {
        return integration_type;
    }

    public void setIntegration_type(String integration_type) {
        this.integration_type = integration_type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
