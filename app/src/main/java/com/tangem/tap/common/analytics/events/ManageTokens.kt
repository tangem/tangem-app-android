package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.common.extensions.filterNotNull

/**
[REDACTED_AUTHOR]
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

        object ScreenOpened : ManageTokens(event = "Custom Token Screen Opened")

        // TODO("Get rid of strong binding (CustomCurrency)
        open class TokenWasAdded(customCurrency: CustomCurrency) : ManageTokens(
            event = "Custom Token Was Added",
            params = convertToParam(customCurrency),
        ) {

            data class Token(
                val symbol: String,
                val derivationPath: String?,
                val blockchain: com.tangem.blockchain.common.Blockchain,
                val contractAddress: String,
            ) : ManageTokens(
                event = "Custom Token Was Added",
                params = mapOf(
                    "Token" to symbol,
                    "Derivation Path" to derivationPath,
                    "Network Id" to blockchain.currency,
                    "Contract Address" to contractAddress,
                ).filterNotNull(),
            )

            data class Blockchain(
                val derivationPath: String?,
                val blockchain: com.tangem.blockchain.common.Blockchain,
            ) : ManageTokens(
                event = "Custom Token Was Added",
                params = mapOf(
                    "Token" to blockchain.currency,
                    "Derivation Path" to derivationPath,
                ).filterNotNull(),
            )

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