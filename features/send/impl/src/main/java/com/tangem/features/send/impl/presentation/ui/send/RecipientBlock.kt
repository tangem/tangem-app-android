package com.tangem.features.send.impl.presentation.ui.send

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.state.previewdata.RecipientStatePreviewData

@Composable
internal fun RecipientBlock(
    recipientState: SendStates.RecipientState,
    isSuccess: Boolean,
    isEditingDisabled: Boolean,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isEditingDisabled) {
        TangemTheme.colors.button.disabled
    } else {
        TangemTheme.colors.background.action
    }

    Column(
        modifier = Modifier
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(backgroundColor)
            .clickable(enabled = !isSuccess && !isEditingDisabled, onClick = onClick)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        AddressBlock(recipientState.addressTextField)
        MemoBlock(recipientState.memoTextField)
    }
}

@Composable
private fun AddressBlock(address: SendTextField.RecipientAddress) {
    Text(
        text = address.label.resolveReference(),
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.secondary,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
    ) {
        IdentIcon(
            address = address.value,
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius18))
                .background(TangemTheme.colors.background.tertiary),
        )
        Text(
            text = address.value,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Composable
private fun MemoBlock(memo: SendTextField.RecipientMemo?) {
    val showMemo = memo != null && memo.value.isNotBlank()
    if (showMemo) {
        HorizontalDivider(
            color = TangemTheme.colors.icon.inactive,
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
        )
        Text(
            text = memo?.label?.resolveReference().orEmpty(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )
        Text(
            text = memo?.value.orEmpty(),
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
        )
    }
}

// region Preview
@Preview
@Composable
private fun RecipientBlockPreview_Light(
    @PreviewParameter(RecipientBlockPreviewProvider::class) value: SendStates.RecipientState,
) {
    TangemTheme {
        RecipientBlock(
            recipientState = value,
            isSuccess = true,
            isEditingDisabled = false,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun RecipientBlockPreview_Dark(
    @PreviewParameter(RecipientBlockPreviewProvider::class) value: SendStates.RecipientState,
) {
    TangemTheme(isDark = true) {
        RecipientBlock(
            recipientState = value,
            isSuccess = true,
            isEditingDisabled = false,
            onClick = {},
        )
    }
}

private class RecipientBlockPreviewProvider : PreviewParameterProvider<SendStates.RecipientState> {

    override val values: Sequence<SendStates.RecipientState>
        get() = sequenceOf(
            RecipientStatePreviewData.recipientState,
        )
}
// endregion