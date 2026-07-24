package com.tangem.feature.wallet.presentation.wallet.ui.components

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.haptic.TangemHapticEffect
import com.tangem.core.ui.res.LocalHapticManager
import com.tangem.core.ui.test.MainScreenTestTags
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletUM
import com.tangem.features.tangempay.component.TangemPayMainBlockComponent
import com.tangem.features.tangempay.entity.TangemPayMainUM
import com.tangem.features.virtualaccount.main.component.VirtualAccountMainBlockComponent
import com.tangem.features.virtualaccount.main.entity.VirtualAccountMainUM

internal const val ORGANIZE_TOKENS_BUTTON_ITEM_KEY = "OrganizeTokensButton"

internal fun LazyListScope.nftCollections2(state: WalletUM, itemModifier: Modifier) {
    (state as? WalletUM.Content)?.let { content ->
        item(key = "NFTCollections", contentType = "NFTCollections") {
            WalletNFTItem2(
                modifier = itemModifier,
                state = content.nftState,
            )
        }
    }
}

/**
 * @param onButtonBoundsChange called with the button root-coordinates bounds on every placement change
 * and with `null` when the button leaves composition
 */
internal fun LazyListScope.organizeTokens2(
    state: WalletUM,
    itemModifier: Modifier,
    onButtonBoundsChange: (Rect?) -> Unit,
) {
    val organizeButton = state.tokensListUM.organizeButtonUM
    if (organizeButton != null) {
        item(
            key = ORGANIZE_TOKENS_BUTTON_ITEM_KEY,
            contentType = ORGANIZE_TOKENS_BUTTON_ITEM_KEY,
        ) {
            val hapticManager = LocalHapticManager.current
            val testTag = if (organizeButton.text == resourceReference(R.string.main_add_and_manage_tokens)) {
                MainScreenTestTags.ADD_AND_MANAGE_BUTTON
            } else {
                MainScreenTestTags.ORGANIZE_TOKENS_BUTTON
            }

            val currentOnButtonBoundsChange by rememberUpdatedState(onButtonBoundsChange)
            DisposableEffect(Unit) {
                onDispose { currentOnButtonBoundsChange(null) }
            }

            TangemButton(
                modifier = itemModifier
                    .onGloballyPositioned { currentOnButtonBoundsChange(it.boundsInRoot()) }
                    .testTag(testTag),
                size = TangemButton.Size.X9,
                onClick = {
                    hapticManager.perform(TangemHapticEffect.View.ContextClick)
                    organizeButton.onClick()
                },
                variant = TangemButton.Variant.Secondary,
                iconStart = organizeButton.tangemIconUM,
                text = organizeButton.text,
                isEnabled = organizeButton.isEnabled,
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