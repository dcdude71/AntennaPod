package de.danoeh.antennapod.model.feed;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.model.feed.EpisodeCondition.Field;
import de.danoeh.antennapod.model.feed.EpisodeCondition.Operator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AdvancedEpisodeFilterTest {
    private static final int MINUTE = 60 * 1000;

    private static EpisodeCondition cond(Field field, Operator operator, String value) {
        return new EpisodeCondition(field, operator, value, false);
    }

    private static AdvancedEpisodeFilter skipAny(EpisodeCondition... conditions) {
        return new AdvancedEpisodeFilter(Collections.emptyList(), false, Arrays.asList(conditions), false);
    }

    private static AdvancedEpisodeFilter keep(boolean all, EpisodeCondition... conditions) {
        return new AdvancedEpisodeFilter(Arrays.asList(conditions), all, Collections.emptyList(), false);
    }

    @Test
    public void emptyFilterMatchesEverything() {
        AdvancedEpisodeFilter.Result result = AdvancedEpisodeFilter.empty().evaluate("Anything", null, 5 * MINUTE);
        assertTrue(result.matches);
        assertEquals(AdvancedEpisodeFilter.Reason.NO_CONDITIONS, result.reason);
    }

    @Test
    public void feedRinseStyleBlockList() {
        EpisodeCondition riou = cond(Field.POST, Operator.CONTAINS, "Riou");
        EpisodeCondition integrale = cond(Field.TITLE, Operator.NOT_CONTAINS, "L'INTÉGRALE");
        AdvancedEpisodeFilter f = skipAny(riou, cond(Field.POST, Operator.CONTAINS, "Thoen"), integrale);

        AdvancedEpisodeFilter.Result result = f.evaluate("L'intégrale du 3 mars", "Avec Jérémy Riou", 0);
        assertFalse(result.matches);
        assertEquals(AdvancedEpisodeFilter.Reason.SKIPPED, result.reason);
        assertEquals(riou, result.condition);

        assertEquals(integrale, f.evaluate("Le best-of", "", 0).condition);
        assertTrue(f.evaluate("L’integrale du 4 mars", "Avec Arnaud Tsamere", 0).matches);
    }

    @Test
    public void ignoresCaseAndAccents() {
        AdvancedEpisodeFilter f = keep(false, cond(Field.TITLE, Operator.CONTAINS, "integrale"));
        assertTrue(f.evaluate("L'INTÉGRALE", null, 0).matches);
    }

    @Test
    public void keepAnyAndAll() {
        EpisodeCondition climate = cond(Field.TITLE, Operator.CONTAINS, "climate");
        EpisodeCondition europe = cond(Field.TITLE, Operator.CONTAINS, "Europe");
        assertTrue(keep(false, climate, europe).evaluate("Climate summit", null, 0).matches);
        AdvancedEpisodeFilter.Result missing = keep(true, climate, europe).evaluate("Climate summit", null, 0);
        assertFalse(missing.matches);
        assertEquals(AdvancedEpisodeFilter.Reason.NOT_KEPT, missing.reason);
        assertEquals(europe, missing.condition);
        AdvancedEpisodeFilter.Result none = keep(false, climate, europe).evaluate("Markets", null, 0);
        assertFalse(none.matches);
        assertNull(none.condition);
    }

    @Test
    public void skipWinsOverKeep() {
        AdvancedEpisodeFilter f = new AdvancedEpisodeFilter(
                List.of(cond(Field.TITLE, Operator.CONTAINS, "Ukraine")), false,
                List.of(cond(Field.TITLE, Operator.CONTAINS, "bonus")), false);
        assertEquals(AdvancedEpisodeFilter.Reason.SKIPPED, f.evaluate("Bonus: Ukraine questions", null, 0).reason);
        assertEquals(AdvancedEpisodeFilter.Reason.KEPT, f.evaluate("Ukraine today", null, 0).reason);
    }

    @Test
    public void fields() {
        String description = "<p>A talk about the <b>climate</b></p>";
        assertFalse(keep(false, cond(Field.TITLE, Operator.CONTAINS, "climate"))
                .evaluate("Hottest year", description, 0).matches);
        assertTrue(keep(false, cond(Field.DESCRIPTION, Operator.CONTAINS, "climate"))
                .evaluate("Hottest year", description, 0).matches);
        assertTrue(keep(false, cond(Field.POST, Operator.CONTAINS, "climate"))
                .evaluate("Hottest year", description, 0).matches);
        assertTrue(keep(false, cond(Field.POST, Operator.CONTAINS, "hottest"))
                .evaluate("Hottest year", description, 0).matches);
    }

    @Test
    public void operators() {
        assertFalse(keep(false, cond(Field.TITLE, Operator.STARTS_WITH, "best of"))
                .evaluate("Best-of: best of the week", null, 0).matches);
        assertTrue(keep(false, cond(Field.TITLE, Operator.STARTS_WITH, "best"))
                .evaluate("Best of the week", null, 0).matches);
        assertTrue(keep(false, cond(Field.TITLE, Operator.ENDS_WITH, "(rediff)"))
                .evaluate("Episode 12 (REDIFF)", null, 0).matches);
        assertTrue(keep(false, cond(Field.TITLE, Operator.IS, "trailer"))
                .evaluate(" Trailer ", null, 0).matches);
        assertFalse(keep(false, cond(Field.TITLE, Operator.IS, "trailer"))
                .evaluate("Trailer 2", null, 0).matches);
    }

    @Test
    public void wholeWords() {
        EpisodeCondition partial = cond(Field.TITLE, Operator.CONTAINS, "war");
        EpisodeCondition whole = new EpisodeCondition(Field.TITLE, Operator.CONTAINS, "war", true);
        assertTrue(keep(false, partial).evaluate("Award show", null, 0).matches);
        assertFalse(keep(false, whole).evaluate("Award show", null, 0).matches);
        assertTrue(keep(false, whole).evaluate("The war, explained", null, 0).matches);
        EpisodeCondition startsWhole = new EpisodeCondition(Field.TITLE, Operator.STARTS_WITH, "best", true);
        assertFalse(keep(false, startsWhole).evaluate("Bestiary", null, 0).matches);
    }

    @Test
    public void length() {
        AdvancedEpisodeFilter f = skipAny(EpisodeCondition.length(Operator.SHORTER_THAN, 5),
                EpisodeCondition.length(Operator.LONGER_THAN, 45));
        assertFalse(f.evaluate("x", null, 4 * MINUTE).matches);
        assertFalse(f.evaluate("x", null, 60 * MINUTE).matches);
        assertTrue(f.evaluate("x", null, 30 * MINUTE).matches);
        assertTrue(f.evaluate("x", null, 0).matches);
        AdvancedEpisodeFilter keepLong = keep(true, EpisodeCondition.length(Operator.LONGER_THAN, 10));
        assertTrue(keepLong.evaluate("x", null, 0).matches);
        assertFalse(keepLong.evaluate("x", null, 5 * MINUTE).matches);
    }

    @Test
    public void invalidConditionsAreIgnored() {
        AdvancedEpisodeFilter f = skipAny(cond(Field.TITLE, Operator.CONTAINS, "  "),
                cond(Field.LENGTH, Operator.SHORTER_THAN, "abc"));
        assertTrue(f.isEmpty());
    }

    @Test
    public void convertsLegacyFilter() {
        AdvancedEpisodeFilter f = AdvancedEpisodeFilter.fromLegacy(List.of("Ukraine"), true, List.of("bonus"),
                true, true, true, 600, 3600);
        assertEquals(List.of(new EpisodeCondition(Field.POST, Operator.CONTAINS, "Ukraine", true)),
                f.getKeepConditions());
        assertTrue(f.isKeepMatchAll());
        assertEquals(List.of(new EpisodeCondition(Field.POST, Operator.CONTAINS, "bonus", true),
                EpisodeCondition.length(Operator.SHORTER_THAN, 10),
                EpisodeCondition.length(Operator.LONGER_THAN, 60)), f.getSkipConditions());
        assertFalse(f.isSkipMatchAll());
    }

    @Test
    public void startsFromBasicFilter() {
        FeedFilter basic = new FeedFilter("\"bonus episode\" trailer", "", 600);
        AdvancedEpisodeFilter f = AdvancedEpisodeFilter.fromBasic(basic);
        assertEquals(List.of(EpisodeCondition.contains(Field.TITLE, "bonus episode", false),
                EpisodeCondition.contains(Field.TITLE, "trailer", false)), f.getKeepConditions());
        assertEquals(List.of(EpisodeCondition.length(Operator.SHORTER_THAN, 10)), f.getSkipConditions());
    }
}
