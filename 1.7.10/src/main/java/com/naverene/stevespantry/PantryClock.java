package com.naverene.stevespantry;

import java.util.function.LongSupplier;

/**
 * Game time as seen by the client, for rendering the freshness bar. Common code can't touch
 * {@code Minecraft} directly, so the client proxy fills this in; it reads -1 on a dedicated server.
 */
public final class PantryClock {
    public static LongSupplier client = () -> -1L;

    private PantryClock() {}
}
