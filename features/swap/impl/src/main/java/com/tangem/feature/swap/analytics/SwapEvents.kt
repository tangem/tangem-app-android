package com.tangem.feature.swap.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACCOUNT_DERIVATION_FROM
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACCOUNT_DERIVATION_TO
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_CODE
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_MESSAGE
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.getReferralParams
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.swap.models.PredefinedPercentAmount
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.domain.models.domain.SwapUIMode
import com.tangem.feature.swap.domain.models.ui.FeeBucket

private const val SWAP_CATEGORY = "Swap"
private const val PROMO_CATEGORY = "Promo"

sealed class SwapEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(SWAP_CATEGORY, event, params) {

    class SwapScreenOpened(
        val fromCurrency: CryptoCurrency?,
        val toCurrency: CryptoCurrency?,
    ) : SwapEvents(
        event = "Swap Screen Opened",
        params = mapOf(
            SEND_TOKEN to fromCurrency?.symbol.orEmpty(),
            "Send Blockchain" to fromCurrency?.network?.name.orEmpty(),
            RECEIVE_TOKEN to toCurrency?.symbol.orEmpty(),
            "Receive Blockchain" to toCurrency?.network?.name.orEmpty(),
        ),
    ), AppsFlyerIncludedEvent

    class SwapType(val mode: SwapUIMode) : SwapEvents(
        event = "Swap type simple/detailed",
        params = mapOf("Swap type" to mode.key),
    )

    class SwapTypeSelect(
        val provider: SwapProvider?,
        val sendToken: String,
        val sendBlockchain: String,
        val receiveToken: String?,
        val receiveBlockchain: String?,
    ) : SwapEvents(
        event = "Button - Swap type menu",
        params = buildMap {
            provider?.let { put(PROVIDER, it.name) }
            put(SEND_TOKEN, sendToken)
            put(SEND_BLOCKCHAIN, sendBlockchain)
            receiveToken?.let { put(RECEIVE_TOKEN, it) }
            receiveBlockchain?.let { put(RECEIVE_BLOCKCHAIN, it) }
        },
    )

    class SwapTypeReSelection(
        val typeFrom: SwapUIMode,
        val typeTo: SwapUIMode,
    ) : SwapEvents(
        event = "Swap type re-selection",
        params = mapOf(
            "Type from" to typeFrom.key,
            "Type to" to typeTo.key,
        ),
    )

    class SendTokenBalanceClicked : SwapEvents(event = "Send Token Balance Clicked")

    class ChooseTokenScreenResult(
        val isTokenChosen: Boolean,
        val token: String? = null,
    ) : SwapEvents(
        event = "Choose Token Screen Result",
        params = buildMap {
            put("Token Chosen", if (isTokenChosen) "Yes" else "No")
            token?.let { put("Token", it) }
        },
    )

    class ChoosePopularToken(
        val direction: String,
        val currency: CryptoCurrency,
    ) : SwapEvents(
        event = "Choose popular token",
        params = mapOf(
            "Direction" to direction,
            "Token" to currency.symbol,
            "Blockchain" to currency.network.name,
        ),
    )

    class PreselectedTokenChanged(
        val direction: String,
        val preSelectedToken: CryptoCurrency,
        val selectedToken: CryptoCurrency,
    ) : SwapEvents(
        event = "Pre-selected token changed",
        params = mapOf(
            "Direction" to direction,
            "Pre-selected Token" to preSelectedToken.symbol,
            "Selected Token" to selectedToken.symbol,
        ),
    )

    class ButtonSwapClicked(
        val sendToken: String,
        val receiveToken: String,
        val swapUIMode: SwapUIMode,
    ) : SwapEvents(
        event = "Button - Swap",
        params = mapOf(
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Swap type" to swapUIMode.key,
        ),
    )

    class ButtonGivePermissionClicked(
        val sendToken: String,
        val receiveToken: String,
        val provider: SwapProvider,
    ) : SwapEvents(
        event = "Button - Give permission",
        params = mapOf(
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Provider" to provider.name,
        ),
    )

    class ButtonSwipeClicked : SwapEvents(event = "Button - Swipe")

    @Suppress("NullableToStringCall", "LongParameterList")
    class SwapInProgressScreen(
        val provider: SwapProvider,
        val commission: FeeBucket, // SLOW / MARKET / FAST / SUGGESTED / CUSTOM
        val sendBlockchain: String,
        val receiveBlockchain: String,
        val sendToken: String,
        val receiveToken: String,
        val feeToken: String,
        val feeAssetType: AnalyticsParam.FeeAssetType,
        val fromDerivationIndex: Int?,
        val toDerivationIndex: Int?,
        val referralId: String?,
    ) : SwapEvents(
        event = "Swap in Progress Screen Opened",
        params = buildMap {
            put("Provider", provider.name)
            put("Commission", if (commission == FeeBucket.MARKET) "Market" else "Fast")
            put("Send Token", sendToken)
            put("Receive Token", receiveToken)
            put("Send Blockchain", sendBlockchain)
            put("Receive Blockchain", receiveBlockchain)
            if (fromDerivationIndex != null) put(ACCOUNT_DERIVATION_FROM, fromDerivationIndex.toString())
            if (toDerivationIndex != null) put(ACCOUNT_DERIVATION_TO, toDerivationIndex.toString())
            put(FEE_TOKEN, feeToken)
            put(AnalyticsParam.Key.FEE_ASSET_TYPE, feeAssetType.value)
            putAll(getReferralParams(referralId))
        },
    ), AppsFlyerIncludedEvent

    class ProviderClicked : SwapEvents("Provider Clicked")

    class ProviderChosen(val provider: SwapProvider) : SwapEvents(
        event = "Provider Chosen",
        params = mapOf("Provider" to provider.name),
    )

    class ButtonStatus(val token: String) : SwapEvents(
        event = "Button - Status",
        params = mapOf("Token" to token),
    )

    class ButtonExplore(val token: String) : SwapEvents(
        event = "Button - Explore",
        params = mapOf("Token" to token),
    )

    class NoticeNoAvailableTokensToSwap : SwapEvents("Notice - No Available Tokens To Swap")

    class NoticeUnavailableToSwapPair(
        val sendToken: String,
        val receiveToken: String,
        val sendBlockchain: String,
        val receiveBlockchain: String,
    ) : SwapEvents(
        event = "Notice - Unavailable To Swap Pair",
        params = mapOf(
            SEND_TOKEN to sendToken,
            RECEIVE_TOKEN to receiveToken,
            "Send Blockchain" to sendBlockchain,
            "Receive Blockchain" to receiveBlockchain,
        ),
    )

    class NoticeNotEnoughFee(val token: String, val blockchain: String) : SwapEvents(
        event = "Notice - Not Enough Fee",
        params = mapOf(
            "Token" to token,
            "Blockchain" to blockchain,
        ),
    )

    class HighPriceImpact(
        val sendToken: String,
        val receiveToken: String,
        val sendBlockchain: String,
        val receiveBlockchain: String,
        val providerName: String,
    ) : SwapEvents(
        event = "Notice - High price impact",
        params = mapOf(
            SEND_TOKEN to sendToken,
            RECEIVE_TOKEN to receiveToken,
            "Send Blockchain" to sendBlockchain,
            "Receive Blockchain" to receiveBlockchain,
            PROVIDER to providerName,
        ),
    )

    class TradeTooLarge(
        val sendToken: String,
        val receiveToken: String,
        val sendBlockchain: String,
        val receiveBlockchain: String,
        val providerName: String,
    ) : SwapEvents(
        event = "Notice - Trade too large",
        params = mapOf(
            SEND_TOKEN to sendToken,
            RECEIVE_TOKEN to receiveToken,
            "Send Blockchain" to sendBlockchain,
            "Receive Blockchain" to receiveBlockchain,
            PROVIDER to providerName,
        ),
    )

    class NoticeProviderError(
        val sendToken: String,
        val receiveToken: String,
        val provider: SwapProvider,
        val errorCode: Int,
        val errorMessage: String?,
    ) : SwapEvents(
        event = "Notice - Express Error",
        params = buildMap {
            put(SEND_TOKEN, sendToken)
            put(RECEIVE_TOKEN, receiveToken)
            put(PROVIDER, provider.name)
            put(ERROR_CODE, errorCode.toString())
            errorMessage?.let { put(ERROR_MESSAGE, it) }
        },
    )
    // TODO parameters

    // region Promo activity
    class ChangellyActivity(
        val promoState: PromoState,
    ) : AnalyticsEvent(
        category = PROMO_CATEGORY,
        event = "Changelly Activity",
        params = mapOf(
            "State" to promoState.name,
        ),
    ) {
        sealed class PromoState(val name: String) {
            data object Native : PromoState("Native")
            data object Recommended : PromoState("Recommended")
        }
    }

    class NoticePermissionNeeded(
        val sendToken: String,
        val receiveToken: String,
        val provider: SwapProvider,
    ) : SwapEvents(
        event = "Notice - Permission Needed",
        params = mapOf(
            "Send Token" to sendToken,
            "Receive Token" to receiveToken,
            "Provider" to provider.name,
        ),
    )

    class FastAmountInput(percent: PredefinedPercentAmount) : SwapEvents(
        event = "Fast amount input",
        params = mapOf("Percentage" to percent.toAnalyticsValue()),
    )

    class TransferModeSwitched(
        fromCurrency: CryptoCurrency?,
        toCurrency: CryptoCurrency?,
    ) : SwapEvents(
        event = "Transfer Mode Switched",
        params = mapOf(
            SEND_TOKEN to fromCurrency?.symbol.orEmpty(),
            "Send Blockchain" to fromCurrency?.network?.name.orEmpty(),
            RECEIVE_TOKEN to toCurrency?.symbol.orEmpty(),
            "Receive Blockchain" to toCurrency?.network?.name.orEmpty(),
        ),
    )

    class ButtonTransferClicked(
        fromCurrency: CryptoCurrency?,
        toCurrency: CryptoCurrency?,
    ) : SwapEvents(
        event = "Button - Transfer",
        params = mapOf(
            SEND_TOKEN to fromCurrency?.symbol.orEmpty(),
            "Send Blockchain" to fromCurrency?.network?.name.orEmpty(),
            RECEIVE_TOKEN to toCurrency?.symbol.orEmpty(),
            "Receive Blockchain" to toCurrency?.network?.name.orEmpty(),
        ),
    )

    @Suppress("NullableToStringCall", "LongParameterList")
    class TransferInProgressScreen(
        fromCurrency: CryptoCurrency?,
        toCurrency: CryptoCurrency?,
        feeNetwork: Network,
    ) : SwapEvents(
        event = "Transfer in Progress Screen Opened",
        params = mapOf(
            SEND_TOKEN to fromCurrency?.symbol.orEmpty(),
            "Send Blockchain" to fromCurrency?.network?.name.orEmpty(),
            RECEIVE_TOKEN to toCurrency?.symbol.orEmpty(),
            "Receive Blockchain" to toCurrency?.network?.name.orEmpty(),
            "Network fee" to feeNetwork.name,
        ),
    ), AppsFlyerIncludedEvent

    class ApproveGasOverrideError(
        fromTokenSymbol: String,
        fromTokenBlockchain: String,
        rpcProvider: String,
        error: String,
    ) : SwapEvents(
        event = "Gas Estimation Override Error",
        params = mapOf(
            TOKEN_PARAM to fromTokenSymbol,
            BLOCKCHAIN to fromTokenBlockchain,
            "RPC Provider" to rpcProvider,
            ERROR_MESSAGE to error,
        ),
    )
}

private fun PredefinedPercentAmount.toAnalyticsValue(): String = when (this) {
    PredefinedPercentAmount.PERCENT_25 -> "25"
    PredefinedPercentAmount.PERCENT_50 -> "50"
    PredefinedPercentAmount.PERCENT_75 -> "75"
    PredefinedPercentAmount.MAX -> "Max"
}