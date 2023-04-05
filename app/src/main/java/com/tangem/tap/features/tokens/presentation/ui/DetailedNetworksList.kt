package com.tangem.tap.features.tokens.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.tokens.presentation.states.AddTokensNetworkItemState
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun DetailedNetworksList(
    isExpanded: Boolean,
    networks: List<AddTokensNetworkItemState>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isExpanded,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        if (isExpanded) {
            Column {
                networks.forEachIndexed { index, network ->
                    key(network.name) {
                        DetailedNetworkItem(model = network, isLastItem = networks.lastIndex == index)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailedNetworkItem(model: AddTokensNetworkItemState, isLastItem: Boolean) {
    val itemHeight = TangemTheme.dimens.size50
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = itemHeight)
            .padding(start = TangemTheme.dimens.spacing38),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkItemArrow(itemHeight = itemHeight, isLastItem = isLastItem)
        Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing14))
        BriefNetworkItem(model = model)
        Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing6))
        NetworkTitle(model = model)

        if (model is AddTokensNetworkItemState.EditAccess) {
            Switch(
                checked = model.isAdded,
                onCheckedChange = { model.onToggleClick(model.networkId) },
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing16, end = TangemTheme.dimens.spacing8),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TangemColorPalette.Meadow,
                ),
            )
        }
    }
}

@Composable
private fun RowScope.NetworkTitle(model: AddTokensNetworkItemState) {
    Text(
        modifier = Modifier.weight(1f),
        text = buildAnnotatedString {
            append(text = model.name)
            append(text = " ")
            withStyle(
                style = SpanStyle(
                    fontWeight = FontWeight.Normal,
                    color = if (model.isMainNetwork) TangemColorPalette.Meadow else TangemColorPalette.Dark2,
                ),
            ) {
                append(text = model.protocolName)
            }
        },
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = if (model is AddTokensNetworkItemState.EditAccess && model.isAdded) {
            TangemColorPalette.Black
        } else {
            TangemColorPalette.Dark2
        },
    )
}

@Preview
@Composable
private fun Preview_DetailedNetworksList_EditAccess() {
    TangemTheme {
        DetailedNetworksList(
            networks = listOf(
                AddTokensNetworkItemState.EditAccess(
                    name = "ETHEREUM",
                    protocolName = "MAIN",
                    iconResId = R.drawable.ic_eth_no_color,
                    isMainNetwork = true,
                    isAdded = true,
                    networkId = "",
                    onToggleClick = {},
                ),
                AddTokensNetworkItemState.EditAccess(
                    name = "BNB SMART CHAIN",
                    protocolName = "BEP20",
                    iconResId = R.drawable.ic_bsc_no_color,
                    isMainNetwork = false,
                    isAdded = false,
                    networkId = "",
                    onToggleClick = {},
                ),
            ),
            isExpanded = false,
        )
    }
}

@Preview
@Composable
private fun Preview_DetailedNetworksList_ReadAccess() {
    TangemTheme {
        DetailedNetworksList(
            networks = listOf(
                AddTokensNetworkItemState.ReadAccess(
                    name = "ETHEREUM",
                    protocolName = "MAIN",
                    iconResId = R.drawable.ic_eth_no_color,
                    isMainNetwork = true,
                ),
                AddTokensNetworkItemState.ReadAccess(
                    name = "BNB SMART CHAIN",
                    protocolName = "BEP20",
                    iconResId = R.drawable.ic_bsc_no_color,
                    isMainNetwork = false,
                ),
            ),
            isExpanded = false,
        )
    }
}