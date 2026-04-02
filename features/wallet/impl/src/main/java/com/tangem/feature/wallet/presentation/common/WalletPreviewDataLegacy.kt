package com.tangem.feature.wallet.presentation.common

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAdditionalInfo
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletCardState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletTopBarConfig
import kotlinx.collections.immutable.persistentListOf

@Suppress("LargeClass")
internal object WalletPreviewDataLegacy {

    val topBarConfig by lazy { WalletTopBarConfig() }

    val walletCardContentState by lazy {
        WalletCardState.Content(
            id = UserWalletId(stringValue = "123"),
            title = "Wallet1Wallet1Wallet1Wallet1Wallet1Wallet1Wallet1Wallet1",
            balance = "8923,05312312312312312312331231231233432423423424234 $",
            additionalInfo = WalletAdditionalInfo(
                hideable = false,
                content = WalletAdditionalInfo.Content.Text(
                    TextReference.Str("3 cards • Seed phrase3 cards • Seed phrasephrasephrasephrase"),
                ),
            ),
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            dropDownItems = persistentListOf(),
            cardCount = 1,
            isZeroBalance = false,
            isBalanceFlickering = false,
        )
    }

    val walletCardLoadingState by lazy {
        WalletCardState.Loading(
            id = UserWalletId("321"),
            title = "Wallet 1",
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            dropDownItems = persistentListOf(),
        )
    }

    val walletCardErrorState by lazy {
        WalletCardState.Error(
            id = UserWalletId("24"),
            title = "Wallet 1",
            imageResId = R.drawable.ill_wallet2_cards3_120_106,
            dropDownItems = persistentListOf(),
        )
    }

    val wallets by lazy {
        mapOf(
            UserWalletId(stringValue = "123") to walletCardContentState,
            UserWalletId(stringValue = "321") to walletCardLoadingState,
            UserWalletId(stringValue = "24") to walletCardErrorState,
        )
    }
}