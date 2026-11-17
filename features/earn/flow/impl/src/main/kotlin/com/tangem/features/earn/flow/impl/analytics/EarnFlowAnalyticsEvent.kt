package com.tangem.features.earn.flow.impl.analytics

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.features.earn.api.EarnEntrySource
import com.tangem.features.earn.product.api.analytics.toAnalyticsValue

private object FlowParam {
    const val CURRENT_APY = "Current APY"
    const val POSITION_BUCKET = "Position Bucket"
    const val LIQUIDITY_CAPPED = "Liquidity Capped"
    const val DISPLAY_CURRENCY = "Display Currency"
    const val SCREEN = "Screen"
    const val PERCENT = "Percent"
    const val FEE_SPEED = "Fee Speed"
    const val TRANSACTION_TYPE = "Transaction Type"
    const val AMOUNT_BUCKET = "Amount Bucket"
    const val PERFORMANCE_FEE = "Performance Fee"
    const val ACTION = "Action"
    const val DESTINATION = "Destination"
}

internal sealed class EarnFlowAnalyticsEvent(
    category: String,
    event: String,
    params: Map<String, String>,
) : AnalyticsEvent(category = category, event = event, params = params) {

    class DepositScreenOpened(
        category: String,
        source: EarnEntrySource,
        currentApy: String?,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Deposit Screen Opened",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source.toAnalyticsValue())
            currentApy?.let { put(FlowParam.CURRENT_APY, it) }
        },
    )

    class WithdrawScreenOpened(
        category: String,
        positionBucket: String?,
        isLiquidityCapped: Boolean?,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Withdraw Screen Opened",
        params = buildMap {
            positionBucket?.let { put(FlowParam.POSITION_BUCKET, it) }
            isLiquidityCapped?.let { put(FlowParam.LIQUIDITY_CAPPED, it.toString()) }
        },
    )

    class HowItWorksOpened(
        category: String,
        source: EarnEntrySource,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "How It Works Sheet Opened",
        params = mapOf(AnalyticsParam.SOURCE to source.toAnalyticsValue()),
    )

    class LearnMore(
        category: String,
        source: EarnEntrySource,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Learn More",
        params = mapOf(AnalyticsParam.SOURCE to source.toAnalyticsValue()),
    )

    class CurrencyToggle(
        category: String,
        currency: EarnDisplayCurrency,
        screen: EarnFlowScreen,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Currency Toggle",
        params = mapOf(
            FlowParam.DISPLAY_CURRENCY to currency.value,
            FlowParam.SCREEN to screen.value,
        ),
    )

    class Max(
        category: String,
        screen: EarnFlowScreen,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Max",
        params = mapOf(FlowParam.SCREEN to screen.value),
    )

    class Percent(
        category: String,
        percent: Int,
        screen: EarnFlowScreen,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Percent",
        params = mapOf(
            FlowParam.PERCENT to percent.toString(),
            FlowParam.SCREEN to screen.value,
        ),
    )

    class FeeSpeed(
        category: String,
        feeType: AnalyticsParam.FeeType,
        screen: EarnFlowScreen,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Fee Speed",
        params = mapOf(
            FlowParam.FEE_SPEED to feeType.value,
            FlowParam.SCREEN to screen.value,
        ),
    )

    class InsufficientBalance(
        category: String,
        screen: EarnFlowScreen,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Notice - Insufficient Balance",
        params = mapOf(FlowParam.SCREEN to screen.value),
    )

    class InsufficientFee(
        category: String,
        screen: EarnFlowScreen,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Notice - Insufficient Fee",
        params = mapOf(FlowParam.SCREEN to screen.value),
    )

    class ConfirmScreenOpened(
        category: String,
        type: EarnTxType,
        amountBucket: String?,
        currentApy: String?,
        performanceFee: String?,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Confirm Screen Opened",
        params = buildMap {
            put(FlowParam.TRANSACTION_TYPE, type.value)
            amountBucket?.let { put(FlowParam.AMOUNT_BUCKET, it) }
            currentApy?.let { put(FlowParam.CURRENT_APY, it) }
            performanceFee?.let { put(FlowParam.PERFORMANCE_FEE, it) }
        },
    )

    class ErrorPopup(
        category: String,
        type: EarnTxType,
        errorCode: String,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Notice - Error Popup",
        params = mapOf(
            FlowParam.TRANSACTION_TYPE to type.value,
            AnalyticsParam.ERROR_CODE to errorCode,
        ),
    )

    class ErrorPopupButton(
        category: String,
        type: EarnTxType,
        action: EarnErrorPopupAction,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Error Popup",
        params = mapOf(
            FlowParam.TRANSACTION_TYPE to type.value,
            FlowParam.ACTION to action.value,
        ),
    )

    class SuccessScreenOpened(
        category: String,
        type: EarnTxType,
        currentApy: String?,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Success Screen Opened",
        params = buildMap {
            put(FlowParam.TRANSACTION_TYPE, type.value)
            currentApy?.let { put(FlowParam.CURRENT_APY, it) }
        },
    )

    class ResultAction(
        category: String,
        destination: EarnResultDestination,
    ) : EarnFlowAnalyticsEvent(
        category = category,
        event = "Button - Result Action",
        params = mapOf(FlowParam.DESTINATION to destination.value),
    )
}