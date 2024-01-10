package com.tangem.tap.common.analytics.events

import com.tangem.core.analytics.models.AnalyticsEvent
import com.tangem.core.analytics.models.OneTimeAnalyticsEvent
import com.tangem.domain.wallets.models.UserWalletId

/**
[REDACTED_AUTHOR]
 */
sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Basic", event, params, error) {

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
        hasBackup: Boolean?,
    ) : Basic(
        event = "Signed in",
        params = buildMap {
            put(AnalyticsParam.CURRENCY, currency.value)
            put(AnalyticsParam.BATCH, batch)
            put("Sign in type", signInType.name)
            put("Wallets Count", walletsCount)
            if (hasBackup != null) {
                put("Backuped", if (hasBackup) "Yes" else "No")
            }
        },
    ) {
        enum class SignInType {
            Card, Biometric
        }
    }

    class ToppedUp(userWalletId: UserWalletId, currency: AnalyticsParam.CardCurrency) :
        Basic(
            event = "Topped up",
            params = mapOf(AnalyticsParam.CURRENCY to currency.value),
        ),
        OneTimeAnalyticsEvent {

        override val oneTimeEventId: String = id + userWalletId.stringValue
    }

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