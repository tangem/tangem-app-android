package com.tangem.feature.tokendetails.presentation.tokendetails.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.tokenaction.TokenActionRow
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.button.SecondaryTangemButton
import com.tangem.core.ui.ds.button.TangemButtonShape
import com.tangem.core.ui.ds.button.TangemButtonSize
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.LocalRedesignEnabled
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TransferUM
import dev.chrisbanes.haze.rememberHazeState
import com.tangem.core.ui.R as CoreR

@Composable
internal fun TransferBottomSheetContent(state: TransferUM, onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        SendActionRow(state = state)
        SwapActionRow(state = state)
        SellActionRow(state = state)

        SpacerH(TangemTheme.dimens2.x2)

        CompositionLocalProvider(LocalHazeState provides rememberHazeState()) {
            SecondaryTangemButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onCloseClick,
                text = resourceReference(CoreR.string.common_close),
                size = TangemButtonSize.X12,
                shape = TangemButtonShape.Rounded,
            )
        }

        SpacerH(TangemTheme.dimens2.x4)
    }
}

@Composable
private fun SendActionRow(state: TransferUM) {
    val row = (state as? TransferUM.Content)?.send
    if (state is TransferUM.Content && row == null) return
    ActionRow(
        iconRes = CoreR.drawable.ic_arrow_up_24,
        title = resourceReference(CoreR.string.common_send),
        description = resourceReference(CoreR.string.quick_action_send_description),
        row = row,
        isLoading = state is TransferUM.Loading,
    )
}

@Composable
private fun SwapActionRow(state: TransferUM) {
    val row = (state as? TransferUM.Content)?.swap
    if (state is TransferUM.Content && row == null) return
    ActionRow(
        iconRes = CoreR.drawable.ic_exchange_mini_24,
        title = resourceReference(CoreR.string.common_swap),
        description = resourceReference(CoreR.string.quick_action_swap_description),
        row = row,
        isLoading = state is TransferUM.Loading,
    )
}

@Composable
private fun SellActionRow(state: TransferUM) {
    val row = (state as? TransferUM.Content)?.sell
    if (state is TransferUM.Content && row == null) return
    ActionRow(
        iconRes = CoreR.drawable.ic_currency_24,
        title = resourceReference(CoreR.string.common_sell),
        description = resourceReference(CoreR.string.quick_action_sell_description),
        row = row,
        isLoading = state is TransferUM.Loading,
    )
}

@Composable
private fun ActionRow(
    iconRes: Int,
    title: TextReference,
    description: TextReference,
    row: TransferUM.Row?,
    isLoading: Boolean,
) {
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
@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview(@PreviewParameter(TransferPreviewProvider::class) state: TransferUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            TransferBottomSheetContent(
                state = state,
                onCloseClick = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private class TransferPreviewProvider : PreviewParameterProvider<TransferUM> {
    override val values: Sequence<TransferUM> = sequenceOf(
        TransferUM.Loading,
        TransferUM.Content(
            send = TransferUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            swap = TransferUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            sell = TransferUM.Row(isLoading = false, isEnabled = true, onClick = {}),
        ),
        TransferUM.Content(
            send = TransferUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            swap = TransferUM.Row(isLoading = false, isEnabled = false, onClick = {}),
            sell = TransferUM.Row(isLoading = false, isEnabled = false, onClick = {}),
        ),
        TransferUM.Content(
            send = TransferUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            swap = null,
            sell = null,
        ),
    )
}
// endregion