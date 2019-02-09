package jenkins.plugins.slack;

import java.util.Objects;

public class Notification {
    private final String message;
    private final String color;

    public static Notification good(String message) {
        return new Notification(message, "good");
    }

    public Notification(String message, String color) {
        this.message = message;
        this.color = color;
    }

    public String getMessage() {
        return message;
    }

    public String getColor() {
        return color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return Objects.equals(message, that.message) &&
                Objects.equals(color, that.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, color);
    }

    @Override
    public String toString() {
        return "[" + color + "] " + message;
    }
}
