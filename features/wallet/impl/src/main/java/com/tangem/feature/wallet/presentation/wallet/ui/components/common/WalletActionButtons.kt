package com.tangem.feature.wallet.presentation.wallet.ui.components.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletManageButton
import com.tangem.feature.wallet.presentation.wallet.ui.components.fastForEach
import com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency.MultiCurrencyAction
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

private const val ACTIONS_CONTENT_TYPE = "Actions"

/**
 * Actions in scrollable container. Like, "Buy", "Sell", etc
 *
 * @param actions             list of buttons
 * @param selectedWalletIndex selected wallet index for creating a unique key
 * @param modifier            modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.lazyActions(
    actions: ImmutableList<WalletManageButton>,
    selectedWalletIndex: Int,
    modifier: Modifier = Modifier,
) {
    item(key = ACTIONS_CONTENT_TYPE + selectedWalletIndex, contentType = ACTIONS_CONTENT_TYPE) {
        HorizontalActionChips(
            buttons = actions.map(WalletManageButton::config).toImmutableList(),
            modifier = modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
            contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}

/**
 * Action buttons. Like, "Buy", "Sell", etc
 *
 * @param actions             list of buttons
 * @param selectedWalletIndex selected wallet index for creating a unique key
 * @param modifier            modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.actions(
    actions: ImmutableList<WalletManageButton>,
    selectedWalletIndex: Int,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    item(key = ACTIONS_CONTENT_TYPE + selectedWalletIndex, contentType = ACTIONS_CONTENT_TYPE) {
        Row(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .animateItem(fadeInSpec = null, fadeOutSpec = null),
            horizontalArrangement = Arrangement.spacedBy(space = TangemTheme.dimens.spacing8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            actions.fastForEach { action ->
                key(action::class.java) {
                    MultiCurrencyAction(
                        config = action.config,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}