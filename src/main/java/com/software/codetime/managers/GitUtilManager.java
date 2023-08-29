package com.software.codetime.managers;

import com.software.codetime.models.*;
import com.software.codetime.utils.UtilManager;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GitUtilManager {
    public static CommitChangeStats accumulateStatChanges(List<String> results, boolean committedChanges) {
        CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

        if (results != null) {
            for (String line : results) {
                line = line.trim();
                if (line.indexOf("changed") != -1 &&
                        (line.indexOf("insertion") != -1 || line.indexOf("deletion") != -1)) {
                    String[] parts = line.split(" ");
                    // the 1st element is the number of files changed
                    int fileCount = Integer.parseInt(parts[0]);
                    changeStats.setFileCount(fileCount);
                    changeStats.setCommitCount(changeStats.getCommitCount() + 1);
                    for (int x = 1; x < parts.length; x++) {
                        String part = parts[x];
                        if (part.indexOf("insertion") != -1) {
                            int insertions = Integer.parseInt(parts[x - 1]);
                            changeStats.setInsertions(changeStats.getInsertions() + insertions);
                        } else if (part.indexOf("deletion") != -1) {
                            int deletions = Integer.parseInt(parts[x - 1]);
                            changeStats.setDeletions(changeStats.getDeletions() + deletions);
                        }
                    }
                }
            }
        }

        return changeStats;
    }

    public static CommitChangeStats getChangeStats(String[] cmdList, String projectDir, boolean committedChanges) {
        CommitChangeStats changeStats = new CommitChangeStats(committedChanges);

        if (!UtilManager.isGitProject(projectDir)) {
            return changeStats;
        }

        /**
         * example:
         * -mbp-2:swdc-vscode xavierluiz$ git diff --stat
         lib/KpmProviderManager.ts | 22 ++++++++++++++++++++--
         1 file changed, 20 insertions(+), 2 deletions(-)

         for multiple files it will look like this...
         7 files changed, 137 insertions(+), 55 deletions(-)
         */
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);

        if (resultList == null || resultList.size() == 0) {
            // something went wrong, but don't try to parse a null or undefined str
            return changeStats;
        }

        // just look for the line with "insertions" and "deletions"
        changeStats = accumulateStatChanges(resultList, committedChanges);

        return changeStats;
    }

    public static CommitChangeStats getUncommitedChanges(String projectDir) {
        CommitChangeStats changeStats = new CommitChangeStats(false);

        if (!UtilManager.isGitProject(projectDir)) {
            return changeStats;
        }

        String[] cmdList = {"git", "diff", "--stat"};

        return getChangeStats(cmdList, projectDir, false);
    }

    // get the git resource config information
    public static ResourceInfo getResourceInfo(String projectDir) {
        ResourceInfo resourceInfo = new ResourceInfo();

        // is the project dir avail?
        if (UtilManager.isGitProject(projectDir)) {
            try {
                String[] branchCmd = { "git", "symbolic-ref", "--short", "HEAD" };
                String branch = UtilManager.runCommand(branchCmd, projectDir);

                String[] identifierCmd = { "git", "config", "--get", "remote.origin.url" };
                String identifier = UtilManager.runCommand(identifierCmd, projectDir);

                String[] emailCmd = { "git", "config", "user.email" };
                String email = UtilManager.runCommand(emailCmd, projectDir);

                String[] tagCmd = { "git", "describe", "--all" };
                String tag = UtilManager.runCommand(tagCmd, projectDir);

                if (StringUtils.isNotBlank(branch) && StringUtils.isNotBlank(identifier)) {
                    resourceInfo.setBranch(branch);
                    resourceInfo.setTag(tag);
                    resourceInfo.setEmail(email);
                    resourceInfo.setIdentifier(identifier);

                    // get the ownerId and repoName out of the identifier
                    String[] parts = identifier.split("/");
                    if (parts.length > 2) {
                        // get the last part
                        String repoNamePart = parts[parts.length - 1];
                        int typeIdx = repoNamePart.indexOf(".git");
                        if (typeIdx != -1) {
                            // it's a git identifier AND it has enough parts
                            // to get the repo name and owner id
                            resourceInfo.setRepoName(repoNamePart.substring(0, typeIdx));
                            resourceInfo.setOwnerId(parts[parts.length - 2]);
                        }
                    }
                }

            } catch (Exception e) {
                //
            }
        }

        return resourceInfo;
    }

    public static String getUsersEmail(String projectDir) {
        if (!UtilManager.isGitProject(projectDir)) {
            return "";
        }
        String[] emailCmd = { "git", "config", "user.email" };
        String email = UtilManager.runCommand(emailCmd, projectDir);
        return email;
    }

    public static CommitChangeStats getTodaysCommits(String projectDir, String email) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (!UtilManager.isGitProject(projectDir)) {
            return changeStats;
        }

        return getCommitsForRange("today", projectDir, email);
    }

    public static CommitChangeStats getYesterdaysCommits(String projectDir, String email) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (!UtilManager.isGitProject(projectDir)) {
            return changeStats;
        }

        return getCommitsForRange("yesterday", projectDir, email);
    }

    public static CommitChangeStats getThisWeeksCommits(String projectDir, String email) {
        CommitChangeStats changeStats = new CommitChangeStats(true);

        if (!UtilManager.isGitProject(projectDir)) {
            return changeStats;
        }

        return getCommitsForRange("thisWeek", projectDir, email);
    }

    public static CommitChangeStats getCommitsForRange(String rangeType, String projectDir, String email) {
        if (!UtilManager.isGitProject(projectDir)) {
            return new CommitChangeStats(true);
        }
        UtilManager.TimesData timesData = UtilManager.getTimesData();
        long startOfRange = 0L;
        if (rangeType == "today") {
            startOfRange = timesData.local_start_day;
        } else if (rangeType == "yesterday") {
            startOfRange = timesData.local_start_yesterday;
        } else if (rangeType == "thisWeek") {
            startOfRange = timesData.local_start_of_week;
        }

        String authorArg = "";
        if (email == null || email.equals("")) {
            ResourceInfo resourceInfo = getResourceInfo(projectDir);
            if (resourceInfo != null && resourceInfo.getEmail() != null && !resourceInfo.getEmail().isEmpty()) {
                authorArg = "--author=" + resourceInfo.getEmail();
            }
        } else {
            authorArg = "--author=" + email;
        }

        // set the until to now
        String untilArg = "--until=" + timesData.now;

        String[] cmdList = {"git", "log", "--stat", "--pretty=COMMIT:%H,%ct,%cI,%s", "--since=" + startOfRange, untilArg, authorArg};

        return getChangeStats(cmdList, projectDir, true);
    }

    public static String getRepoUrlLink(String projectDir) {
        if (!UtilManager.isGitProject(projectDir)) {
            return "";
        }
        String[] cmdList = { "git", "config", "--get", "remote.origin.url" };

        // should only be a result of 1
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        String url = resultList != null && resultList.size() > 0 ? resultList.get(0) : null;
        if (url != null && !url.equals("") && url.indexOf(".git") != -1) {
            url = url.substring(0, url.lastIndexOf(".git"));
        }
        return url;
    }

    public static CommitInfo getLastCommitInfo(String projectDir, String email) {
        if (!UtilManager.isGitProject(projectDir)) {
            return null;
        }
        if (email == null) {
            ResourceInfo resourceInfo = getResourceInfo(projectDir);
            email = resourceInfo != null ? resourceInfo.getEmail() : null;
        }
        CommitInfo commitInfo = new CommitInfo();

        String authorArg = (email != null) ? "--author=" + email : "";

        String[] cmdList = { "git", "log", "--pretty=%H,%s", authorArg, "--max-count=1" };

        // should only be a result of 1
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        if (resultList != null && resultList.size() > 0) {
            String[] parts = resultList.get(0).split(",");
            if (parts != null && parts.length == 2) {
                commitInfo.setCommitId(parts[0]);
                commitInfo.setComment(parts[1]);
                commitInfo.setEmail(email);
            }
        }

        return commitInfo;
    }

    public static List<DiffNumStats> getLocalChanges(String projectDir) {
        return getChangesForCommit(projectDir, null);
    }

    public static List<DiffNumStats> getChangesForCommit(String projectDir, String commit) {
        if (!UtilManager.isGitProject(projectDir)) {
            return null;
        }

        List<DiffNumStats> diffNumStats = new ArrayList<>();
        String[] cmdList = (StringUtils.isNotBlank(commit))
                ? new String[]{ "git", "diff", "--numstat", commit + "~", commit }
                : new String[] { "git", "diff", "--numstat" };
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        if (resultList.size() > 0) {
            diffNumStats = accumulateNumStatChanges(resultList);
        }
        return diffNumStats;
    }

    /*
      //Insert  Delete    Filename
        10      0       src/api/billing_client.js
        5       2       src/api/projects_client.js
        -       -       binary_file.bin
      */
    private static List<DiffNumStats>  accumulateNumStatChanges(List<String> resultList) {
        List<DiffNumStats> diffNumStats = new ArrayList<>();
        for (String result : resultList) {
            try {
                String[] parts = result.split("\t");
                DiffNumStats stats = new DiffNumStats();
                stats.insertions = Integer.parseInt(parts[0]);
                stats.deletions = Integer.parseInt(parts[1]);
                // add backslash to match other filenames in tracking
                stats.file_name = File.separator + parts[2];
                diffNumStats.add(stats);
            } catch (Exception e) {
                //
            }
        }
        return diffNumStats;
    }

    public static Long getAuthoredUnixTimestamp(String projectDir, String commit) {
        if (!UtilManager.isGitProject(projectDir)) {
            return null;
        }

        String[] cmdList = { "git", "show", commit, "--pretty=format:%at", "-s" };
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        if (resultList.size() > 0) {
            try {
                return Long.valueOf(resultList.get(0));
            } catch (Exception e) {
            }
        }
        return null;
    }

    public static String getDefaultBranchFromRemoteBranch(String projectDir, String remoteBranch) {
        if (!UtilManager.isGitProject(projectDir)) {
            return null;
        }

        String defaultBranchFromRemoteBranch = "";
        String remoteName = null;

        String[] cmdList = { "git", "remote" };
        List<String> remotes = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        if (remotes.size() == 0) {
            return null;
        }
        for (String branch : remotes) {
            if (remoteBranch.indexOf(branch) != -1) {
                remoteBranch = branch;
                break;
            }
        }

        List<String> resultList = new ArrayList<>();
        if (StringUtils.isNotBlank(remoteBranch)) {
            // Check if the remote has a HEAD symbolic-ref defined
            cmdList = new String[] { "git", "symbolic-ref", "refs/remotes/" + remoteBranch + "/HEAD" };
            resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
            if (resultList.size() > 0) {
                // Make sure it's not a broken HEAD ref
                String headBranchName = resultList.get(0);
                cmdList = new String[] { "git", "show-ref", "--verify", "'" + headBranchName + "'" };
                resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
                if (resultList.size() > 0) {
                    defaultBranchFromRemoteBranch = headBranchName;
                }
            }

            if (StringUtils.isBlank(defaultBranchFromRemoteBranch)) {
                String assumedDefaultBranch = guessDefaultBranchForRemote(projectDir, remoteName);
                if (StringUtils.isNotBlank(assumedDefaultBranch)) {
                    defaultBranchFromRemoteBranch = assumedDefaultBranch;
                }
            }
        }

        if (StringUtils.isBlank(defaultBranchFromRemoteBranch)) {
            // Check if any HEAD branch is defined on any remote
            cmdList = new String[] { "git", "branch", "-r", "-l", "*/HEAD" };
            resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
            if (resultList.size() > 0) {
                String[] remoteBranches = resultList.get(0).split(" ");
                defaultBranchFromRemoteBranch = remoteBranches[remoteBranches.length - 1];
            }
        }

        if (StringUtils.isBlank(defaultBranchFromRemoteBranch)) {
            for (String remote : remotes) {
                String assumedRemoteDefaultBranch = guessDefaultBranchForRemote(projectDir, remote);
                if (StringUtils.isNotBlank(assumedRemoteDefaultBranch)) {
                    defaultBranchFromRemoteBranch = assumedRemoteDefaultBranch;
                }
            }
        }

        return defaultBranchFromRemoteBranch;
    }

    public static String guessDefaultBranchForRemote(String projectDir, String remoteName) {
        if (!UtilManager.isGitProject(projectDir)) {
            return null;
        }

        String[] cmdList = { "git", "branch", "-r", "-l", "'" + remoteName + "/*'" };
        // Get list of branches for the remote
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        String assumedDefault = null;
        if (resultList.size() > 0) {
            for (String val : resultList) {
                val = val.trim();
                if (val.equals("main") || val.equals("master")) {
                    assumedDefault = val;
                    break;
                }
            }
        }

        return assumedDefault;
    }

    public static String getLatestCommitForBranch(String projectDir, String branch) {
        if (!UtilManager.isGitProject(projectDir)) {
            return null;
        }

        String[] cmdList = { "git", "rev-parse", branch };
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        return (resultList.size() > 0) ? resultList.get(0) : "";
    }

    public static boolean commitAlreadyOnRemote(String projectDir, String commit) {
        if (!UtilManager.isGitProject(projectDir)) {
            return true;
        }
        String[] cmdList = { "git", "branch", "-r", "--contains", commit };
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);

        // If results returned, then that means the commit exists on
        // at least 1 remote branch, so return true.
        return resultList.size() > 0;
    }

    public static boolean isMergeCommit(String projectDir, String commit) {
        if (!UtilManager.isGitProject(projectDir)) {
            return false;
        }
        String[] cmdList = { "git", "rev-list", "--parents", "-n", "1", commit };
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        if (resultList.size() > 0) {
            String[] parents = resultList.get(0).split(" ");
            return parents != null && parents.length > 2;
        }
        return false;
    }

    public static List<AuthorCommit> getCommitsForAuthors(String projectDir, String branch, String startRef, List<String> authors) {
        List<AuthorCommit> authorCommits = new ArrayList<>();
        if (!UtilManager.isGitProject(projectDir)) {
            return authorCommits;
        }

        String range = StringUtils.isNotBlank(startRef) ? startRef + "..HEAD" : "HEAD --since=\"2 weeks ago\"";

        List<String> authorCmds = new ArrayList<>();
        for (String author : authors) {
            authorCmds.add("--author=\"" + author + "\"");
        }
        List<String> cmdList = new ArrayList<>();
        cmdList.add("git");
        cmdList.add("log");
        cmdList.add(branch);
        cmdList.add(range);
        cmdList.add("--no-merges");
        cmdList.add("--pretty=format:\"%at =.= %H\"");
        cmdList.addAll(authorCmds);

        String[] cmdArr = cmdList.stream().toArray(String[]::new);

        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdArr, projectDir);
        if (resultList.size() > 0) {
            for (String result : resultList) {
                AuthorCommit authorCommit = new AuthorCommit();
                String[] parts = result.split(" =.= ");
                if (parts.length == 2) {
                    try {
                        authorCommit.authoredTimestamp = Long.parseLong(parts[0]);
                    } catch (Exception e) {

                    }
                    authorCommit.commit = parts[1];
                    authorCommits.add(authorCommit);
                }
            }
        }

        return authorCommits;
    }

    // Returns an array of authors including names and emails from the git config
    public static List<String> getGitConfigAuthors(String projectDir) {
        List<String> authors = new ArrayList<>();
        if (!UtilManager.isGitProject(projectDir)) {
            return authors;
        }

        String[] cmdList = { "git", "config", "user.email" };
        List<String> resultList = UtilManager.getResultsForCommandArgs(cmdList, projectDir);
        if (resultList.size() > 0) {
            for (String result : resultList) {
                String email = result.trim();
                if (StringUtils.isNotBlank(email) && !authors.contains(email)) {
                    authors.add(email);
                }
            }
        }

        return authors;
    }
}
