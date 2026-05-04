package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.ds.button.TangemButton
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM

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
                buttonUM = organizeButton,
                modifier = itemModifier,
            )
        }
    }
}

internal fun LazyListScope.tangemPay(
    tangemPayComponent: TangemPayMainBlockComponent,
    tangemPayUM: TangemPayMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    with(tangemPayComponent) {
        tangemPayMainContent(modifier = modifier, state = tangemPayUM, isBalanceHidden = isBalanceHidden)
    }
}