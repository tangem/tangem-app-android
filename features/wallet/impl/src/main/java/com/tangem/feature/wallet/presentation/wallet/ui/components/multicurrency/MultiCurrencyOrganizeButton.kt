package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier

/**
 * Organize tokens button
 *
 * @param onClick  callback is invoked when button is clicked
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
internal fun LazyListScope.organizeButton(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    item { OrganizeTokensButton(onClick = onClick, modifier = modifier) }
}