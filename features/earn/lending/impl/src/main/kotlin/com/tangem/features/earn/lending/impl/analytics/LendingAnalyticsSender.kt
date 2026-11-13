package com.tangem.features.earn.lending.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.lending.impl.session.LendingOperation
import com.tangem.features.earn.lending.impl.session.LendingPlan
import javax.inject.Inject

internal class LendingAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase,
) {

    suspend fun onSent(plan: LendingPlan, operation: LendingOperation, currency: CryptoCurrency) {
        val usdRate = currency.id.rawCurrencyId?.let { rawId -> getCurrencyUSDQuoteUseCase(rawId) }
        val amountBucket = usdRate?.let { rate -> LendingAmountBucket.of(plan.amount * rate) }

        analyticsEventHandler.send(
            LendingAnalyticsEvent.TransactionSigned(
                operation = operation,
                isWithUpgrade = plan.isWithUpgrade,
                amountBucket = amountBucket,
            ),
        )
        analyticsEventHandler.send(
            LendingAnalyticsEvent.TransactionSubmitted(operation = operation, isWithUpgrade = plan.isWithUpgrade),
        )
        analyticsEventHandler.send(transactionSent(currency))
    }

    /** A refusal on the card is not a failed transaction */
    fun onSendFailed(plan: LendingPlan, operation: LendingOperation, error: SendTransactionError) {
        if (error is SendTransactionError.UserCancelledError) return

        analyticsEventHandler.send(
            LendingAnalyticsEvent.TransactionFailed(
                operation = operation,
                isWithUpgrade = plan.isWithUpgrade,
                errorCode = error.getAnalyticsDescription(),
            ),
        )
    }

    /** Lending is offered on network coins only, so the fee is paid in the lent currency itself */
    private fun transactionSent(currency: CryptoCurrency) = Basic.TransactionSent(
        sentFrom = AnalyticsParam.TxSentFrom.Earning(
            blockchain = currency.network.name,
            token = currency.symbol,
            feeType = AnalyticsParam.FeeType.Normal,
            feeToken = currency.symbol,
            feeAssetType = AnalyticsParam.FeeAssetType.Coin,
        ),
        memoType = Basic.TransactionSent.MemoType.Null,
        isCustomToken = currency.isCustom,
        isCustomFeeToken = currency.isCustom,
    )
}