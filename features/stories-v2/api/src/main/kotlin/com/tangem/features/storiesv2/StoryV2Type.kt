package com.tangem.features.storiesv2

/**
 * Which story the player is asked to show — the composition, not the entry point, which travels separately in
 * [StoryV2Source]. Hence a single upgrade value: hardware upgrade from Main, from Wallet Settings and hot-to-cold
 * are one composition on three placements.
 */
enum class StoryV2Type {

    /** Bundled story shown inside the wallet creation flow. Works offline, loops until the viewer acts. */
    ONBOARDING,

    /** Selling story shown before the Hardware wallet screen. Its composition is delivered remotely. */
    HARDWARE_UPGRADE,
}