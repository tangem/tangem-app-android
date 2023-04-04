package com.tangem.tap.features.tokens.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.tokens.presentation.states.NetworkItemState
import kotlinx.collections.immutable.ImmutableCollection

/**
 * @author Andrew Khokhlov on 27/03/2023
 */
@Composable
internal fun DetailedNetworksList(
    isExpanded: Boolean,
    tokenId: String?,
    networks: ImmutableCollection<NetworkItemState>,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = isExpanded,
        modifier = modifier,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Column {
            networks.forEachIndexed { index, network ->
                key(network.name) {
                    DetailedNetworkItem(model = network, tokenId = tokenId, isLastItem = networks.size - 1 == index)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailedNetworkItem(model: NetworkItemState, tokenId: String?, isLastItem: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    val itemHeight = TangemTheme.dimens.size50

    Row(
        modifier = Modifier
            .combinedClickable(
                enabled = model is NetworkItemState.ManageAccess,
                onLongClick = {
                    if (model is NetworkItemState.ManageAccess && model.contractAddress != null) {
                        clipboardManager.setText(AnnotatedString(text = model.contractAddress))
                        model.onNetworkClick()
                    }
                },
                onClick = {},
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            )
            .fillMaxWidth()
            .heightIn(min = itemHeight)
            .padding(start = TangemTheme.dimens.spacing38),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkItemArrow(itemHeight = itemHeight, isLastItem = isLastItem)
        Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing16))
        BriefNetworkItem(model = model)
        Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing6))
        NetworkTitle(model = model)

        if (model is NetworkItemState.ManageAccess) {
            Switch(
                checked = model.isAdded,
                onCheckedChange = { model.onToggleClick(requireNotNull(tokenId), model.networkId) },
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing16, end = TangemTheme.dimens.spacing8),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TangemColorPalette.Meadow,
                ),
            )
        }
    }
}

@Composable
private fun RowScope.NetworkTitle(model: NetworkItemState) {
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
        color = if (model is NetworkItemState.ManageAccess && model.isAdded) {
            TangemColorPalette.Black
        } else {
            TangemColorPalette.Dark2
        },
    )
}

@Preview
@Composable
private fun Preview_DetailedNetworksList_ManageAccess() {
    TangemTheme {
        DetailedNetworksList(
            isExpanded = true,
            tokenId = null,
            networks = TokenListPreviewData.createManageNetworksList(),
        )
    }
}

@Preview
@Composable
private fun Preview_DetailedNetworksList_ReadAccess() {
    TangemTheme {
        DetailedNetworksList(
            isExpanded = true,
            tokenId = null,
            networks = TokenListPreviewData.createReadNetworksList(),
        )
    }
}
