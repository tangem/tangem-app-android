package com.tangem.tap.common.analytics.events

import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.common.extensions.filterNotNull

/**
 * Created by Anton Zhilenkov on 28.09.2022.
 */
sealed class ManageTokens(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Manage Tokens", event, params) {

    class ScreenOpened : ManageTokens("Manage Tokens Screen Opened")
    class TokenSearched : ManageTokens("Token Searched")

    class TokenSwitcherChanged(
        type: AnalyticsParam.CurrencyType,
        state: AnalyticsParam.OnOffState,
    ) : ManageTokens(
        "Token Switcher Changed",
        params = mapOf(
            "Token" to type.value,
            "State" to state.value,
        ),
    )

    class ButtonSaveChanges : ManageTokens("Button - Save Changes")
    class ButtonCustomToken : ManageTokens("Button - Custom Token")

    sealed class CustomToken(
        event: String,
        params: Map<String, String> = mapOf(),
    ) : ManageTokens(event, params) {

        class ScreenOpened : ManageTokens("Custom Token Screen Opened")

        class TokenWasAdded(customCurrency: CustomCurrency) : ManageTokens(
            event = "Custom Token Was Added",
            params = convertToParam(customCurrency),
        ) {
            companion object {
                private fun convertToParam(customCurrency: CustomCurrency): Map<String, String> = with(customCurrency) {
                    return when (this) {
                        is CustomCurrency.CustomBlockchain -> mapOf(
                            "Token" to network.currency,
                            "Derivation Path" to derivationPath?.rawPath,
                        ).filterNotNull()
                        is CustomCurrency.CustomToken -> mapOf(
                            "Token" to token.symbol,
                            "Derivation Path" to derivationPath?.rawPath,
                            "Network Id" to network.toNetworkId(),
                            "Contract Address" to token.contractAddress,
                        ).filterNotNull()
                    }
                }
            }
        }
    }
}
