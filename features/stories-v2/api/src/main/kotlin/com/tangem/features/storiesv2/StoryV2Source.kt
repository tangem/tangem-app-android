package com.tangem.features.storiesv2

/**
 * Entry point the story was opened from. Reported as the `Source` property of every story analytics event, so the
 * same composition opened from two places stays distinguishable in the funnel.
 */
enum class StoryV2Source(val analyticsValue: String) {
    MAIN(analyticsValue = "Main"),
    WALLET_SETTINGS(analyticsValue = "Wallet Settings"),
    TOKEN(analyticsValue = "Token"),
    MARKETS(analyticsValue = "Markets"),
    TANGEM_PAY(analyticsValue = "Tangem Pay"),
    LONG_TAP(analyticsValue = "Long Tap"),
    ONBOARDING(analyticsValue = "Onboarding"),

    /** Tester-only. Keeps storybook playbacks out of the product funnels. */
    STORYBOOK(analyticsValue = "Storybook"),
}