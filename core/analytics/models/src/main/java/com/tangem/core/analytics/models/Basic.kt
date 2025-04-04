package com.tangem.core.analytics.models

sealed class Basic(
    event: String,
    params: Map<String, String> = mapOf(),
) : AnalyticsEvent("Basic", event, params) {

    class CardWasScanned(
        source: AnalyticsParam.ScreensSources,
    ) : Basic(
        event = "Card Was Scanned",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
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

    class ToppedUp(userWalletId: String, currency: AnalyticsParam.WalletType) :
        Basic(
            event = "Topped up",
            params = mapOf(AnalyticsParam.CURRENCY to currency.value),
        ),
        OneTimeAnalyticsEvent {

        override val oneTimeEventId: String = id + userWalletId
    }

    class TransactionSent(sentFrom: AnalyticsParam.TxSentFrom, memoType: MemoType) :
        Basic(
            event = "Transaction sent",
            params = buildMap {
                this[AnalyticsParam.SOURCE] = sentFrom.value
                if (sentFrom is AnalyticsParam.TxData) {
                    this[AnalyticsParam.BLOCKCHAIN] = sentFrom.blockchain
                    this[AnalyticsParam.TOKEN_PARAM] = sentFrom.token
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

        enum class WalletForm {
            Card, Ring
        }
    }

    class ButtonSupport(source: AnalyticsParam.ScreensSources) : Basic(
        event = "Request Support",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    )
}