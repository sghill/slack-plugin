package jenkins.plugins.slack;

import hudson.model.Result;

import java.util.List;
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
    boolean hasAtLeastOnePreviousNonAbortedAndCompletedBuild();
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
    List<Commit> changeSetEntries();
    long totalAffectedFilesInChangeSet();
    Build previous();
    boolean doesNotHaveUpstreamCause();
    Build upstream();
    boolean upstreamExists();
}
