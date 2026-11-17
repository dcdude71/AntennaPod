package de.danoeh.antennapod.model.feed;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AdvancedEpisodeFilterTest {
    private static final int MINUTE = 60 * 1000;

    private static AdvancedEpisodeFilter filter(List<String> include, boolean matchAll, List<String> exclude,
                                                boolean title, boolean description, boolean wholeWords,
                                                int min, int max) {
        return new AdvancedEpisodeFilter(include, matchAll, exclude, title, description, wholeWords, min, max);
    }

    @Test
    public void emptyFilterMatchesEverything() {
        AdvancedEpisodeFilter empty = filter(Collections.emptyList(), false, Collections.emptyList(),
                true, false, false, -1, -1);
        assertTrue(empty.isEmpty());
        AdvancedEpisodeFilter.Result result = empty.evaluate("Anything", null, 5 * MINUTE);
        assertTrue(result.matches);
        assertEquals(AdvancedEpisodeFilter.Reason.NO_WORDS, result.reason);
    }

    @Test
    public void excludeWinsOverInclude() {
        AdvancedEpisodeFilter f = filter(Arrays.asList("Ukraine"), false, Arrays.asList("bonus"),
                true, false, false, -1, -1);
        AdvancedEpisodeFilter.Result result = f.evaluate("Bonus: Ukraine questions", null, 0);
        assertFalse(result.matches);
        assertEquals(AdvancedEpisodeFilter.Reason.EXCLUDED, result.reason);
        assertEquals("bonus", result.term);
        assertTrue(f.evaluate("Ukraine talks", null, 0).matches);
    }

    @Test
    public void anyVersusAll() {
        List<String> words = Arrays.asList("climate", "bill");
        AdvancedEpisodeFilter any = filter(words, false, Collections.emptyList(), true, false, false, -1, -1);
        AdvancedEpisodeFilter all = filter(words, true, Collections.emptyList(), true, false, false, -1, -1);
        assertTrue(any.evaluate("Climate summit", null, 0).matches);
        AdvancedEpisodeFilter.Result missing = all.evaluate("Climate summit", null, 0);
        assertFalse(missing.matches);
        assertEquals(AdvancedEpisodeFilter.Reason.MISSING_WORD, missing.reason);
        assertEquals("bill", missing.term);
        assertTrue(all.evaluate("Floods and the climate bill", null, 0).matches);
        assertEquals(AdvancedEpisodeFilter.Reason.NO_WORD_FOUND, any.evaluate("Markets", null, 0).reason);
    }

    @Test
    public void searchesDescriptionOnlyWhenAsked() {
        List<String> words = Arrays.asList("climate");
        AdvancedEpisodeFilter titleOnly = filter(words, false, Collections.emptyList(),
                true, false, false, -1, -1);
        AdvancedEpisodeFilter both = filter(words, false, Collections.emptyList(),
                true, true, false, -1, -1);
        String description = "<p>A record year for the <b>climate</b> &amp; oceans</p>";
        assertFalse(titleOnly.evaluate("Hottest year", description, 0).matches);
        AdvancedEpisodeFilter.Result result = both.evaluate("Hottest year", description, 0);
        assertTrue(result.matches);
        assertTrue(result.inDescription);
        assertTrue(both.needsDescription());
        assertFalse(titleOnly.needsDescription());
    }

    @Test
    public void wholeWords() {
        List<String> words = Arrays.asList("war");
        AdvancedEpisodeFilter anywhere = filter(words, false, Collections.emptyList(),
                true, false, false, -1, -1);
        AdvancedEpisodeFilter whole = filter(words, false, Collections.emptyList(),
                true, false, true, -1, -1);
        assertTrue(anywhere.evaluate("An award show", null, 0).matches);
        assertFalse(whole.evaluate("An award show", null, 0).matches);
        assertTrue(whole.evaluate("The war, explained", null, 0).matches);
        assertTrue(whole.evaluate("WAR", null, 0).matches);
    }

    @Test
    public void durationRange() {
        AdvancedEpisodeFilter f = filter(Collections.emptyList(), false, Collections.emptyList(),
                true, false, false, 10 * 60, 45 * 60);
        assertEquals(AdvancedEpisodeFilter.Reason.TOO_SHORT, f.evaluate("x", null, 4 * MINUTE).reason);
        assertEquals(AdvancedEpisodeFilter.Reason.TOO_LONG, f.evaluate("x", null, 60 * MINUTE).reason);
        assertTrue(f.evaluate("x", null, 30 * MINUTE).matches);
        assertTrue(f.evaluate("x", null, 0).matches);
    }

    @Test
    public void startsFromBasicFilter() {
        FeedFilter basic = new FeedFilter("\"bonus episode\" trailer", "", 600);
        AdvancedEpisodeFilter f = AdvancedEpisodeFilter.fromBasic(basic);
        assertEquals(Arrays.asList("bonus episode", "trailer"), f.getIncludeTerms());
        assertEquals(600, f.getMinDurationSeconds());
        assertTrue(f.isSearchTitle());
        assertFalse(f.isSearchDescription());
    }
}
