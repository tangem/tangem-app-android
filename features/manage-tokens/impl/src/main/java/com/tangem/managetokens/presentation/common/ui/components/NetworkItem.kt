package com.tangem.managetokens.presentation.common.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerW
import com.tangem.core.ui.components.TangemSwitch
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.managetokens.state.TokenItemState
import com.tangem.managetokens.presentation.managetokens.state.previewdata.TokenItemStatePreviewData

@Composable
internal fun NetworkItem(
    state: NetworkItemState,
    tokenState: TokenItemState.Loaded?,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
) {
    Row(
        modifier = modifier
            .background(TangemTheme.colors.background.action)
            .defaultMinSize(minHeight = TangemTheme.dimens.size68)
            .then(
                if (state is NetworkItemState.Selectable) {
                    Modifier.clickable { state.onNetworkClick(state) }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkIcon(model = state)
        SpacerW(width = TangemTheme.dimens.spacing12)
        Text(
            text = state.name,
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle2,
        )
        SpacerW(width = TangemTheme.dimens.spacing6)
        Text(
            text = state.protocolName,
            color = TangemTheme.colors.text.tertiary,
            style = TangemTheme.typography.body2,
            modifier = Modifier
                .weight(1f),
        )
        if (state is NetworkItemState.Toggleable) {
            TangemSwitch(
                onCheckedChange = {
                    state.onToggleClick(tokenState!!, state)
                },
                checked = state.isAdded.value,
            )
        } else if (state is NetworkItemState.Selectable && isSelected) {
            Icon(
                painter = painterResource(id = R.drawable.ic_check_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
    }
}

@Composable
internal fun NetworkIcon(model: NetworkItemState, modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(size = TangemTheme.dimens.size36)) {
        val isAdded = when (model) {
            is NetworkItemState.Selectable -> true
            is NetworkItemState.Toggleable -> model.isAdded.value
        }

        if (!isAdded) {
            Box(
                modifier = Modifier
                    .size(TangemTheme.dimens.size36)
                    .clip(CircleShape)
                    .background(TangemTheme.colors.control.unchecked),
            )
        }
        Icon(
            painter = painterResource(id = model.iconRes),
            contentDescription = null,
            modifier = Modifier.size(size = TangemTheme.dimens.size36),
            tint = if (isAdded) Color.Unspecified else TangemTheme.colors.text.tertiary,
        )

        if (model is NetworkItemState.Toggleable && model.isMainNetwork) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(TangemTheme.dimens.size10)
                    .clip(CircleShape)
                    .background(TangemTheme.colors.stroke.transparency),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(TangemTheme.dimens.size8)
                        .clip(CircleShape)
                        .background(TangemTheme.colors.icon.accent),
                )
            }
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_NetworkItem(@PreviewParameter(NetworkItemStateProvider::class) state: NetworkItemState) {
    TangemThemePreview {
        NetworkItem(state, tokenState = TokenItemStatePreviewData.loadedPriceDown as TokenItemState.Loaded)
    }
}

private class NetworkItemStateProvider : CollectionPreviewParameterProvider<NetworkItemState>(
    collection = listOf(
        NetworkItemState.Toggleable(
            name = "Ethereum",
            protocolName = "ETH",
            iconResId = mutableStateOf(R.drawable.img_polygon_22),
            isMainNetwork = true,
            isAdded = mutableStateOf(true),
            id = "",
            address = "",
            onToggleClick = { _, _ -> },
            decimals = 0,
        ),
        NetworkItemState.Toggleable(
            name = "BNB SMART CHAIN",
            protocolName = "BEP20",
            iconResId = mutableStateOf(R.drawable.ic_bsc_16),
            isMainNetwork = false,
            isAdded = mutableStateOf(false),
            id = "",
            address = "",
            onToggleClick = { _, _ -> },
            decimals = 0,
        ),
        NetworkItemState.Selectable(
            name = "Ethereum",
            protocolName = "ETH",
            iconResId = R.drawable.img_polygon_22,
            id = "",
            onNetworkClick = { },
        ),
    ),
)