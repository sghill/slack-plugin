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
        private final String id;
        private final String displayName;
        private final String humanDuration;

        public static Result valueOf(TestResult r) {
            return new Result(r.getId(), r.getFullDisplayName(), r.getDurationString());
        }

        public Result(String id, String displayName, String humanDuration) {
            this.id = id;
            this.displayName = displayName;
            this.humanDuration = humanDuration;
        }

        public String getId() {
            return id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getHumanDuration() {
            return humanDuration;
        }
    }
}
