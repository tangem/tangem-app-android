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
import com.tangem.feature.tokendetails.presentation.tokendetails.state.AddFundsUM
import dev.chrisbanes.haze.rememberHazeState
import com.tangem.core.ui.R as CoreR

@Composable
internal fun AddFundsBottomSheetContent(state: AddFundsUM, onCloseClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
    ) {
        BuyActionRow(state = state)
        SwapActionRow(state = state)
        ReceiveActionRow(state = state)

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
    }
}

@Composable
private fun BuyActionRow(state: AddFundsUM) {
    val row = (state as? AddFundsUM.Content)?.buy
    if (state is AddFundsUM.Content && row == null) return
    ActionRow(
        iconRes = CoreR.drawable.ic_credit_card_20,
        title = resourceReference(CoreR.string.common_buy),
        description = resourceReference(CoreR.string.quick_action_buy_description),
        row = row,
        isLoading = state is AddFundsUM.Loading,
    )
}

@Composable
private fun SwapActionRow(state: AddFundsUM) {
    val row = (state as? AddFundsUM.Content)?.swap
    if (state is AddFundsUM.Content && row == null) return
    ActionRow(
        iconRes = CoreR.drawable.ic_exchange_mini_24,
        title = resourceReference(CoreR.string.common_swap),
        description = resourceReference(CoreR.string.quick_action_swap_description),
        row = row,
        isLoading = state is AddFundsUM.Loading,
    )
}

@Composable
private fun ReceiveActionRow(state: AddFundsUM) {
    val row = (state as? AddFundsUM.Content)?.receive
    if (state is AddFundsUM.Content && row == null) return
    ActionRow(
        iconRes = CoreR.drawable.ic_qrcode_new_24,
        title = resourceReference(CoreR.string.common_receive),
        description = resourceReference(CoreR.string.quick_action_receive_description),
        row = row,
        isLoading = state is AddFundsUM.Loading,
    )
}

@Composable
private fun ActionRow(
    iconRes: Int,
    title: TextReference,
    description: TextReference,
    row: AddFundsUM.Row?,
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
@Preview(widthDp = 360, showBackground = true)
@Composable
private fun Preview(@PreviewParameter(AddFundsPreviewProvider::class) state: AddFundsUM) {
    TangemThemePreviewRedesign {
        CompositionLocalProvider(LocalRedesignEnabled provides true) {
            AddFundsBottomSheetContent(
                state = state,
                onCloseClick = {},
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private class AddFundsPreviewProvider : PreviewParameterProvider<AddFundsUM> {
    override val values: Sequence<AddFundsUM> = sequenceOf(
        AddFundsUM.Loading,
        AddFundsUM.Content(
            buy = AddFundsUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            swap = AddFundsUM.Row(isLoading = false, isEnabled = true, onClick = {}),
            receive = AddFundsUM.Row(isLoading = false, isEnabled = true, onClick = {}, onLongClick = {}),
        ),
        AddFundsUM.Content(
            buy = AddFundsUM.Row(isLoading = false, isEnabled = false, onClick = {}),
            swap = AddFundsUM.Row(isLoading = false, isEnabled = false, onClick = {}),
            receive = AddFundsUM.Row(isLoading = false, isEnabled = true, onClick = {}, onLongClick = {}),
        ),
        AddFundsUM.Content(
            buy = null,
            swap = null,
            receive = AddFundsUM.Row(isLoading = false, isEnabled = true, onClick = {}, onLongClick = {}),
        ),
    )
}
// endregion