package com.tangem.feature.tokendetails.presentation.tokendetails.state

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TokenDetailsAppBarMenuConfig(val items: ImmutableList<MenuItem>) {
    data class MenuItem(
        val title: TextReference,
        val textColorProvider: @Composable () -> Color,
        val onClick: () -> Unit,
    )
}