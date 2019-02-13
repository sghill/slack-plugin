package jenkins.plugins.slack;

import hudson.model.Result;
import jenkins.plugins.slack.testsupport.SlackNotifierBuilder;
import jenkins.plugins.slack.testsupport.TestBuildBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActiveNotifier_StartBuild_PreviousBuild_WithoutCustomMessage_Test {
    private SlackNotifier preferences = SlackNotifierBuilder.builder()
            .doesNotIncludeCustomMessage()
            .build();
    private TestBuildBuilder builder = TestBuildBuilder.builder()
            .withAtLeastOneCompletedBuild() // everything in this file has a previous completed build
            .withoutResult(); // build will not have a result on start

    @Test
    public void shouldCreateNotificationWithoutScmTriggerCause_Success() {
        // given
        Build build = builder
                .withoutScmTriggerCauseAction()
                .withPreviousNonAbortedResult(Result.SUCCESS)
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
    public void shouldCreateNotificationWithoutScmTriggerCause_Failure() {
        // given
        Build build = builder
                .withoutScmTriggerCauseAction()
                .withPreviousNonAbortedResult(Result.FAILURE)
                .withProjectDisplayName("something")
                .withDisplayName("else")
                .withCauseShortDescription("this one thing")
                .withUrl("http://localhost/some/build")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("something - else this one thing (<http://localhost/some/build|Open>)", "danger");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithoutScmTriggerCause_Unstable() {
        // given
        Build build = builder
                .withoutScmTriggerCauseAction()
                .withPreviousNonAbortedResult(Result.UNSTABLE)
                .withProjectDisplayName("something")
                .withDisplayName("else")
                .withCauseShortDescription("this one thing")
                .withUrl("http://localhost/some/build")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("something - else this one thing (<http://localhost/some/build|Open>)", "warning");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_withoutChangeSetComputed_Success() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withoutChangeSetComputed()
                .withPreviousNonAbortedResult(Result.SUCCESS)
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
    public void shouldCreateNotificationWithScmTriggerCause_withoutChangeSetComputed_Failure() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withoutChangeSetComputed()
                .withPreviousNonAbortedResult(Result.FAILURE)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 3")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/1")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("Project - Build 3 Unknown after Not Started Yet (<http://localhost/some/build/1|Open>)", "danger");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_withoutChangeSetComputed_Unstable() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withoutChangeSetComputed()
                .withPreviousNonAbortedResult(Result.UNSTABLE)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 3")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/1")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("Project - Build 3 Unknown after Not Started Yet (<http://localhost/some/build/1|Open>)", "warning");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_withChangeSetEntries_Success() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withoutChangeSetEntries()
                .withPreviousNonAbortedResult(Result.SUCCESS)
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
    public void shouldCreateNotificationWithScmTriggerCause_withChangeSetEntries_Failure() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withoutChangeSetEntries()
                .withPreviousNonAbortedResult(Result.FAILURE)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 4")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/4")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("Project - Build 4 Unknown after Not Started Yet (<http://localhost/some/build/4|Open>)", "danger");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_withChangeSetEntries_Unstable() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withoutChangeSetEntries()
                .withPreviousNonAbortedResult(Result.UNSTABLE)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 4")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/4")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("Project - Build 4 Unknown after Not Started Yet (<http://localhost/some/build/4|Open>)", "warning");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_changeSetEntries_Success() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withChangeSetEntries()
                .withPreviousNonAbortedResult(Result.SUCCESS)
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

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_changeSetEntries_Failure() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withChangeSetEntries()
                .withPreviousNonAbortedResult(Result.FAILURE)
                .withAuthors("Andrea T. Developer", "Old MacDonald")
                .withTotalAffectedFilesInChangeSet(15)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 4")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/4")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("Project - Build 4 Started by changes from Andrea T. Developer, Old MacDonald (15 file(s) changed) (<http://localhost/some/build/4|Open>)", "danger");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateNotificationWithScmTriggerCause_changeSetEntries_Unstable() {
        // given
        Build build = builder
                .withScmTriggerCauseAction()
                .withChangeSetComputed()
                .withChangeSetEntries()
                .withPreviousNonAbortedResult(Result.UNSTABLE)
                .withAuthors("Andrea T. Developer", "Old MacDonald")
                .withTotalAffectedFilesInChangeSet(15)
                .withProjectDisplayName("Project")
                .withDisplayName("Build 4")
                .withCauseShortDescription("scm changes")
                .withUrl("http://localhost/some/build/4")
                .withHumanDuration("Not Started Yet")
                .build();
        ActiveNotifier notifier = new ActiveNotifier(preferences);
        Notification expected = new Notification("Project - Build 4 Started by changes from Andrea T. Developer, Old MacDonald (15 file(s) changed) (<http://localhost/some/build/4|Open>)", "warning");

        // when
        Notification actual = notifier.startBuild(build);

        // then
        assertEquals(expected, actual);
    }

}
