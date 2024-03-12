package com.software.codetime.models;

import java.util.ArrayList;
import java.util.List;

public class User {
    public long id = 0;
    public int registered = 0;
    public String email = "";
    public List<PluginConnection> latest_plugin_connections = new ArrayList<>();
    public List<IntegrationConnection> integration_connections = new ArrayList<>();
    public String plugin_jwt = "";
    public UserPreferences preferences = new UserPreferences();
    public Profile profile = new Profile();
}
