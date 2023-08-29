package com.software.codetime.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ResourceInfo {
    private String identifier = "";
    private String branch = "";
    private String tag = "";
    private String email = "";
    private String repoName = "";
    private String ownerId = "";
    private List<ResourceTeamMember> members = new ArrayList();

    public ResourceInfo() {
    }

    public ResourceInfo clone() {
        ResourceInfo info = new ResourceInfo();
        info.setIdentifier(this.identifier);
        info.setBranch(this.branch);
        info.setTag(this.tag);
        info.setEmail(this.email);
        info.setMembers(this.members);
        return info;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getBranch() {
        return this.branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getTag() {
        return this.tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<ResourceTeamMember> getMembers() {
        return this.members;
    }

    public void setMembers(List<ResourceTeamMember> members) {
        this.members = members;
    }

    public String getRepoName() {
        return this.repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public JsonArray getJsonMembers() {
        JsonArray jsonMembers = new JsonArray();
        Iterator var2 = this.members.iterator();

        while(var2.hasNext()) {
            ResourceTeamMember member = (ResourceTeamMember)var2.next();
            JsonObject json = new JsonObject();
            json.addProperty("email", member.getEmail());
            json.addProperty("name", member.getName());
            jsonMembers.add(json);
        }

        return jsonMembers;
    }
}
