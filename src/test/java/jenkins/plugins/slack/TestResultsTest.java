package jenkins.plugins.slack;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.*;

public class TestResultsTest {

    @Test
    public void shouldDerivePassedTests() {
        TestResults subject = new TestResults(10, 3, 1, Collections.emptyList());
        int expected = 6;

        int actual = subject.getPassed();

        assertEquals(expected, actual);
    }

    @Test
    public void shouldNeverHaveNegativePassedTests() {
        TestResults subject = new TestResults(5, 5, 1, Collections.emptyList());
        int expected = 0; // computes to -1, forces us to create a floor

        int actual = subject.getPassed();

        assertEquals(expected, actual);
    }
}