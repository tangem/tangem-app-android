package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.core.ui.components.buttons.actions.ActionButtonConfig
import com.tangem.core.ui.components.buttons.actions.RoundedActionButton
import com.tangem.feature.wallet.impl.R

/**
 * Organize tokens button
 *
 * @param onClick  callback, if null button is disabled
 * @param modifier modifier
 *
* [REDACTED_AUTHOR]
 */
@Composable
internal fun OrganizeTokensButton(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    RoundedActionButton(
        config = ActionButtonConfig(
            text = stringResource(id = R.string.organize_tokens_title),
            iconResId = R.drawable.ic_filter_24,
            onClick = onClick ?: {},
            enabled = onClick != null,
        ),
        modifier = modifier,
    )
}
