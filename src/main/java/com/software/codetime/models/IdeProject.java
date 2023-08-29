package com.software.codetime.models;

import java.io.File;

public interface IdeProject {
    Project getProjectForPath(String path);

    Project getFirstActiveProject();

    Project getOpenProject();

    Project buildKeystrokeProject(Object p);

    String getFileSyntax(File f);
}
