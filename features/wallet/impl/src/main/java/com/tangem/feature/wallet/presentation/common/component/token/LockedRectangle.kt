package com.tangem.feature.wallet.presentation.common.component.token

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun LockedRectangle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(
            color = TangemTheme.colors.background.secondary,
            shape = RoundedCornerShape(TangemTheme.dimens.radius4),
        ),
    )
}