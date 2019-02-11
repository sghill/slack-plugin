package jenkins.plugins.slack;

import hudson.model.Result;

import java.util.Set;

public interface Build {
    String projectDisplayName();
    String displayName();
    String url();
    String humanDuration();
    long endTimeInMillis();
    boolean hasTestResults();
    TestResults testResults();
    boolean hasResult();
    boolean projectHasAtLeastOneCompletedBuild();
    boolean projectHasOnlyOneCompletedBuild();
    boolean projectHasAtLeastOneNonAbortedBuild();
    Result result();
    Result previousNonAbortedResult();
    String expandTokensFor(String message);
    boolean hasPreviousSuccess();
    boolean hasCompletedBuildSincePreviousSuccess();
    long endTimeOfInitialFailureInMillis();
    boolean hasFailedSincePreviousSuccess();
    boolean hasNonScmTriggerCauseAction();
    String causeShortDescription();
    boolean doesNotHaveChangeSetComputed();
    Set<String> changeSetAuthors();
    boolean doesNotHaveChangeSetEntries();
    long totalAffectedFilesInChangeSet();
}
