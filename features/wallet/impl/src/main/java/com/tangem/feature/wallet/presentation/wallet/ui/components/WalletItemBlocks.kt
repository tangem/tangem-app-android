package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds.button.TangemButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.feature.wallet.presentation.wallet.ui.components.visa.TangemPayMainScreenBlock

internal fun LazyListScope.nftCollections2(state: WalletUM, itemModifier: Modifier) {
    (state as? WalletUM.Content)?.let { content ->
        item(key = "NFTCollections", contentType = "NFTCollections") {
            WalletNFTItem(
                modifier = itemModifier,
                state = content.nftState,
            )
        }
    }
}

internal fun LazyListScope.organizeTokens2(state: WalletUM, itemModifier: Modifier) {
    val organizeButton = state.tokensListUM.organizeButtonUM
    if (organizeButton != null) {
        item(
            key = "OrganizeTokensButton",
            contentType = "OrganizeTokensButton",
        ) {
            TangemButton(
                organizeButton,
                modifier = itemModifier,
            )
        }
    }
}

internal fun LazyListScope.tangemPay(walletUM: WalletUM, isBalanceHiding: Boolean, modifier: Modifier = Modifier) {
    if (walletUM is WalletState.MultiCurrency) {
        item(
            key = "TangemPayMainScreenBlock",
            contentType = walletUM.tangemPayState::class.java,
        ) {
            TangemPayMainScreenBlock(
                state = walletUM.tangemPayState,
                isBalanceHidden = isBalanceHiding,
                modifier = modifier,
            )
        }
    }
}