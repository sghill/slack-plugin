package jenkins.plugins.slack;

import hudson.model.Result;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class ActiveNotifierStartBuildTest {

    @Test
    public void shouldCreateNotificationWithoutScmTriggerCauseOrCustomMessage() {
        // given
        Build build = TestBuildBuilder.builder()
                .withoutScmTriggerCauseAction()
                .projectDisplayName("something")
                .displayName("else")
                .causeShortDescription("this one thing")
                .url("http://localhost/some/build")
                .build();
        SlackNotifier preferences = SlackNotifierBuilder.builder()
                .doesNotIncludeCustomMessage()
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = Notification.good("something - else this one thing (<http://localhost/some/build|Open>)");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    @Ignore // custom messages are going to require us to split out the token processor from the build interface
    public void shouldCreateNotificationWithoutScmTriggerCauseWithCustomMessage() {
        // given
        Build build = TestBuildBuilder.builder()
                .withoutScmTriggerCauseAction()
                .projectDisplayName("something")
                .displayName("else")
                .causeShortDescription("this one thing")
                .url("http://localhost/some/build")
                .result(Result.SUCCESS)
                .build();
        SlackNotifier preferences = SlackNotifierBuilder.builder()
                .includeCustomMessage()
                .customMessage("hello there folks")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = Notification.good("something - else this one thing (<http://localhost/some/build|Open>)");

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

        public TestBuildBuilder projectDisplayName(String name) {
            this.projectDisplayName = name;
            return this;
        }

        public TestBuildBuilder displayName(String name) {
            this.displayName = name;
            return this;
        }

        public TestBuildBuilder causeShortDescription(String description) {
            this.causeShortDescription = description;
            return this;
        }

        public TestBuildBuilder url(String url) {
            this.url = url;
            return this;
        }

        public TestBuildBuilder result(hudson.model.Result result) {
            this.result = result;
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
                    return null;
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
                    return false;
                }

                @Override
                public Set<String> changeSetAuthors() {
                    return null;
                }

                @Override
                public boolean doesNotHaveChangeSetEntries() {
                    return false;
                }

                @Override
                public List<Commit> changeSetEntries() {
                    return null;
                }

                @Override
                public long totalAffectedFilesInChangeSet() {
                    return 0;
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
    }
}
