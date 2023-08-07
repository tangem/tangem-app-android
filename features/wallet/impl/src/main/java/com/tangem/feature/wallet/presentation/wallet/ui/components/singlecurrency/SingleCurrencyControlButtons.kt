package com.tangem.feature.wallet.presentation.wallet.ui.components.singlecurrency

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import com.tangem.core.ui.components.buttons.HorizontalActionChips
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.res.TangemTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Single currency control buttons. Like, "Buy", "Sell", etc
 *
 * @param configs  list of buttons
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
internal fun LazyListScope.controlButtons(configs: ImmutableList<ActionButtonConfig>, modifier: Modifier = Modifier) {
    item {
        HorizontalActionChips(
            buttons = configs,
            modifier = modifier,
            contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing16),
        )
    }
}
