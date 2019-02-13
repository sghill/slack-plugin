package jenkins.plugins.slack.testsupport;

import jenkins.plugins.slack.ActiveNotifier_StartBuild_NoPreviousBuild_WithoutCustomMessage_Test;
import jenkins.plugins.slack.SlackNotifier;

public class SlackNotifierBuilder {
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
