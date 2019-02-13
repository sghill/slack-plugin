package jenkins.plugins.slack;

import jenkins.plugins.slack.testsupport.SlackNotifierBuilder;
import jenkins.plugins.slack.testsupport.TestBuildBuilder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ActiveNotifier_StartBuild_NoPreviousBuild_WithoutCustomMessage_Test {
    private SlackNotifier preferences = SlackNotifierBuilder.builder()
            .doesNotIncludeCustomMessage()
            .build();
    private TestBuildBuilder builder = TestBuildBuilder.builder()
            .withoutAtLeastOneCompletedBuild()
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

}
