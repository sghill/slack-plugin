package jenkins.plugins.slack;

import com.google.common.collect.Sets;
import hudson.model.Result;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ActiveNotifier_StartBuild_WithoutCustomMessage_Test {
    private SlackNotifier preferences = SlackNotifierBuilder.builder()
            .doesNotIncludeCustomMessage()
            .build();
    private TestBuildBuilder builder = TestBuildBuilder.builder()
            .withoutResult(); // build will not have a result on start

    @Test
    public void shouldCreateNotificationWithoutScmTriggerCause() {
        // given
        Build build = builder
                .withoutScmTriggerCauseAction()
                .withProjectDisplayName("something")
                .withDisplayName("else")
                .withCauseShortDescription("this one thing")
                .withUrl("http://localhost/some/build")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = Notification.good("something - else this one thing (<http://localhost/some/build|Open>)");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_withoutChangeSetComputed() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withoutChangeSetComputed()
                .withProjectDisplayName("Project")
                .withDisplayName("Build 3")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/1")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = Notification.good("Project - Build 3 Unknown after Not Started Yet (<http://localhost/some/build/1|Open>)");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_withChangeSetEntries() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withoutChangeSetEntries()
                .withProjectDisplayName("Project")
                .withDisplayName("Build 4")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/4")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = Notification.good("Project - Build 4 Unknown after Not Started Yet (<http://localhost/some/build/4|Open>)");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_changeSetEntries() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withChangeSetEntries()
                .withAuthors("Andrea T. Developer", "Old MacDonald")
                .withTotalAffectedFilesInChangeSet(15)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 4")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/4")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = Notification.good("Project - Build 4 Started by changes from Andrea T. Developer, Old MacDonald (15 file(s) changed) (<http://localhost/some/build/4|Open>)");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    public static class SlackNotifierBuilder {
        private boolean started;
        private boolean aborted;
        private boolean failure;
        private boolean notBuilt;
        private boolean succeeded;
        private boolean unstable;
        private boolean regression;
        private boolean backToNormal;
        private boolean repeatedFailure;
        private boolean includeTestSummary;
        private boolean includeFailedTests;
        private boolean includeCustomMessage;
        private String customMessage;
        private String customMessageOnSuccess;
        private String customMessageOnAborted;
        private String customMessageOnNotBuilt;
        private String customMessageOnUnstable;
        private String customMessageOnFailure;

        private SlackNotifierBuilder() {
        }

        public static SlackNotifierBuilder builder() {
            return new SlackNotifierBuilder();
        }

        public SlackNotifierBuilder includeCustomMessage() {
            this.includeCustomMessage = true;
            return this;
        }

        public SlackNotifierBuilder doesNotIncludeCustomMessage() {
            this.includeCustomMessage = false;
            return this;
        }

        public SlackNotifierBuilder customMessage(String message) {
            this.customMessage = message;
            return this;
        }

        public SlackNotifier build() {
            return new SlackNotifier(
                    null,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    started,
                    aborted,
                    failure,
                    notBuilt,
                    succeeded,
                    unstable,
                    regression,
                    backToNormal,
                    repeatedFailure,
                    includeTestSummary,
                    includeFailedTests,
                    null,
                    includeCustomMessage,
                    customMessage,
                    customMessageOnSuccess,
                    customMessageOnAborted,
                    customMessageOnNotBuilt,
                    customMessageOnUnstable,
                    customMessageOnFailure
            );
        }
    }

    public static class TestBuildBuilder {
        private String projectDisplayName;
        private String displayName;
        private String url;
        private String humanDuration;
        private long endTimeInMillis;
        private boolean hasTestResults;
        private TestResults testResults;
        private boolean scmTriggerCauseAction;
        private String causeShortDescription;
        private hudson.model.Result result;
        private boolean changeSetComputed;
        private boolean changeSetEntries;
        private Set<String> authors = new HashSet<>();
        private long affectedFiles;

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

        public TestBuildBuilder result(hudson.model.Result result) {
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

        Build build() {
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
                public boolean projectHasAtLeastOneCompletedBuild() {
                    return false;
                }

                @Override
                public boolean projectHasOnlyOneCompletedBuild() {
                    return false;
                }

                @Override
                public boolean projectHasAtLeastOneNonAbortedBuild() {
                    return false;
                }

                @Override
                public Result result() {
                    return result;
                }

                @Override
                public Result previousNonAbortedResult() {
                    return null;
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
    }
}
