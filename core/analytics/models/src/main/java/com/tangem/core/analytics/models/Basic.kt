package com.tangem.core.analytics.models

sealed class Basic(
    event: String,
    params: Map<String, EventValue> = mapOf(),
    error: Throwable? = null,
) : AnalyticsEvent("Basic", event, params, error) {

    class CardWasScanned(
        source: AnalyticsParam.ScreensSources,
    ) : Basic(
        event = "Card Was Scanned",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
        ),
    )

    class SignedIn(
        currency: AnalyticsParam.WalletType,
        batch: String,
        signInType: SignInType,
        walletsCount: String,
        hasBackup: Boolean?,
    ) : Basic(
        event = "Signed in",
        params = buildMap {
            put(AnalyticsParam.CURRENCY, currency.value.asStringValue())
            put(AnalyticsParam.BATCH, batch.asStringValue())
            put("Sign in type", signInType.name.asStringValue())
            put("Wallets Count", walletsCount.asStringValue())
            if (hasBackup != null) {
                put("Backuped", if (hasBackup) "Yes".asStringValue() else "No".asStringValue())
            }
        },
    ) {
        enum class SignInType {
            Card, Biometric
        }
    }

    class ToppedUp(userWalletId: String, currency: AnalyticsParam.WalletType) :
        Basic(
            event = "Topped up",
            params = mapOf(AnalyticsParam.CURRENCY to currency.value.asStringValue()),
        ),
        OneTimeAnalyticsEvent {

        override val oneTimeEventId: String = id + userWalletId
    }

    class TransactionSent(sentFrom: AnalyticsParam.TxSentFrom, memoType: MemoType) :
        Basic(
            event = "Transaction sent",
            params = buildMap {
                this[AnalyticsParam.SOURCE] = sentFrom.value.asStringValue()
                if (sentFrom is AnalyticsParam.TxData) {
                    this[AnalyticsParam.BLOCKCHAIN] = sentFrom.blockchain.asStringValue()
                    this[AnalyticsParam.TOKEN_PARAM] = sentFrom.token.asStringValue()
                    this[AnalyticsParam.FEE_TYPE] = sentFrom.feeType.value.asStringValue()
                }
                if (sentFrom is AnalyticsParam.TxSentFrom.Approve) {
                    this[AnalyticsParam.PERMISSION_TYPE] = sentFrom.permissionType.asStringValue()
                }
                this["Memo"] = memoType.name.asStringValue()
            },
        ) {
        enum class MemoType {
            Empty, Full, Null
        }

        enum class WalletForm {
            Card, Ring
        }
    }

    class ScanError(error: Throwable) : Basic(
        event = "Scan",
        error = error,
    )

    class ButtonSupport(source: AnalyticsParam.ScreensSources) : Basic(
        event = "Request Support",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value.asStringValue(),
        ),
    )
}