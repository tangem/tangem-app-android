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
        walletsCount: String,
    ) : Basic(
        event = "Signed in",
        params = mapOf(
            AnalyticsParam.CURRENCY to currency.value,
            AnalyticsParam.BATCH to batch,
            "Sign in type" to signInType.name,
            "Wallets Count" to walletsCount,
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

    class TransactionSent(sentFrom: AnalyticsParam.TxSentFrom, memoType: MemoType) : Basic(
        event = "Transaction sent",
        params = buildMap {
            this[AnalyticsParam.SOURCE] = sentFrom.value
            if (sentFrom is AnalyticsParam.TxData) {
                this[AnalyticsParam.BLOCKCHAIN] = sentFrom.blockchain
                this[AnalyticsParam.TOKEN] = sentFrom.token
                this[AnalyticsParam.FEE_TYPE] = sentFrom.feeType.value
            }
            if (sentFrom is AnalyticsParam.TxSentFrom.Approve) {
                this[AnalyticsParam.PERMISSION_TYPE] = sentFrom.permissionType
            }
            this["Memo"] = memoType.name
        },
    ) {
        enum class MemoType {
            Empty, Full, Null
        }
    }

    class ScanError(error: Throwable) : Basic(
        event = "Scan",
        error = error,
    )

    class WalletOpened : Basic(event = "Wallet Opened")
}