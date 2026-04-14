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
            AnalyticsParam.Key.SOURCE to source.value,
        ),
    ), CriticalEvent

    class SignedIn(
        signInType: SignInType,
        walletsCount: Int,
        isImported: Boolean,
    ) : Basic(
        event = "Signed in",
        params = buildMap {
            put("Sign in type", signInType.value)
            put("Wallets Count", walletsCount.toString())
            put("Wallet Type", if (isImported) "Seed Phrase" else "Seedless")
        },
    ), CriticalEvent {
        enum class SignInType(val value: String) {
            Card("Card"),
            Biometric("Biometric"),
            NoSecurity("No Security"),
            AccessCode("Access Code"),
        }
    }

    class BalanceLoaded(balance: AnalyticsParam.CardBalanceState, tokensCount: Int?) : Basic(
        event = "Balance Loaded",
        params = buildMap {
            put(AnalyticsParam.BALANCE, balance.value)
            tokensCount?.let { put(AnalyticsParam.TOKENS_COUNT, it.toString()) }
        },
    ), AppsFlyerIncludedEvent, CriticalEvent

    class TokenBalance(balance: AnalyticsParam.EmptyFull, token: String) : Basic(
        event = "Token Balance",
        params = mapOf(
            AnalyticsParam.STATE to balance.value,
            AnalyticsParam.TOKEN_PARAM to token,
        ),
    )

    class ButtonBuy(
        source: AnalyticsParam.ScreensSources,
    ) : Basic(
        event = "Button - Buy",
        params = buildMap {
            put(AnalyticsParam.Key.SOURCE, source.value)
        },
    )

    class ToppedUp(userWalletId: String, currency: AnalyticsParam.WalletType) :
        Basic(
            event = "Topped up",
            params = mapOf(AnalyticsParam.CURRENCY to currency.value),
        ),
        OneTimeAnalyticsEvent,
        CriticalEvent,
        AppsFlyerIncludedEvent {

        override val oneTimeEventId: String = id + userWalletId
    }

    class TransactionSent(sentFrom: AnalyticsParam.TxSentFrom, memoType: MemoType) :
        Basic(
            event = "Transaction sent",
            params = buildMap {
                this[AnalyticsParam.Key.SOURCE] = sentFrom.value
                if (sentFrom is AnalyticsParam.TxData) {
                    this[AnalyticsParam.Key.BLOCKCHAIN] = sentFrom.blockchain
                    this[AnalyticsParam.Key.TOKEN_PARAM] = sentFrom.token
                    sentFrom.feeType?.value?.let {
                        this[AnalyticsParam.Key.FEE_TYPE] = it
                    }
                    this[AnalyticsParam.Key.FEE_TOKEN] = sentFrom.feeToken
                }
                if (sentFrom is AnalyticsParam.TxSentFrom.Approve) {
                    this[AnalyticsParam.Key.PERMISSION_TYPE] = sentFrom.permissionType
                }
                this["Memo"] = memoType.name
            },
        ), AppsFlyerIncludedEvent, CriticalEvent {
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
            AnalyticsParam.Key.SOURCE to source.value,
        ),
    )

    class BiometryFailed(
        source: AnalyticsParam.ScreensSources,
        reason: BiometricFailReason,
    ) : Basic(
        event = "Biometry Failed",
        params = mapOf(
            AnalyticsParam.Key.SOURCE to source.value,
            "Reason" to reason.value,
        ),
    ) {
        sealed class BiometricFailReason(val value: String) {
            data object AuthenticationLockout : BiometricFailReason("BiometricsAuthenticationLockout")
            data object AuthenticationLockoutPermanent : BiometricFailReason("BiometricsAuthenticationLockoutPermanent")
            data object BiometricsAuthenticationDisabled : BiometricFailReason("BiometricsAuthenticationDisabled")
            data object AllKeysInvalidated : BiometricFailReason("AllKeysInvalidated")
            data object AuthenticationCancelled : BiometricFailReason("AuthenticationCancelled")
            data object AuthenticationAlreadyInProgress : BiometricFailReason("AuthenticationAlreadyInProgress")
            data class Other(val reason: String) : BiometricFailReason(reason)
        }
    }
}