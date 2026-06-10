package com.tangem.core.analytics.models

sealed class Basic(
    event: String,
    params: Map<String, String> = emptyMap(),
) : AnalyticsEvent(category = "Basic", event = event, params = params) {

    /**
     * Tracks card scanning from specific entry points (Introduction, Main, My Wallets, Sign In).
     * The originating screen is reported via the [AnalyticsParam.SOURCE] parameter.
     */
    class CardWasScanned(
        source: AnalyticsParam.ScreensSources,
    ) : Basic(
        event = "Card Was Scanned",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    ), CriticalEvent

    /**
     * Tracks any sign-in into a wallet (card scan, FaceID, or wallet switch).
     * Counted as a single sign-in per session — subsequent card scans within the same session are ignored.
     */
    class SignedIn(
        signInType: AnalyticsParam.SignInType,
        walletsCount: Int,
        isImported: Boolean,
        isBackedUp: Boolean,
    ) : Basic(
        event = "Signed in",
        params = buildMap {
            put(AnalyticsParam.SIGN_IN_TYPE, signInType.value)
            put(AnalyticsParam.WALLETS_COUNT, walletsCount.toString())
            put(AnalyticsParam.WALLET_TYPE, if (isImported) "Seed Phrase" else "Seedless")
            put(AnalyticsParam.BACKUPED, if (isBackedUp) "Yes" else "No")
        },
    ), CriticalEvent, OneTimePerSessionEvent {
        override val oneTimeEventId: String = id
    }

    class ButtonBuy(
        source: AnalyticsParam.ScreensSources,
    ) : Basic(
        event = "Button - Buy",
        params = buildMap {
            put(AnalyticsParam.SOURCE, source.value)
        },
    )

    /**
     * Tracks the first time a user wallet is topped up. Sent once per wallet, when the balance
     * transitions from zero to positive (Total Balance for multi-currency wallets, or Balance for Note).
     * A wallet scanned with a non-zero balance does not count as a top-up — the event must be sent
     * only after all tokens have finished loading.
     */
    class ToppedUp(userWalletId: String, walletType: AnalyticsParam.WalletType) :
        Basic(
            event = "Topped up",
            params = mapOf(AnalyticsParam.CURRENCY to walletType.value),
        ),
        OneTimeAnalyticsEvent, AppsFlyerIncludedEvent, CriticalEvent {

        override val oneTimeEventId: String = id + userWalletId
    }

    /**
     * Tracks transaction submission from various screens (Send, Swap, WalletConnect, Sell, Approve, Staking).
     */
    class TransactionSent(sentFrom: AnalyticsParam.TxSentFrom, memoType: MemoType) :
        Basic(
            event = "Transaction sent",
            params = buildMap {
                put(AnalyticsParam.SOURCE, sentFrom.value)
                if (sentFrom is AnalyticsParam.TxData) {
                    put(AnalyticsParam.BLOCKCHAIN, sentFrom.blockchain)
                    put(AnalyticsParam.TOKEN_PARAM, sentFrom.token)
                    sentFrom.feeType?.value?.let { put(AnalyticsParam.FEE_TYPE, it) }
                    put(AnalyticsParam.FEE_TOKEN, sentFrom.feeToken)
                    put(AnalyticsParam.FEE_ASSET_TYPE, sentFrom.feeAssetType.value)
                }
                if (sentFrom is AnalyticsParam.TxSentFrom.Approve) {
                    put(AnalyticsParam.PERMISSION_TYPE, sentFrom.permissionType)
                }
                put(AnalyticsParam.MEMO, memoType.name)
            },
        ), AppsFlyerIncludedEvent, CriticalEvent {
        enum class MemoType {
            Empty, Full, Null
        }

        enum class WalletForm {
            Card, Ring
        }
    }

    /**
     * Tracks the user invoking the "Request Support" email flow from various screens of the app.
     */
    class ButtonSupport(source: AnalyticsParam.ScreensSources) : Basic(
        event = "Request Support",
        params = mapOf(
            AnalyticsParam.SOURCE to source.value,
        ),
    ), CriticalEvent

    /**
     * Tracks loading of the user's total balance after sign-in. Reports whether the balance is
     * empty, has funds, failed to load, or could not be returned because of a custom token.
     */
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
}