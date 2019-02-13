package jenkins.plugins.slack;

import java.util.Objects;

public class Commit {
    private final String author;
    private final String message;

    public Commit(String author, String message) {
        this.author = author;
        this.message = message;
    }

    public String getAuthor() {
        return author;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(author, commit.author) &&
                Objects.equals(message, commit.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(author, message);
    }

    @Override
    public String toString() {
        return "Commit{" +
                "author='" + author + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
