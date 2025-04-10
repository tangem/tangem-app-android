package com.tangem.core.ui.components.currency.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun CurrencyIconBottomBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size12)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = CircleShape,
            ),
    ) {
        Box(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing2)
                .matchParentSize()
                .background(
                    color = TangemTheme.colors.icon.informative,
                    shape = CircleShape,
                ),
        )
    }
}