package com.tangem.tap.features.tokens.impl.presentation.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.tap.features.tokens.impl.presentation.states.NetworkItemState
import com.tangem.tap.features.tokens.impl.presentation.states.TokenItemState
import kotlinx.collections.immutable.ImmutableCollection

/**
[REDACTED_AUTHOR]
 */
@Composable
internal fun DetailedNetworksList(
    isExpanded: Boolean,
    token: TokenItemState,
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
                key(network.name + network.protocolName) {
                    DetailedNetworkItem(token = token, network = network, isLastItem = networks.size - 1 == index)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetailedNetworkItem(token: TokenItemState, network: NetworkItemState, isLastItem: Boolean) {
    val clipboardManager = LocalClipboardManager.current
    val itemHeight = TangemTheme.dimens.size50

    Row(
        modifier = Modifier
            .combinedClickable(
                enabled = network is NetworkItemState.ManageContent,
                onLongClick = {
                    if (network is NetworkItemState.ManageContent && network.address != null) {
                        clipboardManager.setText(AnnotatedString(text = network.address))
                        network.onNetworkClick()
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
        BriefNetworkItem(model = network)
        Spacer(modifier = Modifier.width(TangemTheme.dimens.spacing6))
        NetworkTitle(model = network)

        if (network is NetworkItemState.ManageContent) {
            Switch(
                checked = network.isAdded.value,
                onCheckedChange = {
                    network.onToggleClick(
                        requireNotNull(token as? TokenItemState.ManageContent),
                        network,
                    )
                },
                modifier = Modifier.padding(start = TangemTheme.dimens.spacing16, end = TangemTheme.dimens.spacing8),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TangemTheme.colors.control.key,
                    checkedTrackColor = TangemTheme.colors.icon.accent,
                    uncheckedThumbColor = TangemTheme.colors.control.key,
                    uncheckedTrackColor = TangemTheme.colors.icon.informative,
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
                    color = if (model.isMainNetwork) {
                        TangemTheme.colors.icon.accent
                    } else {
                        TangemTheme.colors.icon.secondary
                    },
                ),
            ) {
                append(text = model.protocolName)
            }
        },
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = if (model is NetworkItemState.ManageContent && model.isAdded.value) {
            TangemTheme.colors.text.primary1
        } else {
            TangemTheme.colors.text.secondary
        },
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DetailedNetworksList_ManageAccess() {
    TangemThemePreview {
        DetailedNetworksList(
            isExpanded = true,
            token = TokenListPreviewData.createManageToken(),
            networks = TokenListPreviewData.createManageNetworksList(),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_DetailedNetworksList_ReadAccess() {
    TangemThemePreview {
        DetailedNetworksList(
            isExpanded = true,
            token = TokenListPreviewData.createReadToken(),
            networks = TokenListPreviewData.createReadNetworksList(),
        )
    }
}