package com.tangem.tap.common.analytics.events

/**
* [REDACTED_AUTHOR]
 */
sealed class ManageTokens(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Manage Tokens", event, params) {

    class ScreenOpened : ManageTokens("Manage Tokens Screen Opened")
    class TokenSearched : ManageTokens("Token Searched")
    class TokenSwitcherChanged : ManageTokens("Token Switcher Changed")
    class ButtonSaveChanges : ManageTokens("Button - Save Changes")
    class ButtonCustomToken : ManageTokens("Button - Custom Token")

    sealed class CustomToken(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : ManageTokens(event, params) {

        class ScreenOpened : ManageTokens("Custom Token Screen Opened")

        class TokenWasAdded(symbol: String, network: String, address: String) : ManageTokens(
            event = "Custom Token Was Added",
            params = mapOf(
                "Symbol" to symbol,
                "Network" to network,
                "Address" to address,
            ),
        )
    }
}
