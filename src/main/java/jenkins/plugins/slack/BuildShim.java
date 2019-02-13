package jenkins.plugins.slack;

import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.tasks.test.TestResult;
import hudson.triggers.SCMTrigger;
import hudson.util.LogTaskListener;
import jenkins.model.Jenkins;
import org.jenkinsci.plugins.displayurlapi.DisplayURLProvider;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.logging.Level.INFO;

public class BuildShim implements Build {
    private static final Logger logger = Logger.getLogger(BuildShim.class.getName());
    private final AbstractBuild<?, ?> build;

    public static BuildShim create(AbstractBuild<?, ?> build) {
        return new BuildShim(build);
    }

    private BuildShim(AbstractBuild<?, ?> build) {
        this.build = build;
    }

    @Override
    public String projectDisplayName() {
        return build.getProject().getFullDisplayName();
    }

    @Override
    public String displayName() {
        return build.getDisplayName();
    }

    @Override
    public String url() {
        return DisplayURLProvider.get().getRunURL(build);
    }

    @Override
    public String humanDuration() {
        return build.getDurationString();
    }

    @Override
    public long endTimeInMillis() {
        return build.getStartTimeInMillis() + build.getDuration();
    }

    @Override
    public boolean hasTestResults() {
        return getTestResults() != null;
    }

    @Override
    public TestResults testResults() {
        AbstractTestResultAction<?> results = getTestResults();
        return new TestResults(
                results.getTotalCount(),
                results.getFailCount(),
                results.getSkipCount(),
                results.getFailedTests().stream()
                        .map(r -> (TestResult) r)
                        .map(TestResults.Result::valueOf)
                        .collect(Collectors.toList()));
    }

    @Override
    public boolean hasResult() {
        return build.getResult() != null;
    }

    @Override
    public boolean hasAtLeastOnePreviousNonAbortedAndCompletedBuild() {
        AbstractBuild<?, ?> lastBuild = build.getProject().getLastBuild();
        Run lastNonAbortedBuild = lastBuild.getPreviousBuild();
        while (lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
            lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
        }
        return lastNonAbortedBuild != null;
    }

    @Override
    public Result result() {
        return build.getResult();
    }

    /**
     * If the last build was aborted, go back to find the last non-aborted build.
     * This is so that aborted builds do not affect build transitions.
     * I.e. if build 1 was failure, build 2 was aborted and build 3 was a success the transition
     * should be failure -> success (and therefore back to normal) not aborted -> success.
     *
     * If all previous builds have been aborted, then use
     * SUCCESS as a default status so an aborted message is sent
     */
    @Override
    public Result previousNonAbortedResult() {
        AbstractBuild<?, ?> lastBuild = build.getProject().getLastBuild();
        Run lastNonAbortedBuild = lastBuild.getPreviousBuild();
        while (lastNonAbortedBuild != null && lastNonAbortedBuild.getResult() == Result.ABORTED) {
            lastNonAbortedBuild = lastNonAbortedBuild.getPreviousBuild();
        }
        return lastNonAbortedBuild == null ? Result.SUCCESS : lastNonAbortedBuild.getResult();
    }

    @Override
    public String expandTokensFor(String message) {
        try {
            return TokenMacro.expandAll(build, new LogTaskListener(logger, INFO), message, false, null);
        } catch (MacroEvaluationException | IOException | InterruptedException e) {
            logger.severe(() -> "Failed to expand tokens in custom message for " + buildkey() + ".\n" +
                    "\t custom message: " + message + "\n" +
                    "\t exception: " + e);
            return "";
        }
    }

    @Override
    public boolean hasPreviousSuccess() {
        return build.getPreviousSuccessfulBuild() != null;
    }

    @Override
    public boolean hasCompletedBuildSincePreviousSuccess() {
        return build.getPreviousSuccessfulBuild().getNextBuild() != null;
    }

    @Override
    public long endTimeOfInitialFailureInMillis() {
        // TODO: what if this was aborted?
        AbstractBuild<?, ?> firstFailure = build.getPreviousSuccessfulBuild().getNextBuild();
        return firstFailure.getStartTimeInMillis() + firstFailure.getDuration();
    }

    @Override
    public boolean hasFailedSincePreviousSuccess() {
        return build.getPreviousSuccessfulBuild().getNextBuild() != null;
    }

    @Override
    public boolean hasNonScmTriggerCauseAction() {
        CauseAction causeAction = build.getAction(CauseAction.class);
        return causeAction != null && causeAction.findCause(SCMTrigger.SCMTriggerCause.class) == null;
    }

    @Override
    public String causeShortDescription() {
        return build.getAction(CauseAction.class).getCauses().get(0).getShortDescription();
    }

    @Override
    public boolean doesNotHaveChangeSetComputed() {
        return !build.hasChangeSetComputed();
    }

    @Override
    public Set<String> changeSetAuthors() {
        return changeSetEntryStream()
                .map(ChangeLogSet.Entry::getAuthor)
                .map(User::getDisplayName)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean doesNotHaveChangeSetEntries() {
        Object[] items = build.getChangeSet().getItems();
        return items == null || items.length == 0;
    }

    @Override
    public List<Commit> changeSetEntries() {
        return changeSetEntryStream()
                .map(e -> new Commit(e.getAuthor().getDisplayName(), e.getMsg()))
                .collect(Collectors.toList());
    }

    @Override
    public long totalAffectedFilesInChangeSet() {
        return changeSetEntryStream()
                .mapToLong(e -> e.getAffectedFiles().size())
                .sum();
    }

    @Override
    public Build previous() {
        return BuildShim.create(build.getPreviousBuild());
    }

    @Override
    public boolean doesNotHaveUpstreamCause() {
        return build.getCause(Cause.UpstreamCause.class) == null;
    }

    @Override
    public Build upstream() {
        Cause.UpstreamCause c = build.getCause(Cause.UpstreamCause.class);
        if (c == null) {
            return null;
        }
        String upProjectName = c.getUpstreamProject();
        int buildNumber = c.getUpstreamBuild();
        AbstractProject project = Jenkins.get().getItemByFullName(upProjectName, AbstractProject.class);
        if (project == null) {
            return null;
        }
        return BuildShim.create(project.getBuildByNumber(buildNumber));
    }

    @Override
    public boolean upstreamExists() {
        return false;
    }

    private Stream<ChangeLogSet.Entry> changeSetEntryStream() {
        return Arrays.stream(build.getChangeSet().getItems())
                .map(i -> (ChangeLogSet.Entry) i);
    }

    private AbstractTestResultAction<?> getTestResults() {
        return build.getAction(AbstractTestResultAction.class);
    }

    private String buildkey() {
        return build.getProject().getFullDisplayName() + " #" + build.getNumber();
    }
}
