package com.griefprevention.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformDetectionTest
{

    @Test
    void classExists_returnsTrue_forExistingClass()
    {
        assertTrue(PlatformDetection.classExists("java.lang.String"));
        assertTrue(PlatformDetection.classExists("java.util.List"));
        assertTrue(PlatformDetection.classExists("org.bukkit.Bukkit"));
    }

    @Test
    void classExists_returnsFalse_forNonExistentClass()
    {
        assertFalse(PlatformDetection.classExists("com.example.NonExistentClass"));
        assertFalse(PlatformDetection.classExists("org.bukkit.NotARealClass"));
        assertFalse(PlatformDetection.classExists(""));
    }

    @Test
    void getPlatform_returnsNonNull()
    {
        PlatformDetection.Platform platform = PlatformDetection.getPlatform();
        assertNotNull(platform);
    }

    @Test
    void getPlatform_returnsCachedValue()
    {
        // Call twice to verify caching (same instance should be returned)
        PlatformDetection.Platform first = PlatformDetection.getPlatform();
        PlatformDetection.Platform second = PlatformDetection.getPlatform();
        assertEquals(first, second);
    }

    @Test
    void capabilityChecks_returnStableValues()
    {
        // These key off classExists / cached booleans and must never throw,
        // and must be stable across repeated calls.
        assertEquals(PlatformDetection.isFolia(), PlatformDetection.isFolia());
        assertEquals(PlatformDetection.hasAsyncScheduler(), PlatformDetection.hasAsyncScheduler());
        assertEquals(PlatformDetection.hasAdventureComponentApi(), PlatformDetection.hasAdventureComponentApi());
        assertEquals(PlatformDetection.isPaper(), PlatformDetection.getPlatform() == PlatformDetection.Platform.PAPER);
    }

    @Test
    void asyncScheduler_impliesPaper()
    {
        // The async scheduler is a Paper-only capability.
        if (PlatformDetection.hasAsyncScheduler())
        {
            assertTrue(PlatformDetection.isPaper());
        }
    }

    @Test
    void parseMajorVersion_readsLeadingMajor()
    {
        assertEquals(1, PlatformDetection.parseMajorVersion("1.21.11-R0.1-SNAPSHOT"));
        assertEquals(1, PlatformDetection.parseMajorVersion("1.21.11"));
        assertEquals(26, PlatformDetection.parseMajorVersion("26.1.2-R0.1-SNAPSHOT"));
        assertEquals(26, PlatformDetection.parseMajorVersion("26.2.build.53-alpha"));
        assertEquals(27, PlatformDetection.parseMajorVersion("27.0.1"));
        assertEquals(-1, PlatformDetection.parseMajorVersion(""));
        assertEquals(-1, PlatformDetection.parseMajorVersion("unknown"));
    }

    @Test
    void isAtLeastPaper26x_distinguishesReleaseLines()
    {
        assertFalse(PlatformDetection.isAtLeastPaper26x("1.21.11-R0.1-SNAPSHOT"));
        assertFalse(PlatformDetection.isAtLeastPaper26x("1.20.6"));
        assertTrue(PlatformDetection.isAtLeastPaper26x("26.1.2-R0.1-SNAPSHOT"));
        assertTrue(PlatformDetection.isAtLeastPaper26x("26.2.build.53-alpha"));
        assertTrue(PlatformDetection.isAtLeastPaper26x("27.0.0"));
    }

}