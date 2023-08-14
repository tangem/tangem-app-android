package com.tangem.feature.wallet.presentation.wallet.ui.components.multicurrency

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier

private const val ORGANIZE_BUTTON_CONTENT_TYPE = "OrganizeTokensButton"

/**
 * Organize tokens button
 *
 * @param onClick  callback is invoked when button is clicked
 * @param modifier modifier
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalFoundationApi::class)
internal fun LazyListScope.organizeButton(onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    item(key = ORGANIZE_BUTTON_CONTENT_TYPE, contentType = ORGANIZE_BUTTON_CONTENT_TYPE) {
        OrganizeTokensButton(onClick = onClick, modifier = modifier.animateItemPlacement())
    }
}