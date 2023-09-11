package com.tangem.feature.wallet.presentation.common.component.token.icon

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import com.tangem.core.ui.res.TangemTheme

@Composable
internal fun NetworkBadge(@DrawableRes iconResId: Int, colorFilter: ColorFilter?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size18)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = CircleShape,
            ),
    ) {
        Image(
            modifier = Modifier
                .padding(all = TangemTheme.dimens.spacing2)
                .matchParentSize(),
            painter = painterResource(id = iconResId),
            colorFilter = colorFilter,
            contentDescription = null,
        )
    }
}

@Composable
internal fun CustomBadge(modifier: Modifier = Modifier) {
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