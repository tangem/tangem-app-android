package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import javax.inject.Inject

internal class WalletClickIntentsV2 @Inject constructor(
    private val walletCardClickIntentsImplementor: WalletCardClickIntentsImplementor,
    private val warningsClickIntentsImplementer: WalletWarningsClickIntentsImplementer,
    private val currencyActionsClickIntentsImplementor: WalletCurrencyActionsClickIntentsImplementor,
    private val contentClickIntentsImplementor: WalletContentClickIntentsImplementor,
) : WalletCardClickIntents by walletCardClickIntentsImplementor,
    WalletWarningsClickIntents by warningsClickIntentsImplementer,
    WalletCurrencyActionsClickIntents by currencyActionsClickIntentsImplementor,
    WalletContentClickIntents by contentClickIntentsImplementor {

    @Suppress("UnusedPrivateMember")
    fun onWalletChange(index: Int) {
// [REDACTED_TODO_COMMENT]
    }

    fun onRefreshSwipe() {
// [REDACTED_TODO_COMMENT]
    }

    fun onReloadClick() {
// [REDACTED_TODO_COMMENT]
    }
}
