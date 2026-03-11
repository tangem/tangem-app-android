package com.tangem.features.feed.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun ContainerWithDivider(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(start = TangemTheme.dimens2.x3),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(paddingValues),
                color = TangemTheme.colors2.graphic.neutral.quaternary,
                thickness = 1.dp,
            )
        }
    }
}