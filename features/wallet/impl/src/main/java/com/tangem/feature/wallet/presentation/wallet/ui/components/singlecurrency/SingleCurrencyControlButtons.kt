package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val CONTROL_BUTTONS_CONTENT_TYPE = "ControlButtons"

/**
 * Single currency control buttons. Like, "Buy", "Sell", etc
 *
 * @param configs             list of buttons
 * @param selectedWalletIndex selected wallet index for creating a unique key
 * @param modifier            modifier
 *
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.controlButtons(
    configs: ImmutableList<WalletManageButton>,
    selectedWalletIndex: Int,
    modifier: Modifier = Modifier,
) {
    item(key = CONTROL_BUTTONS_CONTENT_TYPE + selectedWalletIndex, contentType = CONTROL_BUTTONS_CONTENT_TYPE) {
        HorizontalActionChips(
            buttons = configs.map(WalletManageButton::config).toImmutableList(),
            modifier = modifier.animateItemPlacement(),
            contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}
