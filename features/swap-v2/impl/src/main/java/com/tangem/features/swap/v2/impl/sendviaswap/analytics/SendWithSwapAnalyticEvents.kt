package com.tangem.features.swap.v2.impl.sendviaswap.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACCOUNT_DERIVATION_FROM
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACCOUNT_DERIVATION_TO
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_DESCRIPTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.FEE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.PROVIDER
import com.tangem.core.analytics.models.AnalyticsParam.Key.RATE_TYPE
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.RECEIVE_TOKEN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.SEND_TOKEN
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.features.send.v2.api.analytics.CommonSendAnalyticEvents

internal sealed class SendWithSwapAnalyticEvents(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = CommonSendAnalyticEvents.SEND_CATEGORY, event = event, params = params) {

    /** Confirmation screen opened */
    data class ConfirmationScreenOpened(
        val providerName: String,
        val rateType: RateType,
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
    ) : SendWithSwapAnalyticEvents(
        event = "Send With Swap Confirm Screen Opened",
        params = buildMap {
            put(SEND_TOKEN, fromToken.symbol)
            put(RECEIVE_TOKEN, toToken.symbol)
            put(SEND_BLOCKCHAIN, fromToken.network.name)
            put(RECEIVE_BLOCKCHAIN, toToken.network.name)
            put(RATE_TYPE, rateType.name)
            put(PROVIDER, providerName)
        },
    ), AppsFlyerIncludedEvent

    /** Amount screen opened */
    data class AmountScreenOpened(
        val rateType: RateType,
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
    ) : SendWithSwapAnalyticEvents(
        event = "Send With Swap Amount Screen Opened",
        params = buildMap {
            put(SEND_TOKEN, fromToken.symbol)
            put(RECEIVE_TOKEN, toToken.symbol)
            put(SEND_BLOCKCHAIN, fromToken.network.name)
            put(RECEIVE_BLOCKCHAIN, toToken.network.name)
            put(RATE_TYPE, rateType.name)
        },
    ), AppsFlyerIncludedEvent

    data class TransactionScreenOpened(
        val providerName: String,
        val feeType: AnalyticsParam.FeeType,
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
        val fromDerivationIndex: Int?,
        val toDerivationIndex: Int?,
    ) : SendWithSwapAnalyticEvents(
        event = "Send With Swap In Progress Screen Opened",
        params = buildMap {
            put(PROVIDER, providerName)
            put(FEE_TYPE, if (feeType is AnalyticsParam.FeeType.Normal) "Market" else "Fast")
            put(SEND_TOKEN, fromToken.symbol)
            put(RECEIVE_TOKEN, toToken.symbol)
            put(SEND_BLOCKCHAIN, fromToken.network.name)
            put(RECEIVE_BLOCKCHAIN, toToken.network.name)
            if (fromDerivationIndex != null) put(ACCOUNT_DERIVATION_FROM, fromDerivationIndex.toString())
            if (toDerivationIndex != null) put(ACCOUNT_DERIVATION_TO, toDerivationIndex.toString())
        },
    ), AppsFlyerIncludedEvent

    data class OnSendClick(
        val providerName: String,
        val feeType: AnalyticsParam.FeeType,
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
        val fromDerivationIndex: Int?,
        val toDerivationIndex: Int?,
    ) : SendWithSwapAnalyticEvents(
        event = "Button - Send with Swap",
        params = buildMap {
            put(PROVIDER, providerName)
            put(FEE_TYPE, if (feeType is AnalyticsParam.FeeType.Normal) "Market" else "Fast")
            put(SEND_TOKEN, fromToken.symbol)
            put(RECEIVE_TOKEN, toToken.symbol)
            put(SEND_BLOCKCHAIN, fromToken.network.name)
            put(RECEIVE_BLOCKCHAIN, toToken.network.name)
            if (fromDerivationIndex != null) put(ACCOUNT_DERIVATION_FROM, fromDerivationIndex.toString())
            if (toDerivationIndex != null) put(ACCOUNT_DERIVATION_TO, toDerivationIndex.toString())
        },
    ), AppsFlyerIncludedEvent

    data class NoticeCanNotSwapToken(
        val fromToken: CryptoCurrency,
        val toTokenSymbol: String,
    ) : SendWithSwapAnalyticEvents(
        event = "Notice - Can`t Swap This Token",
        params = mapOf(
            SEND_TOKEN to fromToken.symbol,
            RECEIVE_TOKEN to toTokenSymbol,
            SEND_BLOCKCHAIN to fromToken.network.name,
        ),
    )

    data object NoticeFixedRate : SendWithSwapAnalyticEvents(
        event = "Notice - Fixed Rate",
        params = emptyMap(),
    )

    data object NoticeFloatRate : SendWithSwapAnalyticEvents(
        event = "Notice - Float Rate",
        params = emptyMap(),
    )

    data class ErrorInsufficientBalance(
        val fromToken: CryptoCurrency,
    ) : SendWithSwapAnalyticEvents(
        event = "Error - Insufficient balance",
        params = mapOf(
            SEND_TOKEN to fromToken.symbol,
            SEND_BLOCKCHAIN to fromToken.network.name,
        ),
    )

    data class ErrorMinAmount(
        val fromToken: CryptoCurrency,
    ) : SendWithSwapAnalyticEvents(
        event = "Error - Min amount",
        params = mapOf(
            SEND_TOKEN to fromToken.symbol,
            SEND_BLOCKCHAIN to fromToken.network.name,
        ),
    )

    data class ErrorMaxAmount(
        val fromToken: CryptoCurrency,
    ) : SendWithSwapAnalyticEvents(
        event = "Error - Max amount",
        params = mapOf(
            SEND_TOKEN to fromToken.symbol,
            SEND_BLOCKCHAIN to fromToken.network.name,
        ),
    )

    data class ErrorExpressQuote(
        val fromToken: CryptoCurrency,
        val toToken: CryptoCurrency,
        val errorDescription: String? = null,
    ) : SendWithSwapAnalyticEvents(
        event = "Error - Express quote",
        params = buildMap {
            put(SEND_TOKEN, fromToken.symbol)
            put(SEND_BLOCKCHAIN, fromToken.network.name)
            put(RECEIVE_TOKEN, toToken.symbol)
            put(RECEIVE_BLOCKCHAIN, toToken.network.name)
            if (errorDescription != null) put(ERROR_DESCRIPTION, errorDescription)
        },
    )

    class HighPriceImpact(
        val sendToken: String,
        val receiveToken: String,
        val sendBlockchain: String,
        val receiveBlockchain: String,
        val providerName: String,
    ) : SendWithSwapAnalyticEvents(
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

    ) : SendWithSwapAnalyticEvents(
        event = "Notice - Trade too large",
        params = mapOf(
            SEND_TOKEN to sendToken,
            RECEIVE_TOKEN to receiveToken,
            "Send Blockchain" to sendBlockchain,
            "Receive Blockchain" to receiveBlockchain,
            PROVIDER to providerName,
        ),
    )

    enum class RateType {
        Float,
        Fixed,
    }

    fun ExpressRateType.toAnalyticsRateType(): RateType = when (this) {
        ExpressRateType.Float -> RateType.Float
        ExpressRateType.Fixed -> RateType.Fixed
    }
}