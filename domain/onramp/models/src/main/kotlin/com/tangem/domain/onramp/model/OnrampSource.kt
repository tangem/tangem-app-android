package com.tangem.domain.onramp.model

// TODO: Move to features/onramp/api package after removing old buy logic. [REDACTED_JIRA]
enum class OnrampSource(val analyticsName: String) {
    ACTION_BUTTONS("Main"),
    TOKEN_LONG_TAP("Long Tap"),
    TOKEN_DETAILS("Token"),
    MARKETS("Markets"),
    SEPA_BANNER("SEPA Banner"),
}