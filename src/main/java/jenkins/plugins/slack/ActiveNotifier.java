package jenkins.plugins.slack;

import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Result;
import hudson.scm.ChangeLogSet;
import hudson.scm.ChangeLogSet.AffectedFile;
import hudson.scm.ChangeLogSet.Entry;
import hudson.tasks.junit.TestResultAction;
import hudson.tasks.test.TestResult;
import jenkins.model.Jenkins;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements NotificationProducer<AbstractBuild<?, ?>, Notification> {

    private static final Logger logger = Logger.getLogger(SlackNotifier.class.getName());

    private final SlackNotifier notifier;

    public ActiveNotifier(SlackNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public Notification startBuild(AbstractBuild<?, ?> build) {
        Build shim = BuildShim.create(build);

        if (shim.hasNonScmTriggerCauseAction()) {
            MessageBuilder message = new MessageBuilder(notifier, shim);
            message.append(shim.causeShortDescription());
            message.appendOpenLink();
            if (notifier.getIncludeCustomMessage()) {
                message.appendCustomMessage(shim.result());
            }
            return notifyStart(shim, message.toString());
            // Cause was found, exit early to prevent double-message
        }

        String changes = getChanges(shim, notifier.getIncludeCustomMessage());
        Notification notification;
        if (changes != null) {
            notification = notifyStart(shim, changes);
        } else {
            notification = notifyStart(shim, getBuildStatusMessage(shim, false, false, notifier.getIncludeCustomMessage()));
        }
        return notification;
    }

    private Notification notifyStart(Build build, String message) {
        if (build.projectHasAtLeastOneCompletedBuild()) {
            if (build.projectHasOnlyOneCompletedBuild()) {
                return Notification.good(message);
            }
            return new Notification(message, getBuildColor(build));
        }
        return Notification.good(message);
    }

    @Override
    public Notification finalizeBuild(AbstractBuild<?, ?> r) {
        Build shim = BuildShim.create(r);
        AbstractProject<?, ?> project = r.getProject();
        Result result = r.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild();
        Notification notification = null;
        if (null != previousBuild) {
            do {
                previousBuild = previousBuild.getPreviousCompletedBuild();
            } while (previousBuild != null && previousBuild.getResult() == Result.ABORTED);
            Result previousResult = (previousBuild != null) ? previousBuild.getResult() : Result.SUCCESS;
            if(null != previousResult && (result != null && result.isWorseThan(previousResult) || moreTestFailuresThanPreviousBuild(r, previousBuild)) && notifier.getNotifyRegression()) {
                String message = getBuildStatusMessage(shim, notifier.getIncludeTestSummary(),
                        notifier.getIncludeFailedTests(), notifier.getIncludeCustomMessage());
                if (notifier.getCommitInfoChoice().showAnything()) {
                    message = message + "\n" + getCommitList(r);
                }
                notification = new Notification(message, getBuildColor(shim));
            }
        }
        return notification;
    }

    @Override
    public Notification completedBuild(AbstractBuild<?, ?> r) {
        Build build = BuildShim.create(r);
        AbstractProject<?, ?> project = r.getProject();
        Result result = r.getResult();
        AbstractBuild<?, ?> previousBuild = project.getLastBuild();
        Notification notification = null;
        if (null != previousBuild) {
            do {
                previousBuild = previousBuild.getPreviousCompletedBuild();
            } while (null != previousBuild && previousBuild.getResult() == Result.ABORTED);
            Result previousResult = (null != previousBuild) ? previousBuild.getResult() : Result.SUCCESS;
            if ((result == Result.ABORTED && notifier.getNotifyAborted())
                    || (result == Result.FAILURE //notify only on single failed build
                    && previousResult != Result.FAILURE
                    && notifier.getNotifyFailure())
                    || (result == Result.FAILURE //notify only on repeated failures
                    && previousResult == Result.FAILURE
                    && notifier.getNotifyRepeatedFailure())
                    || (result == Result.NOT_BUILT && notifier.getNotifyNotBuilt())
                    || (result == Result.SUCCESS
                    && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                    && notifier.getNotifyBackToNormal())
                    || (result == Result.SUCCESS && notifier.getNotifySuccess())
                    || (result == Result.UNSTABLE && notifier.getNotifyUnstable())) {
                String message = getBuildStatusMessage(build, notifier.getIncludeTestSummary(),
                        notifier.getIncludeFailedTests(), notifier.getIncludeCustomMessage());
                if (notifier.getCommitInfoChoice().showAnything()) {
                    message = message + "\n" + getCommitList(r);
                }
                notification = new Notification(message, getBuildColor(build));
            }
        }
        return notification;
    }

    private boolean moreTestFailuresThanPreviousBuild(AbstractBuild currentBuild, AbstractBuild<?, ?> previousBuild) {
        if (getTestResult(currentBuild) != null && getTestResult(previousBuild) != null) {
            if (getTestResult(currentBuild).getFailCount() > getTestResult(previousBuild).getFailCount())
                return true;

            // test if different tests failed.
            return !getFailedTestIds(currentBuild).equals(getFailedTestIds(previousBuild));
        }
        return false;
    }

    private TestResultAction getTestResult(AbstractBuild build) {
        return build.getAction(TestResultAction.class);
    }

    private Set<String> getFailedTestIds(AbstractBuild currentBuild) {
        Set<String> failedTestIds = new HashSet<>();
        List<? extends TestResult> failedTests = getTestResult(currentBuild).getFailedTests();
        for(TestResult result : failedTests) {
            failedTestIds.add(result.getId());
        }

        return failedTestIds;
    }

    String getChanges(Build build, boolean includeCustomMessage) {
        if (build.doesNotHaveChangeSetComputed()) {
            logger.info("No change set computed...");
            return null;
        }
        if (build.doesNotHaveChangeSetEntries()) {
            logger.info("Empty change...");
            return null;
        }
        Set<String> authors = build.changeSetAuthors();
        MessageBuilder message = new MessageBuilder(notifier, build);
        message.append("Started by changes from ");
        message.append(StringUtils.join(authors, ", "));
        message.append(" (");
        message.append(build.totalAffectedFilesInChangeSet());
        message.append(" file(s) changed)");
        message.appendOpenLink();
        if (includeCustomMessage) {
            message.appendCustomMessage(build.result());
        }
        return message.toString();
    }

    String getCommitList(AbstractBuild r) {
        ChangeLogSet changeSet = r.getChangeSet();
        List<Entry> entries = new LinkedList<>();
        for (Object o : changeSet.getItems()) {
            Entry entry = (Entry) o;
            logger.info("Entry " + o);
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            logger.info("Empty change...");
            Cause.UpstreamCause c = (Cause.UpstreamCause)r.getCause(Cause.UpstreamCause.class);
            if (c == null) {
                return "No Changes.";
            }
            String upProjectName = c.getUpstreamProject();
            int buildNumber = c.getUpstreamBuild();
            AbstractProject project = Jenkins.get().getItemByFullName(upProjectName, AbstractProject.class);
            if (project != null) {
                AbstractBuild upBuild = project.getBuildByNumber(buildNumber);
                return getCommitList(upBuild);
            }
        }
        Set<String> commits = new HashSet<>();
        for (Entry entry : entries) {
            StringBuilder commit = new StringBuilder();
            CommitInfoChoice commitInfoChoice = notifier.getCommitInfoChoice();
            if (commitInfoChoice.showTitle()) {
                commit.append(entry.getMsg());
            }
            if (commitInfoChoice.showAuthor()) {
                commit.append(" [").append(entry.getAuthor().getDisplayName()).append("]");
            }
            commits.add(commit.toString());
        }
        MessageBuilder message = new MessageBuilder(notifier, BuildShim.create(r));
        message.append("Changes:\n- ");
        message.append(StringUtils.join(commits, "\n- "));
        return message.toString();
    }

    static String getBuildColor(Build build) {
        if (build.result() == Result.SUCCESS) {
            return "good";
        } else if (build.result() == Result.FAILURE) {
            return "danger";
        } else {
            return "warning";
        }
    }

    String getBuildStatusMessage(Build build, boolean includeTestSummary, boolean includeFailedTests, boolean includeCustomMessage) {
        MessageBuilder message = new MessageBuilder(notifier, build);
        message.appendStatusMessage();
        message.appendDuration();
        message.appendOpenLink();
        if (includeTestSummary) {
            message.appendTestSummary();
        }
        if (includeFailedTests) {
            message.appendFailedTests();
        }
        if (includeCustomMessage) {
            message.appendCustomMessage(build.result());
        }
        return message.toString();
    }

    public static class MessageBuilder {

        private static final Pattern aTag = Pattern.compile("(?i)<a([^>]+)>(.+?)</a>|([{%])");
        private static final Pattern href = Pattern.compile("\\s*(?i)href\\s*=\\s*(\"([^\"]*\")|'[^']*'|([^'\">\\s]+))");
        private static final String BACK_TO_NORMAL_STATUS_MESSAGE = "Back to normal",
                                    STILL_FAILING_STATUS_MESSAGE = "Still Failing",
                                    SUCCESS_STATUS_MESSAGE = "Success",
                                    FAILURE_STATUS_MESSAGE = "Failure",
                                    ABORTED_STATUS_MESSAGE = "Aborted",
                                    NOT_BUILT_STATUS_MESSAGE = "Not built",
                                    UNSTABLE_STATUS_MESSAGE = "Unstable",
                                    REGRESSION_STATUS_MESSAGE = "Regression",
                                    UNKNOWN_STATUS_MESSAGE = "Unknown";

        private StringBuilder message;
        private SlackNotifier notifier;
        private final Build build;

        public MessageBuilder(SlackNotifier notifier, Build build) {
            this.notifier = notifier;
            this.message = new StringBuilder();
            this.build = build;
            startMessage();
        }

        public MessageBuilder appendStatusMessage() {
            message.append(this.escape(getStatusMessage()));
            return this;
        }

        private String getStatusMessage() {
            Result previousResult;
            if(build.hasResult()) {
                if (build.projectHasAtLeastOneCompletedBuild()) {


                    previousResult = build.previousNonAbortedResult();

                    /* Back to normal should only be shown if the build has actually succeeded at some point.
                     * Also, if a build was previously unstable and has now succeeded the status should be
                     * "Back to normal"
                     */
                    if (build.result() == Result.SUCCESS
                            && (previousResult == Result.FAILURE || previousResult == Result.UNSTABLE)
                            && build.hasPreviousSuccess() && notifier.getNotifyBackToNormal()) {
                        return BACK_TO_NORMAL_STATUS_MESSAGE;
                    }
                    if (build.result() == Result.FAILURE && previousResult == Result.FAILURE) {
                        return STILL_FAILING_STATUS_MESSAGE;
                    }
                    if (build.result() == Result.SUCCESS) {
                        return SUCCESS_STATUS_MESSAGE;
                    }
                    if (build.result() == Result.FAILURE) {
                        return FAILURE_STATUS_MESSAGE;
                    }
                    if (build.result() == Result.ABORTED) {
                        return ABORTED_STATUS_MESSAGE;
                    }
                    if (build.result() == Result.NOT_BUILT) {
                        return NOT_BUILT_STATUS_MESSAGE;
                    }
                    if (build.result() == Result.UNSTABLE) {
                        return UNSTABLE_STATUS_MESSAGE;
                    }
                    if (build.projectHasAtLeastOneNonAbortedBuild() && previousResult != null && build.result().isWorseThan(previousResult)) {
                        return REGRESSION_STATUS_MESSAGE;
                    }
                }
            }
            return UNKNOWN_STATUS_MESSAGE;
        }

        public MessageBuilder append(String string) {
            message.append(this.escape(string));
            return this;
        }

        public MessageBuilder append(Object string) {
            message.append(this.escape(string.toString()));
            return this;
        }

        private MessageBuilder startMessage() {
            message.append(this.escape(build.projectDisplayName()));
            message.append(" - ");
            message.append(this.escape(build.displayName()));
            message.append(" ");
            return this;
        }

        public MessageBuilder appendOpenLink() {
            String url = build.url();
            message.append(" (<").append(url).append("|Open>)");
            return this;
        }

        public MessageBuilder appendDuration() {
            message.append(" after ");
            String durationString;
            if(message.toString().contains(BACK_TO_NORMAL_STATUS_MESSAGE)){
                durationString = createBackToNormalDurationString();
            } else {
                durationString = build.humanDuration();
            }
            message.append(durationString);
            return this;
        }

        public MessageBuilder appendTestSummary() {
            if (build.hasTestResults()) {
                TestResults testResults = build.testResults();
                message.append("\nTest Status:\n");
                message.append("\tPassed: ").append(testResults.getPassed());
                message.append(", Failed: ").append(testResults.getFailed());
                message.append(", Skipped: ").append(testResults.getSkipped());
            } else {
                message.append("\nNo Tests found.");
            }
            return this;
        }

        public MessageBuilder appendFailedTests() {
            if (build.hasTestResults()) {
                TestResults results = build.testResults();
                int failed = results.getFailed();
                message.append("\n").append(failed).append(" Failed Tests:\n");
                for(TestResults.Result result : results.getFailedTests()) {
                    message.append("\t").append(getTestClassAndMethod(result)).append(" after ")
                            .append(result.getHumanDuration()).append("\n");
                }
            }
            return this;
        }

        public MessageBuilder appendCustomMessage(Result buildResult) {
            String customMessage = "";
            if (buildResult != null) {
                if (buildResult == Result.SUCCESS) {
                    customMessage = notifier.getCustomMessageSuccess();
                } else if (buildResult == Result.ABORTED) {
                    customMessage = notifier.getCustomMessageAborted();
                } else if (buildResult == Result.NOT_BUILT) {
                    customMessage = notifier.getCustomMessageNotBuilt();
                } else if (buildResult == Result.UNSTABLE) {
                    customMessage = notifier.getCustomMessageUnstable();
                } else if (buildResult == Result.FAILURE) {
                    customMessage = notifier.getCustomMessageFailure();
                }
            }
            if (customMessage == null || customMessage.isEmpty()) {
                customMessage = notifier.getCustomMessage();
            }
            message.append("\n");
            message.append(build.expandTokensFor(customMessage));
            return this;
        }

        private String getTestClassAndMethod(TestResults.Result result) {
            String fullDisplayName = result.getDisplayName();

            if (StringUtils.countMatches(fullDisplayName, ".") > 1) {
                int methodDotIndex = fullDisplayName.lastIndexOf('.');
                int testClassDotIndex = fullDisplayName.substring(0, methodDotIndex).lastIndexOf('.');

                return fullDisplayName.substring(testClassDotIndex + 1);

            } else {
                return fullDisplayName;
            }
        }

        private String createBackToNormalDurationString(){
            // This status code guarantees that the previous build fails and has been successful before
            // The back to normal time is the time since the build first broke
            boolean hasPreviousSuccessfulBuild = build.hasPreviousSuccess();
            boolean mostRecentSuccessfulBuildHadNextBuild = hasPreviousSuccessfulBuild && build.hasCompletedBuildSincePreviousSuccess();
            if (mostRecentSuccessfulBuildHadNextBuild) {
                if (build.hasFailedSincePreviousSuccess()) {
                    long backToNormalDuration = build.endTimeInMillis() - build.endTimeOfInitialFailureInMillis();
                    return Util.getTimeSpanString(backToNormalDuration);
                }
            }
            return null;
        }

        private String escapeCharacters(String string) {
            string = string.replace("&", "&amp;");
            string = string.replace("<", "&lt;");
            string = string.replace(">", "&gt;");

            return string;
        }

        private String[] extractReplaceLinks(Matcher aTag, StringBuffer sb) {
            int size = 0;
            List<String> links = new ArrayList<>();
            while (aTag.find()) {
                String firstGroup = aTag.group(1);
                if (firstGroup != null) {
                    Matcher url = href.matcher(firstGroup);
                    if (url.find()) {
                        String escapeThis = aTag.group(3);
                        if (escapeThis != null) {
                            aTag.appendReplacement(sb, String.format("{%s}", size++));
                            links.add(escapeThis);
                        } else {
                            aTag.appendReplacement(sb, String.format("{%s}", size++));
                            links.add(String.format("<%s|%s>", url.group(1).replaceAll("\"", ""), aTag.group(2)));
                        }
                    }
                } else {
                    String escapeThis = aTag.group(3);
                    aTag.appendReplacement(sb, String.format("{%s}", size++));
                    links.add(escapeThis);
                }
            }
            aTag.appendTail(sb);
            return links.toArray(new String[size]);
        }

        public String escape(String string) {
            StringBuffer pattern = new StringBuffer();
            String[] links = extractReplaceLinks(aTag.matcher(string), pattern);
            return MessageFormat.format(escapeCharacters(pattern.toString()), links);
        }

        public String toString() {
            return message.toString();
        }
    }
}
