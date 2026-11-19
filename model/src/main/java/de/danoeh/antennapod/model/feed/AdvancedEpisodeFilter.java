package de.danoeh.antennapod.model.feed;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdvancedEpisodeFilter {
    public static final int NO_LIMIT = -1;

    public enum Reason {
        NO_CONDITIONS, KEPT, NOT_KEPT, SKIPPED, NOT_SKIPPED
    }

    public static final class Result {
        public final boolean matches;
        @NonNull
        public final Reason reason;
        @Nullable
        public final EpisodeCondition condition;

        Result(boolean matches, @NonNull Reason reason, @Nullable EpisodeCondition condition) {
            this.matches = matches;
            this.reason = reason;
            this.condition = condition;
        }
    }

    private final List<EpisodeCondition> keepConditions;
    private final boolean keepMatchAll;
    private final List<EpisodeCondition> skipConditions;
    private final boolean skipMatchAll;

    public AdvancedEpisodeFilter(@NonNull List<EpisodeCondition> keepConditions, boolean keepMatchAll,
                                 @NonNull List<EpisodeCondition> skipConditions, boolean skipMatchAll) {
        this.keepConditions = clean(keepConditions);
        this.keepMatchAll = keepMatchAll;
        this.skipConditions = clean(skipConditions);
        this.skipMatchAll = skipMatchAll;
    }

    @NonNull
    public static AdvancedEpisodeFilter empty() {
        return new AdvancedEpisodeFilter(Collections.emptyList(), false, Collections.emptyList(), false);
    }

    @NonNull
    public static AdvancedEpisodeFilter fromBasic(@Nullable FeedFilter basic) {
        if (basic == null) {
            return empty();
        }
        int minimal = basic.hasMinimalDurationFilter() ? basic.getMinimalDurationFilter() : NO_LIMIT;
        return fromLegacy(basic.getIncludeFilter(), false, basic.getExcludeFilter(),
                true, false, false, minimal, NO_LIMIT);
    }

    @NonNull
    public static AdvancedEpisodeFilter fromLegacy(@NonNull List<String> includeTerms, boolean matchAll,
                                                   @NonNull List<String> excludeTerms, boolean searchTitle,
                                                   boolean searchDescription, boolean wholeWords,
                                                   int minDurationSeconds, int maxDurationSeconds) {
        EpisodeCondition.Field field = searchTitle && searchDescription ? EpisodeCondition.Field.POST
                : searchDescription ? EpisodeCondition.Field.DESCRIPTION : EpisodeCondition.Field.TITLE;
        List<EpisodeCondition> keep = new ArrayList<>();
        for (String term : includeTerms) {
            keep.add(EpisodeCondition.contains(field, term, wholeWords));
        }
        List<EpisodeCondition> skip = new ArrayList<>();
        for (String term : excludeTerms) {
            skip.add(EpisodeCondition.contains(field, term, wholeWords));
        }
        if (minDurationSeconds > 0) {
            skip.add(EpisodeCondition.length(EpisodeCondition.Operator.SHORTER_THAN,
                    Math.max(1, minDurationSeconds / 60)));
        }
        if (maxDurationSeconds > 0) {
            skip.add(EpisodeCondition.length(EpisodeCondition.Operator.LONGER_THAN,
                    Math.max(1, maxDurationSeconds / 60)));
        }
        return new AdvancedEpisodeFilter(keep, matchAll, skip, false);
    }

    @NonNull
    public List<EpisodeCondition> getKeepConditions() {
        return new ArrayList<>(keepConditions);
    }

    public boolean isKeepMatchAll() {
        return keepMatchAll;
    }

    @NonNull
    public List<EpisodeCondition> getSkipConditions() {
        return new ArrayList<>(skipConditions);
    }

    public boolean isSkipMatchAll() {
        return skipMatchAll;
    }

    public boolean isEmpty() {
        return keepConditions.isEmpty() && skipConditions.isEmpty();
    }

    public boolean needsDescription() {
        for (EpisodeCondition condition : keepConditions) {
            if (condition.usesDescription()) {
                return true;
            }
        }
        for (EpisodeCondition condition : skipConditions) {
            if (condition.usesDescription()) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public Result evaluate(@Nullable String title, @Nullable String description, int durationMillis) {
        if (isEmpty()) {
            return new Result(true, Reason.NO_CONDITIONS, null);
        }
        String normalizedTitle = EpisodeCondition.normalize(title);
        String normalizedDescription = needsDescription() && description != null
                ? EpisodeCondition.normalize(toPlainText(description)) : "";

        EpisodeCondition kept = null;
        if (!keepConditions.isEmpty()) {
            EpisodeCondition decisive = null;
            boolean keep = keepMatchAll;
            for (EpisodeCondition condition : keepConditions) {
                Boolean holds = condition.test(normalizedTitle, normalizedDescription, durationMillis);
                boolean passes = holds == null || holds;
                if (keepMatchAll && !passes) {
                    keep = false;
                    decisive = condition;
                    break;
                }
                if (!keepMatchAll && passes) {
                    keep = true;
                    decisive = condition;
                    break;
                }
            }
            if (!keep) {
                return new Result(false, Reason.NOT_KEPT, decisive);
            }
            kept = keepMatchAll ? null : decisive;
        }

        if (!skipConditions.isEmpty()) {
            EpisodeCondition decisive = null;
            boolean skip = skipMatchAll;
            for (EpisodeCondition condition : skipConditions) {
                Boolean holds = condition.test(normalizedTitle, normalizedDescription, durationMillis);
                boolean applies = holds != null && holds;
                if (skipMatchAll && !applies) {
                    skip = false;
                    break;
                }
                if (!skipMatchAll && applies) {
                    skip = true;
                    decisive = condition;
                    break;
                }
            }
            if (skip) {
                return new Result(false, Reason.SKIPPED, decisive);
            }
            if (keepConditions.isEmpty()) {
                return new Result(true, Reason.NOT_SKIPPED, null);
            }
        }
        return new Result(true, Reason.KEPT, kept);
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
    private static List<EpisodeCondition> clean(@NonNull List<EpisodeCondition> conditions) {
        List<EpisodeCondition> cleaned = new ArrayList<>();
        for (EpisodeCondition condition : conditions) {
            if (condition != null && condition.isValid() && !cleaned.contains(condition)) {
                cleaned.add(condition);
            }
        }
        return cleaned;
    }
}
