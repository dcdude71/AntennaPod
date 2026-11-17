package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class AdvancedEpisodeFilter {
    public static final int NO_LIMIT = -1;

    public enum Reason {
        NO_WORDS, INCLUDED, EXCLUDED, MISSING_WORD, NO_WORD_FOUND, TOO_SHORT, TOO_LONG
    }

    public static final class Result {
        public final boolean matches;
        @NonNull
        public final Reason reason;
        @Nullable
        public final String term;
        public final boolean inDescription;

        Result(boolean matches, @NonNull Reason reason, @Nullable String term, boolean inDescription) {
            this.matches = matches;
            this.reason = reason;
            this.term = term;
            this.inDescription = inDescription;
        }
    }

    private final List<String> includeTerms;
    private final boolean matchAll;
    private final List<String> excludeTerms;
    private final boolean searchTitle;
    private final boolean searchDescription;
    private final boolean wholeWords;
    private final int minDurationSeconds;
    private final int maxDurationSeconds;

    public AdvancedEpisodeFilter(@NonNull List<String> includeTerms, boolean matchAll,
                                 @NonNull List<String> excludeTerms, boolean searchTitle, boolean searchDescription,
                                 boolean wholeWords, int minDurationSeconds, int maxDurationSeconds) {
        this.includeTerms = cleanTerms(includeTerms);
        this.matchAll = matchAll;
        this.excludeTerms = cleanTerms(excludeTerms);
        this.searchTitle = searchTitle;
        this.searchDescription = searchDescription;
        this.wholeWords = wholeWords;
        this.minDurationSeconds = minDurationSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
    }

    @NonNull
    public static AdvancedEpisodeFilter fromBasic(@Nullable FeedFilter basic) {
        if (basic == null) {
            return new AdvancedEpisodeFilter(Collections.emptyList(), false, Collections.emptyList(),
                    true, false, false, NO_LIMIT, NO_LIMIT);
        }
        int minimal = basic.hasMinimalDurationFilter() ? basic.getMinimalDurationFilter() : NO_LIMIT;
        return new AdvancedEpisodeFilter(basic.getIncludeFilter(), false, basic.getExcludeFilter(),
                true, false, false, minimal, NO_LIMIT);
    }

    @NonNull
    public List<String> getIncludeTerms() {
        return new ArrayList<>(includeTerms);
    }

    public boolean isMatchAll() {
        return matchAll;
    }

    @NonNull
    public List<String> getExcludeTerms() {
        return new ArrayList<>(excludeTerms);
    }

    public boolean isSearchTitle() {
        return searchTitle;
    }

    public boolean isSearchDescription() {
        return searchDescription;
    }

    public boolean isWholeWords() {
        return wholeWords;
    }

    public int getMinDurationSeconds() {
        return minDurationSeconds;
    }

    public int getMaxDurationSeconds() {
        return maxDurationSeconds;
    }

    public boolean isEmpty() {
        return includeTerms.isEmpty() && excludeTerms.isEmpty()
                && minDurationSeconds <= 0 && maxDurationSeconds <= 0;
    }

    public boolean needsDescription() {
        return searchDescription && (!includeTerms.isEmpty() || !excludeTerms.isEmpty());
    }

    @NonNull
    public Result evaluate(@Nullable String title, @Nullable String description, int durationMillis) {
        int seconds = durationMillis / 1000;
        if (durationMillis > 0 && minDurationSeconds > 0 && seconds < minDurationSeconds) {
            return new Result(false, Reason.TOO_SHORT, null, false);
        }
        if (durationMillis > 0 && maxDurationSeconds > 0 && seconds > maxDurationSeconds) {
            return new Result(false, Reason.TOO_LONG, null, false);
        }
        String titleText = searchTitle && title != null ? title : "";
        String descriptionText = searchDescription && description != null ? toPlainText(description) : "";

        for (String term : excludeTerms) {
            if (contains(titleText, term)) {
                return new Result(false, Reason.EXCLUDED, term, false);
            }
            if (contains(descriptionText, term)) {
                return new Result(false, Reason.EXCLUDED, term, true);
            }
        }
        if (includeTerms.isEmpty()) {
            return new Result(true, Reason.NO_WORDS, null, false);
        }
        String firstFound = null;
        boolean firstInDescription = false;
        for (String term : includeTerms) {
            boolean inTitle = contains(titleText, term);
            boolean inDescription = !inTitle && contains(descriptionText, term);
            if (inTitle || inDescription) {
                if (!matchAll) {
                    return new Result(true, Reason.INCLUDED, term, inDescription);
                }
                if (firstFound == null) {
                    firstFound = term;
                    firstInDescription = inDescription;
                }
            } else if (matchAll) {
                return new Result(false, Reason.MISSING_WORD, term, false);
            }
        }
        if (matchAll) {
            return new Result(true, Reason.INCLUDED, firstFound, firstInDescription);
        }
        return new Result(false, Reason.NO_WORD_FOUND, null, false);
    }

    private boolean contains(@NonNull String text, @NonNull String term) {
        if (text.isEmpty()) {
            return false;
        }
        if (!wholeWords) {
            return text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
        }
        Pattern pattern = Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(term) + "(?![\\p{L}\\p{N}])",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        return pattern.matcher(text).find();
    }

    @NonNull
    static String toPlainText(@NonNull String html) {
        return html.replaceAll("<[^>]*>", " ")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    @NonNull
    private static List<String> cleanTerms(@NonNull List<String> terms) {
        List<String> cleaned = new ArrayList<>();
        for (String term : terms) {
            String trimmed = term == null ? "" : term.trim();
            if (!trimmed.isEmpty() && !cleaned.contains(trimmed)) {
                cleaned.add(trimmed);
            }
        }
        return cleaned;
    }
}
