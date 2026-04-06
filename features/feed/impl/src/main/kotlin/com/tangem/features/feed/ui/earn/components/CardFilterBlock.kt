package com.tangem.features.feed.ui.earn.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun CardFilterBlock(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = TangemTheme.colors2.surface.level2,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            )
            .border(
                width = 1.dp,
                color = TangemTheme.colors2.border.neutral.primary,
                shape = RoundedCornerShape(TangemTheme.dimens2.x5),
            ),
        content = content,
    )
}