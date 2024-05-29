package com.tangem.features.details.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun BlockCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = TangemTheme.shapes.roundedCornersXMedium,
        colors = BlockColors,
        enabled = enabled,
        content = content,
    )
}

private val BlockColors: CardColors
    @Composable
    @ReadOnlyComposable
    get() = CardColors(
        containerColor = TangemTheme.colors.background.primary,
        contentColor = TangemTheme.colors.text.primary1,
        disabledContainerColor = TangemTheme.colors.button.disabled,
        disabledContentColor = TangemTheme.colors.text.disabled,
    )