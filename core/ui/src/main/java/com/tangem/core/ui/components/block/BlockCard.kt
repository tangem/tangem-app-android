package com.tangem.core.ui.components.block

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme

@Composable
fun BlockCard(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CardColors = TangemBlockCardColors,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        shape = TangemTheme.shapes.roundedCornersXMedium,
        colors = colors,
        enabled = enabled,
        content = content,
    )
}

val TangemBlockCardColors: CardColors
    @Composable
    @ReadOnlyComposable
    get() = CardColors(
        containerColor = TangemTheme.colors.background.primary,
        contentColor = TangemTheme.colors.text.primary1,
        disabledContainerColor = TangemTheme.colors.background.primary,
        disabledContentColor = TangemTheme.colors.text.primary1,
    )
