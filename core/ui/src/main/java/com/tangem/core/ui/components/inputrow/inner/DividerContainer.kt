package com.tangem.core.ui.components.inputrow.inner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme

@Composable
fun DividerContainer(
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    paddingValues: PaddingValues = PaddingValues(start = TangemTheme.dimens.spacing12),
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(paddingValues),
                color = TangemTheme.colors.stroke.primary,
                thickness = TangemTheme.dimens.size0_5,
            )
        }
    }
}
