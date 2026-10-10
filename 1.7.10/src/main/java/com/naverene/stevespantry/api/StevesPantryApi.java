package com.naverene.stevespantry.api;

/**
 * Entry point to the Steve's Pantry API.
 *
 * <pre>{@code
 * if (StevesPantryApi.isAvailable()) {
 *     IPantryApi pantry = StevesPantryApi.get();
 *     pantry.registerCoolant(MyItems.FREEZER_PACK, 48000);
 * }
 * }</pre>
 *
 * Check {@link #isAvailable()} first when Steve's Pantry is an optional dependency.
 */
public final class StevesPantryApi {
    /** Bumped when the API changes in a way addons may need to check for. */
    public static final int VERSION = 1;

    private static final String IMPL = "com.naverene.stevespantry.PantryApiImpl";
    private static IPantryApi instance;
    private static boolean looked;

    private StevesPantryApi() {}

    /** True when Steve's Pantry is installed. Safe to call at any point, including mod construction. */
    public static boolean isAvailable() {
        return lookup() != null;
    }

    /** The API. Throws if Steve's Pantry isn't installed; check {@link #isAvailable()} for a soft dependency. */
    public static IPantryApi get() {
        IPantryApi api = lookup();
        if (api == null) {
            throw new IllegalStateException("Steve's Pantry is not installed");
        }
        return api;
    }

    // Found by name, so the API works however mods are ordered and the api jar compiles on its own.
    private static synchronized IPantryApi lookup() {
        if (!looked) {
            looked = true;
            try {
                instance = (IPantryApi) Class.forName(IMPL).getField("INSTANCE").get(null);
            } catch (ReflectiveOperationException | LinkageError e) {
                instance = null;
            }
        }
        return instance;
    }
}
