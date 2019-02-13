package jenkins.plugins.slack.testsupport;

import com.google.common.collect.Sets;
import hudson.model.Result;
import jenkins.plugins.slack.Build;
import jenkins.plugins.slack.Commit;
import jenkins.plugins.slack.TestResults;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestBuildBuilder {
    private String projectDisplayName;
    private String displayName;
    private String url;
    private String humanDuration;
    private long endTimeInMillis;
    private boolean hasTestResults;
    private TestResults testResults;
    private boolean scmTriggerCauseAction;
    private String causeShortDescription;
    private Result result;
    private boolean changeSetComputed;
    private boolean changeSetEntries;
    private Set<String> authors = new HashSet<>();
    private long affectedFiles;
    private boolean atLeastOneCompletedBuild;
    private Result previousResult;

    private TestBuildBuilder() {
    }

    public static TestBuildBuilder builder() {
        return new TestBuildBuilder();
    }

    public TestBuildBuilder withScmTriggerCauseAction() {
        this.scmTriggerCauseAction = true;
        return this;
    }

    public TestBuildBuilder withoutScmTriggerCauseAction() {
        this.scmTriggerCauseAction = false;
        return this;
    }

    public TestBuildBuilder withProjectDisplayName(String name) {
        this.projectDisplayName = name;
        return this;
    }

    public TestBuildBuilder withDisplayName(String name) {
        this.displayName = name;
        return this;
    }

    public TestBuildBuilder withCauseShortDescription(String description) {
        this.causeShortDescription = description;
        return this;
    }

    public TestBuildBuilder withUrl(String url) {
        this.url = url;
        return this;
    }

    public TestBuildBuilder result(Result result) {
        this.result = result;
        return this;
    }

    public TestBuildBuilder withChangeSetComputed() {
        this.changeSetComputed = true;
        return this;
    }

    public TestBuildBuilder withoutChangeSetComputed() {
        this.changeSetComputed = false;
        return this;
    }

    public Build build() {
        return new Build() {
            @Override
            public String projectDisplayName() {
                return projectDisplayName;
            }

            @Override
            public String displayName() {
                return displayName;
            }

            @Override
            public String url() {
                return url;
            }

            @Override
            public String humanDuration() {
                return humanDuration;
            }

            @Override
            public long endTimeInMillis() {
                return 0;
            }

            @Override
            public boolean hasTestResults() {
                return false;
            }

            @Override
            public TestResults testResults() {
                return null;
            }

            @Override
            public boolean hasResult() {
                return result() != null;
            }

            @Override
            public boolean hasAtLeastOnePreviousNonAbortedAndCompletedBuild() {
                return atLeastOneCompletedBuild;
            }

            @Override
            public Result result() {
                return result;
            }

            @Override
            public Result previousNonAbortedResult() {
                return previousResult;
            }

            @Override
            public String expandTokensFor(String message) {
                return null;
            }

            @Override
            public boolean hasPreviousSuccess() {
                return false;
            }

            @Override
            public boolean hasCompletedBuildSincePreviousSuccess() {
                return false;
            }

            @Override
            public long endTimeOfInitialFailureInMillis() {
                return 0;
            }

            @Override
            public boolean hasFailedSincePreviousSuccess() {
                return false;
            }

            @Override
            public boolean hasNonScmTriggerCauseAction() {
                return !scmTriggerCauseAction;
            }

            @Override
            public String causeShortDescription() {
                return causeShortDescription;
            }

            @Override
            public boolean doesNotHaveChangeSetComputed() {
                return !changeSetComputed;
            }

            @Override
            public Set<String> changeSetAuthors() {
                return authors;
            }

            @Override
            public boolean doesNotHaveChangeSetEntries() {
                return !changeSetEntries;
            }

            @Override
            public List<Commit> changeSetEntries() {
                return null;
            }

            @Override
            public long totalAffectedFilesInChangeSet() {
                return affectedFiles;
            }

            @Override
            public Build previous() {
                return null;
            }

            @Override
            public boolean doesNotHaveUpstreamCause() {
                return false;
            }

            @Override
            public Build upstream() {
                return null;
            }

            @Override
            public boolean upstreamExists() {
                return false;
            }
        };
    }

    public TestBuildBuilder withoutResult() {
        this.result = null;
        return this;
    }

    public TestBuildBuilder withHumanDuration(String duration) {
        this.humanDuration = duration;
        return this;
    }

    public TestBuildBuilder withoutChangeSetEntries() {
        this.changeSetEntries = false;
        return this;
    }

    public TestBuildBuilder withChangeSetEntries() {
        this.changeSetEntries = true;
        return this;
    }

    public TestBuildBuilder withAuthors(String first, String... rest) {
        Set<String> given = Sets.newHashSet(first);
        given.addAll(Arrays.asList(rest));
        this.authors = given;
        return this;
    }

    public TestBuildBuilder withTotalAffectedFilesInChangeSet(long total) {
        this.affectedFiles = total;
        return this;
    }

    public TestBuildBuilder withoutAtLeastOneCompletedBuild() {
        this.atLeastOneCompletedBuild = false;
        return this;
    }

    public TestBuildBuilder withAtLeastOneCompletedBuild() {
        this.atLeastOneCompletedBuild = true;
        return this;
    }

    public TestBuildBuilder withPreviousNonAbortedResult(Result result) {
        this.previousResult = result;
        return this;
    }
}
