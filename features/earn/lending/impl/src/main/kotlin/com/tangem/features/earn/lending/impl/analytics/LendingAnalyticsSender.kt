package com.tangem.features.earn.lending.impl.analytics

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.quotes.GetCurrencyUSDQuoteUseCase
import com.tangem.domain.transaction.error.SendTransactionError
import com.tangem.features.earn.lending.impl.session.LendingPlan
import com.tangem.features.earn.lending.impl.session.LendingPlannedOperation
import javax.inject.Inject

internal class LendingAnalyticsSender @Inject constructor(
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val getCurrencyUSDQuoteUseCase: GetCurrencyUSDQuoteUseCase,
) {

    /** The amount bucket is left out when the USD price of [currency] is unknown */
    suspend fun onSent(plan: LendingPlan, currency: CryptoCurrency) {
        val transaction = plan.toAnalyticsTransaction()
        val usdRate = currency.id.rawCurrencyId?.let { rawId -> getCurrencyUSDQuoteUseCase(rawId) }
        val amountBucket = usdRate?.let { rate -> LendingAmountBucket.of(plan.amount * rate) }

        analyticsEventHandler.send(LendingAnalyticsEvent.TransactionSigned(transaction, amountBucket))
        analyticsEventHandler.send(LendingAnalyticsEvent.TransactionSubmitted(transaction))
    }

    /** A refusal on the card is not a failed transaction */
    fun onSendFailed(plan: LendingPlan, error: SendTransactionError) {
        if (error is SendTransactionError.UserCancelledError) return

        analyticsEventHandler.send(
            LendingAnalyticsEvent.TransactionFailed(
                transaction = plan.toAnalyticsTransaction(),
                errorCode = error.getAnalyticsDescription(),
            ),
        )
    }

    private fun LendingPlan.toAnalyticsTransaction() = LendingAnalyticsTransaction(
        type = when (operation) {
            LendingPlannedOperation.Deposit -> LendingAnalyticsTransaction.Type.Deposit
            is LendingPlannedOperation.Withdraw -> LendingAnalyticsTransaction.Type.Withdraw
        },
        isWithUpgrade = isWithUpgrade,
    )
}