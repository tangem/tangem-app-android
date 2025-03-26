package com.tangem.features.onboarding.v2.common.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.onboarding.v2.impl.R

@Composable
internal fun RefreshButton(isRefreshing: Boolean, onRefreshBalanceClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(TangemTheme.dimens.size48)
            .shadow(elevation = 2.dp, shape = CircleShape)
            .background(TangemTheme.colors.background.action)
            .clickable(
                enabled = !isRefreshing,
                onClick = onRefreshBalanceClick,
                indication = ripple(),
                interactionSource = remember { MutableInteractionSource() },
            ),
    ) {
        if (isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.padding(TangemTheme.dimens.spacing12),
                color = TangemTheme.colors.icon.primary1,
                strokeWidth = TangemTheme.dimens.size2,
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_refresh_24),
                contentDescription = null,
                tint = TangemTheme.colors.text.primary1,
                modifier = Modifier.padding(TangemTheme.dimens.spacing12),
            )
        }
    }
}