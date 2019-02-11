package jenkins.plugins.slack;

import hudson.tasks.test.TestResult;

import java.util.List;

public class TestResults {
    private final int total;
    private final int failed;
    private final int skipped;
    private final List<Result> failedTests;

    public TestResults(int total, int failed, int skipped, List<Result> failedTests) {
        this.total = total;
        this.failed = failed;
        this.skipped = skipped;
        this.failedTests = failedTests;
    }

    public int getTotal() {
        return total;
    }

    public int getFailed() {
        return failed;
    }

    public int getSkipped() {
        return skipped;
    }

    public int getPassed() {
        return Math.max(total - failed - skipped, 0);
    }

    public List<Result> getFailedTests() {
        return failedTests;
    }

    public static class Result {
        private final String displayName;
        private final String humanDuration;

        public static Result valueOf(TestResult testResult) {
            return new Result(testResult.getFullDisplayName(), testResult.getDurationString());
        }

        public Result(String displayName, String humanDuration) {
            this.displayName = displayName;
            this.humanDuration = humanDuration;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getHumanDuration() {
            return humanDuration;
        }
    }
}
