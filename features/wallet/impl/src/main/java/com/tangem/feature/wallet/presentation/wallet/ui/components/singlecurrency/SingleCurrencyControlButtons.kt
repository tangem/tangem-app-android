package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletManageButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Single currency control buttons. Like, "Buy", "Sell", etc
 *
 * @param configs  list of buttons
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.controlButtons(configs: ImmutableList<WalletManageButton>, modifier: Modifier = Modifier) {
    item {
        HorizontalActionChips(
            buttons = configs.map(WalletManageButton::config).toImmutableList(),
            modifier = modifier.animateItemPlacement(),
            contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}