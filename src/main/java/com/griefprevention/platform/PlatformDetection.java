package com.griefprevention.platform;

import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

/**
 * Central utility for detecting server platform and API capabilities at runtime.
 * <p>
 * GriefPrevention compiles against the Paper API but must also run on Spigot,
 * where Paper-only classes (the async scheduler, Adventure {@code Component},
 * Folia's regionized schedulers) are absent. Rather than assume a capability is
 * present because it compiled, callers should gate Paper-only or version-gated
 * code paths behind the checks provided here.
 * <p>
 * Detection results are cached to avoid repeated reflection and version parsing.
 *
 * @see PlatformListener for adding platform-specific listeners
 */
public final class PlatformDetection
{

    /**
     * Server platforms that the plugin can detect and adapt to.
     */
    public enum Platform
    {
        /** Paper and Paper forks (Purpur, Pufferfish, Folia, etc.) */
        PAPER,
        /** Spigot and Spigot-based servers without Paper API */
        SPIGOT
    }

    private static Platform detectedPlatform = null;
    private static Boolean folia = null;
    private static Boolean adventureComponentApi = null;
    private static Boolean asyncScheduler = null;
    private static Boolean atLeastPaper26x = null;

    private PlatformDetection()
    {
    }

    /**
     * Detects and returns the server platform.
     * Result is cached after first detection.
     *
     * @return the detected platform
     */
    public static @NotNull Platform getPlatform()
    {
        if (detectedPlatform == null)
        {
            detectedPlatform = detectPlatform();
        }
        return detectedPlatform;
    }

    private static @NotNull Platform detectPlatform()
    {
        if (classExists("com.destroystokyo.paper.PaperConfig")
                || classExists("io.papermc.paper.configuration.Configuration"))
        {
            return Platform.PAPER;
        }
        return Platform.SPIGOT;
    }

    /**
     * @return {@code true} if running on Paper or a Paper fork
     */
    public static boolean isPaper()
    {
        return getPlatform() == Platform.PAPER;
    }

    /**
     * Checks whether the server is running Folia (regionized threading).
     * <p>
     * On Folia, {@link org.bukkit.scheduler.BukkitScheduler} methods such as
     * {@code runTaskAsynchronously} throw {@link UnsupportedOperationException};
     * scheduling must go through the regionized schedulers instead. Result is
     * cached after first detection.
     *
     * @return {@code true} if running on Folia
     */
    public static boolean isFolia()
    {
        if (folia == null)
        {
            folia = classExists("io.papermc.paper.threadedregions.RegionizedServer");
        }
        return folia;
    }

    /**
     * Checks whether the Adventure {@code Component} chat API is available.
     * <p>
     * Always {@code true} on modern Paper, but absent on Spigot, so player-facing
     * {@code Component} output must be guarded on the Spigot code path.
     *
     * @return {@code true} if Adventure components can be sent to players
     */
    public static boolean hasAdventureComponentApi()
    {
        if (adventureComponentApi == null)
        {
            adventureComponentApi = classExists("net.kyori.adventure.text.Component");
        }
        return adventureComponentApi;
    }

    /**
     * Checks whether Paper's async scheduler
     * ({@code Bukkit.getAsyncScheduler()}) is available.
     * <p>
     * Absent on Spigot, where {@link org.bukkit.scheduler.BukkitScheduler}'s
     * asynchronous task methods remain the only option.
     *
     * @return {@code true} if the Paper async scheduler is available
     */
    public static boolean hasAsyncScheduler()
    {
        if (asyncScheduler == null)
        {
            asyncScheduler = isPaper()
                    && classExists("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
        }
        return asyncScheduler;
    }

    /**
     * Checks whether the running server is Paper 26.x (the year-based release
     * line) or newer.
     * <p>
     * Paper moved from the {@code 1.21.x} scheme to a year-based
     * {@code 26.x}/{@code 27.x} scheme. Use this to gate 26.x-only API calls;
     * pair it with a {@link LinkageError} guard as described in the migration
     * notes. Result is cached after first detection.
     *
     * @return {@code true} if on Paper 26.x or later
     */
    public static boolean isAtLeastPaper26x()
    {
        if (atLeastPaper26x == null)
        {
            atLeastPaper26x = isPaper() && isAtLeastPaper26x(Bukkit.getBukkitVersion());
        }
        return atLeastPaper26x;
    }

    /**
     * Version-string test for the 26.x (year-based) Paper release line, split
     * out for testing without a live server.
     *
     * @param bukkitVersion a value like {@code "1.21.11-R0.1-SNAPSHOT"} or
     *                      {@code "26.1.2-R0.1-SNAPSHOT"}
     * @return {@code true} if the leading major version is 26 or greater
     */
    static boolean isAtLeastPaper26x(@NotNull String bukkitVersion)
    {
        return parseMajorVersion(bukkitVersion) >= 26;
    }

    /**
     * Extracts the leading numeric major version from a Bukkit/Minecraft version
     * string. The classic scheme ({@code 1.21.11}) yields {@code 1}; the
     * year-based scheme ({@code 26.1.2}) yields {@code 26}.
     *
     * @param version the version string
     * @return the leading major version, or {@code -1} if none could be parsed
     */
    static int parseMajorVersion(@NotNull String version)
    {
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end)))
        {
            end++;
        }
        if (end == 0)
        {
            return -1;
        }
        try
        {
            return Integer.parseInt(version.substring(0, end));
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }

    /**
     * Checks if a class exists on the classpath.
     * <p>
     * Useful for checking if platform-specific APIs are available
     * before attempting to use them.
     *
     * @param className the fully qualified class name
     * @return true if the class exists
     */
    public static boolean classExists(String className)
    {
        try
        {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

}
