package de.danoeh.antennapod.net.sync.wearinterface;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CastlistWearAdSkipTest {
    @Test
    public void testPathRoundTrip() {
        assertEquals(Long.valueOf(42L), CastlistWearAdSkip.parseAdSkipPath(CastlistWearAdSkip.adSkipPath(42L)));
        assertNull(CastlistWearAdSkip.parseAdSkipPath(CastlistWearAdSkip.SKIP_AD));
        assertNull(CastlistWearAdSkip.parseAdSkipPath(CastlistWearAdSkip.AD_SKIP_PREFIX + "x"));
    }

    @Test
    public void testSecondsRoundTrip() {
        assertEquals(30, CastlistWearAdSkip.secondsFromBytes(CastlistWearAdSkip.secondsToBytes(30)));
        assertEquals(0, CastlistWearAdSkip.secondsFromBytes(null));
        assertEquals(0, CastlistWearAdSkip.secondsFromBytes("bad".getBytes(StandardCharsets.UTF_8)));
    }
}
