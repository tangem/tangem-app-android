package com.tangem.feature.wallet.presentation.ui

import com.tangem.core.ui.R
import com.tangem.feature.wallet.presentation.state.WalletCardState
import com.tangem.feature.wallet.presentation.state.WalletStateHolder
import java.util.UUID

internal object WalletPreviewData {

    val walletCardContent = WalletCardState.Content(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        balance = "8923,05 $",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardLoading = WalletCardState.Loading(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardHiddenContent = WalletCardState.HiddenContent(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletCardError = WalletCardState.Error(
        id = UUID.randomUUID().toString(),
        title = "Wallet 1",
        additionalInfo = "3 cards • Seed enabled",
        imageResId = R.drawable.ill_businessman_3d,
        onClick = {},
    )

    val walletScreenState = WalletStateHolder(
        onBackClick = {},
        headerConfig = WalletStateHolder.HeaderConfig(
            wallets = listOf(walletCardContent, walletCardLoading, walletCardHiddenContent, walletCardError),
            onScanCardClick = {},
            onMoreClick = {},
        ),
    )
}