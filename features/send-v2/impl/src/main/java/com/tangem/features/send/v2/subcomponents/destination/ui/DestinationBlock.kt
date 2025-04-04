package com.tangem.features.send.v2.subcomponents.destination.ui

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
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationTextFieldUM
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM

@Composable
internal fun DestinationBlock(
    destinationUM: DestinationUM,
    isClickDisabled: Boolean,
    isEditingDisabled: Boolean,
    onClick: () -> Unit,
) {
    val destinationUM = destinationUM as? DestinationUM.Content ?: return

    val backgroundColor = if (isEditingDisabled) {
        TangemTheme.colors.button.disabled
    } else {
        TangemTheme.colors.background.action
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(backgroundColor)
            .clickable(enabled = !isClickDisabled && !isEditingDisabled, onClick = onClick)
            .padding(TangemTheme.dimens.spacing12),
    ) {
        AddressBlock(destinationUM.addressTextField)
        MemoBlock(destinationUM.memoTextField)
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
        Text(
            text = address.value,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.primary1,
        )
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