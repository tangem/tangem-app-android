package com.tangem.feature.wallet.presentation.preview

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletActionButtons
import kotlinx.collections.immutable.persistentListOf

internal object WalletPreviewData {

    val wallets by lazy {
        mapOf(
            UserWalletId(stringValue = "123") to WalletBalancePreview.content,
            UserWalletId(stringValue = "321") to WalletBalancePreview.loading,
            UserWalletId(stringValue = "24") to WalletBalancePreview.error,
        )
    }

    val actionButtons = persistentListOf(
        WalletActionButtons.Buy({}, false).buttonUM,
        WalletActionButtons.Swap({}, false).buttonUM,
        WalletActionButtons.Sell({}, false).buttonUM,
    )
}