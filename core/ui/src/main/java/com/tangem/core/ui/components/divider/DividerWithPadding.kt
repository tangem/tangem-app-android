package com.tangem.core.ui.components.divider

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme

@Composable
fun DividerWithPadding(start: Dp = 0.dp, end: Dp = 0.dp, top: Dp = 0.dp, bottom: Dp = 0.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(
            start = start,
            end = end,
            top = top,
            bottom = bottom,
        ),
        thickness = TangemTheme.dimens.size1,
        color = TangemTheme.colors.stroke.primary,
    )
}