package com.tangem.features.send.v2.subcomponents.destination.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationRecipientListUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun DestinationBlock(
    destinationUM: DestinationUM,
    isClickDisabled: Boolean,
    isEditingDisabled: Boolean,
    isRedesignEnabled: Boolean,
    onClick: () -> Unit,
) {
    if (destinationUM !is DestinationUM.Content) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isClickDisabled && !isEditingDisabled, onClick = onClick)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        if (isRedesignEnabled) {
            AddressWithMemoBlock(
                address = destinationUM.addressTextField,
                memo = destinationUM.memoTextField,
            )
        } else {
            AddressBlock(destinationUM.addressTextField)
            MemoBlock(destinationUM.memoTextField)
        }
    }
}

@Composable
private fun AddressBlock(address: DestinationTextFieldUM.RecipientAddress) {
    Text(
        text = address.label.resolveReference(),
        style = TangemTheme.typography.subtitle2,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = address.value,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            val blockchainAddress = address.briefBlockchainAddress
            if (!blockchainAddress.isNullOrBlank()) {
                Text(
                    text = blockchainAddress,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Composable
private fun MemoBlock(memo: DestinationTextFieldUM.RecipientMemo?) {
    if (memo != null && memo.value.isNotBlank()) {
        HorizontalDivider(
            color = TangemTheme.colors.icon.inactive,
            modifier = Modifier.padding(vertical = TangemTheme.dimens.spacing12),
        )
        Text(
            text = memo.label.resolveReference(),
            style = TangemTheme.typography.subtitle2,
            color = TangemTheme.colors.text.secondary,
        )
        Text(
            text = memo.value,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
        )
    }
}

@Composable
private fun AddressWithMemoBlock(
    address: DestinationTextFieldUM.RecipientAddress,
    memo: DestinationTextFieldUM.RecipientMemo?,
) {
    Text(
        text = stringResourceSafe(R.string.send_to_address),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = address.value,
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.primary1,
            )
            val blockchainAddress = address.briefBlockchainAddress
            if (!blockchainAddress.isNullOrBlank()) {
                Text(
                    text = blockchainAddress,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
        IdentIcon(
            address = address.value,
            modifier = Modifier
                .size(TangemTheme.dimens.size36)
                .clip(RoundedCornerShape(TangemTheme.dimens.radius18))
                .background(TangemTheme.colors.background.tertiary),
        )
    }

    if (memo != null && memo.value.isNotBlank()) {
        Text(
            text = stringResourceSafe(R.string.send_memo, memo.value),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DestinationBlockPreview(
    @PreviewParameter(DestinationBlockPreviewProvider::class) state: DestinationUM.Content,
) {
    TangemThemePreview {
        DestinationBlock(
            destinationUM = state,
            isClickDisabled = false,
            isEditingDisabled = false,
            isRedesignEnabled = state.isRedesignEnabled,
            onClick = {},
        )
    }
}

private class DestinationBlockPreviewProvider : PreviewParameterProvider<DestinationUM.Content> {
    val previewItem = DestinationUM.Content(
        isPrimaryButtonEnabled = true,
        addressTextField = DestinationTextFieldUM.RecipientAddress(
            value = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
            keyboardOptions = KeyboardOptions.Default,
            placeholder = TextReference.Str("Enter address"),
            label = TextReference.Str("Recipient Address"),
            isError = false,
            error = null,
            isValuePasted = false,
        ),
        memoTextField = DestinationTextFieldUM.RecipientMemo(
            value = "Test memo for transaction",
            keyboardOptions = KeyboardOptions.Default,
            placeholder = TextReference.Str("Enter memo (optional)"),
            label = TextReference.Str("Memo"),
            isError = false,
            error = null,
            disabledText = TextReference.Str("Memo disabled"),
            isEnabled = true,
            isValuePasted = false,
        ),
        recent = emptyList<DestinationRecipientListUM>().toImmutableList(),
        wallets = emptyList<DestinationRecipientListUM>().toImmutableList(),
        networkName = "Ethereum",
        isValidating = false,
        isInitialized = true,
        isRedesignEnabled = false,
    )

    override val values: Sequence<DestinationUM.Content>
        get() = sequenceOf(
            previewItem,
            previewItem.copy(
                addressTextField = previewItem.addressTextField.copy(
                    value = "vitalik.eth",
                    blockchainAddress = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
                ),
            ),
            previewItem.copy(isRedesignEnabled = true),
            previewItem.copy(
                addressTextField = previewItem.addressTextField.copy(
                    value = "vitalik.eth",
                    blockchainAddress = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
                ),
                isRedesignEnabled = true,
            ),
        )
}