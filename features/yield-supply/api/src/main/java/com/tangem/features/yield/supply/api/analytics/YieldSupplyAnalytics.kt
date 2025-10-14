package com.tangem.features.yield.supply.api.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam.Key.ACTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.BLOCKCHAIN
import com.tangem.core.analytics.models.AnalyticsParam.Key.ERROR_DESCRIPTION
import com.tangem.core.analytics.models.AnalyticsParam.Key.STATUS
import com.tangem.core.analytics.models.AnalyticsParam.Key.TOKEN_PARAM

sealed class YieldSupplyAnalytics(
    event: String,
    params: Map<String, String> = mapOf(),
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
    )

    data class StartEarningScreen(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Start Earning Screen",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

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

    data object ButtonFeePolicy : YieldSupplyAnalytics(
        event = "Button - Fee Policy",
    )

    data object EarnInProgressScreen : YieldSupplyAnalytics(
        event = "Earn In Progress Screen",
    )

    data object FundsEarned : YieldSupplyAnalytics(
        event = "Funds Earned",
    )

    data class FundsWithdrawn(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Funds Withdrawn",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

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
    ) : YieldSupplyAnalytics(
        event = "Notice - Not Enough Fee",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
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
        params = buildMap {
            ACTION to action
            ERROR_DESCRIPTION to errorDescription.orEmpty()
        },
    )

    data class AssetStatusLoaded(
        val status: AssetStatus,
    ) : YieldSupplyAnalytics(
        event = "Asset Status Loaded",
        params = mapOf(
            STATUS to status.value,
        ),
    )

    data class ActionBlockedByStatus(
        val status: AssetStatus,
    ) : YieldSupplyAnalytics(
        event = "Action Blocked by Status",
        params = mapOf(
            STATUS to status.value,
        ),
    )

    data object ApyChartViewed : YieldSupplyAnalytics(
        event = "APY Chart",
    )

    data class NoticeCommissionTooHigh(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Notice - Commission Is Too High",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    data class NoticeNotEnoughMinAmount(
        val token: String,
        val blockchain: String,
    ) : YieldSupplyAnalytics(
        event = "Notice - Not Enough Min Amount",
        params = mapOf(
            TOKEN_PARAM to token,
            BLOCKCHAIN to blockchain,
        ),
    )

    enum class Action(val value: String) {
        Approve("Approve"),
        Stop("Stop"),
    }

    enum class AssetStatus(val value: String) {
        Active("ACTIVE"),
        Inactive("Inactive"),
    }
}