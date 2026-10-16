package com.tangem.features.yield.supply.api.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.APY
import com.tangem.core.analytics.models.AnalyticsParam.Key.BALANCE
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.CLOSE_METHOD
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_DESCRIPTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKENS_COUNT
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM
import com.tangem.core.analytics.models.AppsFlyerIncludedEvent
import com.tangem.core.analytics.models.getReferralParams
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoOffer

sealed class YieldSupplyAnalytics(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Earning", event = event, params = params) {

    data class EarningScreenInfoOpened(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Earning Screen Info Opened",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    ), AppsFlyerIncludedEvent

    data class StartEarningScreen(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Start Earning Screen",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    ), AppsFlyerIncludedEvent

    data class StopEarningScreen(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Stop Earning Screen",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    ), AppsFlyerIncludedEvent

    data class ButtonStartEarning(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Button - Start Earning",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class ButtonStopEarning(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Button - Stop Earning",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class ButtonGiveApprove(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = " Button - Give Approve",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class ButtonFeePolicy(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Button - Fee Policy",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class EarnInProgressScreen(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Earn In Progress Screen",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    ), AppsFlyerIncludedEvent

    data class FundsEarned(
        val token: String,
        val blockchain: String,
        val referralId: String?,
    ) : YieldSupplyAnalytics(
        event = "Funds Earned",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
            *getReferralParams(referralId).toTypedArray(),
        ),
    ), AppsFlyerIncludedEvent

    data class FundsWithdrawn(
        val token: String,
        val blockchain: String,
        val referralId: String?,
    ) : YieldSupplyAnalytics(
        event = "Funds Withdrawn",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
            *getReferralParams(referralId).toTypedArray(),
        ),
    ), AppsFlyerIncludedEvent

    data class EarnedFundsInfo(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Earned Funds Info",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class NoticeNotEnoughFee(
        val token: String,
        val blockchain: String,
        val balance: AnalyticsParam.TokenBalanceState,
    ) : YieldSupplyAnalytics(
        event = "Notice - Not Enough Fee",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
            BALANCE to balance.value,
        ),
    )

    data class NoticeApproveNeeded(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Notice - Approve Needed",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class ApprovalAction(
        val token: String,
        val blockchain: String,
        val action: Action,
    ) : YieldSupplyAnalytics(
        event = "Approval Action",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
            ACTION to action.value,
        ),
    )

    data class NoticeHighNetworkFee(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Notice - High Network Fee",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class EarnErrors(
        val action: Action,
        val errorDescription: String?,
    ) : YieldSupplyAnalytics(
        event = "Earn Errors",
        params = mapOf(
            ACTION to action.value,
            ERROR_DESCRIPTION to errorDescription.orEmpty(),
        ),
    )

    class ApyChartViewed : YieldSupplyAnalytics(
        event = "APY Chart",
    )

    data class NoticeAmountNotDeposited(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Notice - Amount Not Deposited",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class NoticeHighFee(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Notice - High Network Fee",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    enum class Action(val value: String) {
        Start("Start"),
        Approve("Approve"),
        Stop("Stop"),
    }

    data class PromoScreenOpened(val offer: YieldModePromoOffer) : YieldSupplyAnalytics(
        event = "Promo Screen Opened",
        params = offer.toParams() + (TOKENS_COUNT to offer.tokensCount.toString()),
    )

    data class PromoScreenClosed(
        val offer: YieldModePromoOffer,
        val closeMethod: CloseMethod,
    ) : YieldSupplyAnalytics(
        event = "Promo Screen Closed",
        params = offer.toParams() + (CLOSE_METHOD to closeMethod.value),
    )

    data class ButtonTryYieldMode(val offer: YieldModePromoOffer) : YieldSupplyAnalytics(
        event = "Button - Try Yield Mode",
        params = offer.toParams(),
    )

    enum class CloseMethod(val value: String) {
        Cross("X"),
        Swipe("Swipe"),
        SystemBack("System Back"),
    }
}

private fun YieldModePromoOffer.toParams(): Map<String, String> = mapOf(
    TOKEN_PARAM to tokenSymbol,
    BLOCKCHAIN to blockchain,
    APY to apy.toPlainString(),
)