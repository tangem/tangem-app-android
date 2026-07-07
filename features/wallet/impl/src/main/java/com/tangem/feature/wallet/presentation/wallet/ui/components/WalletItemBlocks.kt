package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.ds.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.virtualaccount.main.component.VirtualAccountMainBlockComponent
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM

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
            val testTag = if (organizeButton.text == resourceReference(R.string.main_add_and_manage_tokens)) {
                MainScreenTestTags.ADD_AND_MANAGE_BUTTON
            } else {
                MainScreenTestTags.ORGANIZE_TOKENS_BUTTON
            }
            TangemButton(
                buttonUM = organizeButton,
                modifier = itemModifier.testTag(testTag),
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

internal fun LazyListScope.virtualAccount(
    virtualAccountComponent: VirtualAccountMainBlockComponent,
    virtualAccountUM: VirtualAccountMainUM,
    isBalanceHidden: Boolean,
    modifier: Modifier = Modifier,
) {
    with(virtualAccountComponent) {
        virtualAccountMainContent(modifier = modifier, state = virtualAccountUM, isBalanceHidden = isBalanceHidden)
    }
}