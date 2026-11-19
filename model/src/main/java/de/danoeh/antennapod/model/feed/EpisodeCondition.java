package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public final class EpisodeCondition {
    public enum Field {
        POST, TITLE, DESCRIPTION, LENGTH
    }

    public enum Operator {
        CONTAINS, NOT_CONTAINS, STARTS_WITH, ENDS_WITH, IS, SHORTER_THAN, LONGER_THAN;

        public boolean isLength() {
            return this == SHORTER_THAN || this == LONGER_THAN;
        }
    }

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    @NonNull
    private final Field field;
    @NonNull
    private final Operator operator;
    @NonNull
    private final String value;
    private final boolean wholeWord;

    public EpisodeCondition(@NonNull Field field, @NonNull Operator operator, @Nullable String value,
                            boolean wholeWord) {
        this.field = field;
        this.operator = operator;
        this.value = value == null ? "" : value.trim();
        this.wholeWord = wholeWord;
    }

    @NonNull
    public static EpisodeCondition contains(@NonNull Field field, @NonNull String value, boolean wholeWord) {
        return new EpisodeCondition(field, Operator.CONTAINS, value, wholeWord);
    }

    @NonNull
    public static EpisodeCondition length(@NonNull Operator operator, int minutes) {
        return new EpisodeCondition(Field.LENGTH, operator, String.valueOf(minutes), false);
    }

    @NonNull
    public Field getField() {
        return field;
    }

    @NonNull
    public Operator getOperator() {
        return operator;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    public boolean isWholeWord() {
        return wholeWord;
    }

    public int getMinutes() {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean isValid() {
        if (field == Field.LENGTH) {
            return operator.isLength() && getMinutes() > 0;
        }
        return !operator.isLength() && !normalize(value).isEmpty();
    }

    public boolean usesDescription() {
        return field == Field.POST || field == Field.DESCRIPTION;
    }

    @Nullable
    Boolean test(@NonNull String normalizedTitle, @NonNull String normalizedDescription, int durationMillis) {
        if (field == Field.LENGTH) {
            if (durationMillis <= 0) {
                return null;
            }
            int seconds = durationMillis / 1000;
            int limit = getMinutes() * 60;
            return operator == Operator.SHORTER_THAN ? seconds < limit : seconds > limit;
        }
        String term = normalize(value);
        if (operator == Operator.NOT_CONTAINS) {
            return !textMatches(Operator.CONTAINS, normalizedTitle, normalizedDescription, term);
        }
        return textMatches(operator, normalizedTitle, normalizedDescription, term);
    }

    private boolean textMatches(@NonNull Operator op, @NonNull String title, @NonNull String description,
                                @NonNull String term) {
        switch (field) {
            case TITLE:
                return matches(op, title, term);
            case DESCRIPTION:
                return matches(op, description, term);
            case POST:
            default:
                return matches(op, title, term) || matches(op, description, term);
        }
    }

    private boolean matches(@NonNull Operator op, @NonNull String text, @NonNull String term) {
        if (text.isEmpty()) {
            return false;
        }
        if (op == Operator.IS) {
            return text.equals(term);
        }
        String quoted = Pattern.quote(term);
        String before = wholeWord ? "(?<![\\p{L}\\p{N}])" : "";
        String after = wholeWord ? "(?![\\p{L}\\p{N}])" : "";
        String regex;
        if (op == Operator.STARTS_WITH) {
            regex = "^" + quoted + after;
        } else if (op == Operator.ENDS_WITH) {
            regex = before + quoted + "$";
        } else if (wholeWord) {
            regex = before + quoted + after;
        } else {
            return text.contains(term);
        }
        return Pattern.compile(regex).matcher(text).find();
    }

    @NonNull
    public static String normalize(@Nullable String text) {
        if (text == null) {
            return "";
        }
        String stripped = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        stripped = stripped.replace('’', '\'').replace('‘', '\'').replace('ʼ', '\'')
                .replace('“', '"').replace('”', '"');
        return SPACES.matcher(stripped.toLowerCase(Locale.ROOT)).replaceAll(" ").trim();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EpisodeCondition)) {
            return false;
        }
        EpisodeCondition other = (EpisodeCondition) o;
        return field == other.field && operator == other.operator && value.equals(other.value)
                && wholeWord == other.wholeWord;
    }

    @Override
    public int hashCode() {
        return ((field.hashCode() * 31 + operator.hashCode()) * 31 + value.hashCode()) * 31 + (wholeWord ? 1 : 0);
    }
}
