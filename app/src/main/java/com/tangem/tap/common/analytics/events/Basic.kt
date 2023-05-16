package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.AnalyticsEvent

/**
[REDACTED_AUTHOR]
 */
sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Basic", event, params, error) {

    class BalanceLoaded(balance: AnalyticsParam.CardBalanceState) : Basic(
        event = "Balance Loaded",
        params = mapOf(
            AnalyticsParam.BALANCE to balance.value,
        ),
    )

    class CardWasScanned(
        source: AnalyticsParam.ScannedFrom,
    ) : Basic(
        event = "Card Was Scanned",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    )

    class SignedIn(
        currency: AnalyticsParam.CardCurrency,
        batch: String,
        signInType: SignInType,
    ) : Basic(
        event = "Signed in",
        params = mapOf(
            AnalyticsParam.CURRENCY to currency.value,
            AnalyticsParam.BATCH to batch,
            "Sign in type" to signInType.name,
        ),
    ) {
        enum class SignInType {
            Card, Biometric
        }
    }

    class ToppedUp(currency: AnalyticsParam.CardCurrency) : Basic(
        event = "Topped up",
        params = mapOf(AnalyticsParam.CURRENCY to currency.value),
    )

    class TransactionSent(sentFrom: AnalyticsParam.TxSentFrom) : Basic(
        event = "Transaction sent",
        params = mapOf(AnalyticsParam.SOURCE to sentFrom.value),
    )

    class ScanError(error: Throwable) : Basic(
        event = "Scan",
        error = error,
    )

    class WalletOpened : Basic(event = "Wallet Opened")
}