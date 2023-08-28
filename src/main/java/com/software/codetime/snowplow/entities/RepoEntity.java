package com.software.codetime.snowplow.entities;

import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.software.codetime.managers.HashManager;

import java.util.HashMap;
import java.util.Map;

public class RepoEntity {
    // Repo related attributes
    public String repo_identifier = "";
    public String repo_name = "";
    public String owner_id = "";
    public String git_branch = "";
    public String git_tag = "";

    public SelfDescribingJson buildContext() {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("repo_identifier", HashManager.hashValue(this.repo_identifier, "repo_identifier"));
        eventData.put("repo_name", HashManager.hashValue(this.repo_name, "repo_name"));
        eventData.put("owner_id", HashManager.hashValue(this.owner_id, "owner_id"));
        eventData.put("git_branch", HashManager.hashValue(this.git_branch, "git_branch"));
        eventData.put("git_tag", HashManager.hashValue(this.git_tag, "git_tag"));
        return new SelfDescribingJson("iglu:com.software/repo/jsonschema/1-0-0", eventData);
    }
}
