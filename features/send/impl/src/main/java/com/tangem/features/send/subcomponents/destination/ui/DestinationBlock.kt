package com.tangem.features.send.subcomponents.destination.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.account.AccountIcon
import com.tangem.core.ui.components.SecondaryButtonIconStart
import com.tangem.core.ui.components.account.AccountIconSize
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.test.SendConfirmScreenTestTags
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationRecipientListUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.impl.R
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun DestinationBlock(
    destinationUM: DestinationUM,
    isClickDisabled: Boolean,
    isEditingDisabled: Boolean,
    onClick: () -> Unit,
    showAddContact: Boolean = false,
    onAddContactClick: () -> Unit = {},
) {
    if (destinationUM !is DestinationUM.Content) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(TangemTheme.colors.background.action)
            .clickable(enabled = !isClickDisabled && !isEditingDisabled, onClick = onClick)
            .padding(TangemTheme.dimens.spacing12)
            .testTag(SendConfirmScreenTestTags.RECIPIENT_BLOCK),
    ) {
        AddressWithMemoBlock(
            address = destinationUM.addressTextField,
            memo = destinationUM.memoTextField,
        )
        if (showAddContact) {
            SecondaryButtonIconStart(
                text = stringResourceSafe(com.tangem.core.ui.R.string.address_book_add_contact),
                iconResId = com.tangem.core.ui.R.drawable.ic_plus_24,
                onClick = onAddContactClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = TangemTheme.dimens.spacing12),
            )
        }
    }
}

@Composable
private fun AddressWithMemoBlock(
    address: DestinationTextFieldUM.RecipientAddress,
    memo: DestinationTextFieldUM.RecipientMemo?,
) {
    Text(
        text = stringResourceSafe(R.string.send_recipient),
        style = TangemTheme.typography.subtitle2,
        color = TangemTheme.colors.text.tertiary,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing24),
        modifier = Modifier.padding(top = TangemTheme.dimens.spacing8),
    ) {
        val contactName = address.contactName
        val contactIcon = address.contactIcon
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contactName ?: address.value,
                style = TangemTheme.typography.body1,
                color = TangemTheme.colors.text.primary1,
                modifier = Modifier.testTag(SendConfirmScreenTestTags.RECIPIENT_ADDRESS),
            )
            val recipient = if (contactName != null) address.value else address.briefBlockchainAddress
            if (!recipient.isNullOrBlank()) {
                Text(
                    text = recipient,
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    modifier = Modifier.testTag(SendConfirmScreenTestTags.BLOCKCHAIN_ADDRESS),
                )
            }
        }
        if (contactName != null && contactIcon != null) {
            AccountIcon(
                name = stringReference(contactName),
                icon = contactIcon,
                size = AccountIconSize.Medium,
                modifier = Modifier.testTag(SendConfirmScreenTestTags.RECIPIENT_ADDRESS_ICON),
            )
        } else {
            IdentIcon(
                address = address.value,
                modifier = Modifier
                    .size(TangemTheme.dimens.size36)
                    .clip(RoundedCornerShape(TangemTheme.dimens.radius18))
                    .background(TangemTheme.colors.background.tertiary)
                    .testTag(SendConfirmScreenTestTags.RECIPIENT_ADDRESS_ICON),
            )
        }
    }

    if (memo != null && memo.value.isNotBlank()) {
        Text(
            text = stringResourceSafe(R.string.send_memo, memo.value),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.tertiary,
            modifier = Modifier
                .padding(top = TangemTheme.dimens.spacing8)
                .testTag(SendConfirmScreenTestTags.RECIPIENT_MEMO),
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
            label = resourceReference(R.string.send_recipient),
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
        isRecentHidden = false,
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
            previewItem.copy(
                addressTextField = previewItem.addressTextField.copy(
                    value = "vitalik.eth",
                    blockchainAddress = "0x34B4492A412D84A6E606288f3Bd714b89135D4dE",
                ),
            ),
        )
}