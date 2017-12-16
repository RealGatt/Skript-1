
package ch.njol.skript.timings;

import co.aikar.timings.lib.MCTiming;
import co.aikar.timings.lib.TimingManager;
import org.eclipse.jdt.annotation.Nullable;

import ch.njol.skript.Skript;

/**
 * Static utils for Skript timings.
 */
public class SkriptTimings {

    private static volatile boolean enabled;
    @SuppressWarnings("null")
    private static Skript skript; // Initialized on Skript load, before any timings would be used anyway

    @Nullable
    public static Object start(String name) {
        if (!enabled()) // Timings disabled :(
            return null;

        MCTiming timing = TimingManager.of(skript).of("Skript");
        timing.startTiming(); // No warning spam in async code
        return timing;
    }

    public static void stop(@Nullable Object timing) {
        if (timing == null) // Timings disabled...
            return;
        ((MCTiming) timing).stopTiming();
    }

    public static boolean enabled() {
        // First check if we can run timings (enabled in settings + running Paper)
        // After that (we know that class exists), check if server has timings running
        return enabled;
    }

    public static void setEnabled(boolean flag) {
        enabled = flag;
    }

    public static void setSkript(Skript plugin) {
        skript = plugin;
    }

}