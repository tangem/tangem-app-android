package com.tangem.feature.tokendetails.presentation.tokendetails.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.tokenaction.TokenActionRow
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.tokendetails.presentation.tokendetails.state.ZeroBalanceActionsUM
import com.tangem.core.ui.R as CoreR

@Composable
internal fun ZeroBalanceActionsBlock(state: ZeroBalanceActionsUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = TangemTheme.dimens2.x6),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        ActionRow(
            iconRes = CoreR.drawable.ic_credit_card_20,
            title = resourceReference(CoreR.string.common_buy),
            description = resourceReference(CoreR.string.quick_action_buy_description),
            row = (state as? ZeroBalanceActionsUM.Content)?.buy,
            isLoading = state is ZeroBalanceActionsUM.Loading,
        )
        ActionRow(
            iconRes = CoreR.drawable.ic_exchange_mini_24,
            title = resourceReference(CoreR.string.common_swap),
            description = resourceReference(CoreR.string.quick_action_swap_description),
            row = (state as? ZeroBalanceActionsUM.Content)?.swap,
            isLoading = state is ZeroBalanceActionsUM.Loading,
        )
        ActionRow(
            iconRes = CoreR.drawable.ic_qrcode_new_24,
            title = resourceReference(CoreR.string.common_receive),
            description = resourceReference(CoreR.string.quick_action_receive_description),
            row = (state as? ZeroBalanceActionsUM.Content)?.receive,
            isLoading = state is ZeroBalanceActionsUM.Loading,
        )
    }
}

@Composable
private fun ActionRow(
    @DrawableRes iconRes: Int,
    title: TextReference,
    description: TextReference,
    row: ZeroBalanceActionsUM.Row?,
    isLoading: Boolean,
) {
    // UM-level Loading (no emission yet) OR per-row Loading (Buy/Swap reason carries Loading
    // marker) → spinner. Otherwise chevron tail with `isEnabled` gating clicks.
    if (isLoading || row?.isLoading == true) {
        TokenActionRow(
            iconRes = iconRes,
            title = title,
            description = description,
            tailContent = { TailLoader() },
        )
    } else {
        TokenActionRow(
            iconRes = iconRes,
            title = title,
            description = description,
            onClick = row?.onClick,
            onLongClick = row?.onLongClick,
            isEnabled = row?.isEnabled == true,
        )
    }
}

@Composable
private fun TailLoader() {
    CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        color = TangemTheme.colors2.graphic.neutral.tertiary,
        strokeWidth = 2.dp,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360)
@Composable
private fun Preview(@PreviewParameter(ZeroBalanceActionsPreviewProvider::class) state: ZeroBalanceActionsUM) {
    TangemThemePreviewRedesign {
        ZeroBalanceActionsBlock(state = state)
    }
}

private class ZeroBalanceActionsPreviewProvider : PreviewParameterProvider<ZeroBalanceActionsUM> {
    override val values: Sequence<ZeroBalanceActionsUM> = sequenceOf(
        ZeroBalanceActionsUM.Loading,
        ZeroBalanceActionsUM.Content(
            buy = ZeroBalanceActionsUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            swap = ZeroBalanceActionsUM.Row(isLoading = false, isEnabled = false, onClick = {}),
            receive = ZeroBalanceActionsUM.Row(isLoading = false, isEnabled = true, onClick = {}, onLongClick = {}),
        ),
    )
}
// endregion