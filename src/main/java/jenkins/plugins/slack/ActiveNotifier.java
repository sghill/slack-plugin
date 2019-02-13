package jenkins.plugins.slack;

import com.google.common.collect.ImmutableMap;
import hudson.Util;
import hudson.model.Result;
import org.apache.commons.lang.StringUtils;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SuppressWarnings("rawtypes")
public class ActiveNotifier implements NotificationProducer<Build, Notification> {
    private static final Logger logger = Logger.getLogger(SlackNotifier.class.getName());
    private static final Map<Result, String> RESULT_COLOR = ImmutableMap.of(
            Result.SUCCESS, "good",
            Result.FAILURE, "danger"
    );

    private final SlackNotifier notifier;

    public ActiveNotifier(SlackNotifier notifier) {
        this.notifier = notifier;
    }

    @Override
    public Notification startBuild(Build build) {
        boolean includeCustomMessage = notifier.getIncludeCustomMessage();
        if (build.hasNonScmTriggerCauseAction()) {
            MessageBuilder message = new MessageBuilder(notifier, build);
            message.append(build.causeShortDescription());
            message.appendOpenLink();
            if (includeCustomMessage) {
                message.appendCustomMessage(build.result());
            }
            return notifyStart(build, message.toString());
            // Cause was found, exit early to prevent double-message
        }

        String changes = getChanges(build, includeCustomMessage);

        if (changes != null) {
            return notifyStart(build, changes);
        }
        return notifyStart(build, getBuildStatusMessage(build, false, false, includeCustomMessage));
    }

    private Notification notifyStart(Build build, String message) {
        if (build.hasAtLeastOnePreviousNonAbortedAndCompletedBuild()) {
            return new Notification(message, getResultColor(build.previousNonAbortedResult()));
        }
        return Notification.good(message);
    }

    @Override
    public Notification finalizeBuild(Build build) {
        Result result = build.result();
        Build previous = build.previous();
        Notification notification = null;
        if (build.hasAtLeastOnePreviousNonAbortedAndCompletedBuild()) {
            Result previousResult = build.previousNonAbortedResult();
            boolean alwaysTrue = null != previousResult;
            boolean currentBuildHasResult = build.hasResult();
            if(alwaysTrue && (currentBuildHasResult && result.isWorseThan(previousResult) || moreTestFailuresThanPreviousBuild(build, previous)) && notifier.getNotifyRegression()) {
                String message = getBuildStatusMessage(build, notifier.getIncludeTestSummary(),
                        notifier.getIncludeFailedTests(), notifier.getIncludeCustomMessage());
                if (notifier.getCommitInfoChoice().showAnything()) {
                    message = message + "\n" + getCommitList(build);
                }
                notification = new Notification(message, getResultColor(build.result()));
            }
        }
        return notification;
    }

    @Override
    public Notification completedBuild(Build build) {
        Result result = build.result();
        Notification notification = null;
        if (build.hasAtLeastOnePreviousNonAbortedAndCompletedBuild()) {
            Result previousResult = build.previousNonAbortedResult();
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
                    message = message + "\n" + getCommitList(build);
                }
                notification = new Notification(message, getResultColor(build.result()));
            }
        }
        return notification;
    }

    private boolean moreTestFailuresThanPreviousBuild(Build currentBuild, Build previousBuild) {
        if (currentBuild.hasTestResults() && previousBuild.hasTestResults()) {
            if (currentBuild.testResults().getFailed() > previousBuild.testResults().getFailed()) {
                return true;
            }

            // test if different tests failed.
            return !getFailedTestIds(currentBuild).equals(getFailedTestIds(previousBuild));
        }
        return false;
    }

    private Set<String> getFailedTestIds(Build build) {
        return build.testResults().getFailedTests().stream()
                .map(TestResults.Result::getId)
                .collect(Collectors.toSet());
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

    String getCommitList(Build build) {
        if (build.doesNotHaveChangeSetEntries()) {
            logger.info("Empty change...");
            if (build.doesNotHaveUpstreamCause()) {
                return "No Changes.";
            }
            if (build.upstreamExists()) {
                return getCommitList(build.upstream());
            }
        }
        Set<String> commits = new HashSet<>();
        for (Commit entry : build.changeSetEntries()) {
            StringBuilder commit = new StringBuilder();
            CommitInfoChoice commitInfoChoice = notifier.getCommitInfoChoice();
            if (commitInfoChoice.showTitle()) {
                commit.append(entry.getMessage());
            }
            if (commitInfoChoice.showAuthor()) {
                commit.append(" [").append(entry.getAuthor()).append("]");
            }
            commits.add(commit.toString());
        }
        MessageBuilder message = new MessageBuilder(notifier, build);
        message.append("Changes:\n- ");
        message.append(StringUtils.join(commits, "\n- "));
        return message.toString();
    }

    static String getResultColor(Result result) {
        return RESULT_COLOR.getOrDefault(result, "warning");
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
                if (build.hasAtLeastOnePreviousNonAbortedAndCompletedBuild()) {


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
                    if (build.hasAtLeastOnePreviousNonAbortedAndCompletedBuild() && previousResult != null && build.result().isWorseThan(previousResult)) {
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
