package com.software.codetime.models;

import com.software.codetime.managers.GitUtilManager;

public class Project {
    private String name;
    private String directory;
    private String identifier;
    private ResourceInfo resource = new ResourceInfo();
    private Object ideProject = null;

    public Project(String name, String directory) {
        this.name = name;
        this.directory = directory;
        ResourceInfo resourceInfo = GitUtilManager.getResourceInfo(directory);
        if (resourceInfo != null) {
            this.resource.setIdentifier(resourceInfo.getIdentifier());
            this.resource.setTag(resourceInfo.getTag());
            this.resource.setBranch(resourceInfo.getBranch());
            this.resource.setEmail(resourceInfo.getEmail());
            this.identifier = resourceInfo.getIdentifier();
        }

    }

    public Project cloneProject() {
        Project p = new Project(this.name, this.directory);
        p.setIdentifier(p.getIdentifier());
        if (this.resource != null) {
            p.setResource(this.resource.clone());
        }

        return p;
    }

    public Object getIdeProject() {
        return this.ideProject;
    }

    public void setIdeProject(Object project) {
        this.ideProject = project;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDirectory() {
        return this.directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public ResourceInfo getResource() {
        return this.resource;
    }

    public void setResource(ResourceInfo resource) {
        this.resource = resource;
    }

    public String toString() {
        return "KeystrokeProject{name='" + this.name + "', directory='" + this.directory + "'}";
    }
}
