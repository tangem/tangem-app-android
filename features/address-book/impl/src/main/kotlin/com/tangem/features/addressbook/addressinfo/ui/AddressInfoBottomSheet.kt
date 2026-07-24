package com.tangem.features.addressbook.addressinfo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetType
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_copy_20
import com.tangem.features.addressbook.addressinfo.ui.state.AddressInfoUM
import com.tangem.features.addressbook.impl.R

@Composable
internal fun AddressInfoBottomSheet(state: AddressInfoUM, onDismiss: () -> Unit) {
    TangemBottomSheet<TangemBottomSheetConfigContent.Empty>(
        config = TangemBottomSheetConfig(
            isShown = true,
            onDismissRequest = onDismiss,
            content = TangemBottomSheetConfigContent.Empty,
        ),
        type = TangemBottomSheetType.Modal,
        containerColor = TangemTheme.colors3.bg.secondary,
        title = {
            TangemTopNavigation(
                windowInsets = WindowInsets(0),
                blurBackground = false,
                endButton = { TangemButton.Close(onClick = onDismiss) },
            )
        },
        content = {
            AddressInfoContent(
                address = state.address,
                networkCount = state.networkCount,
                onCopy = state.onCopy,
                onEditAddress = state.onEditAddress,
                onDeleteAddress = state.onDeleteAddress,
            )
        },
    )
}

@Suppress("LongParameterList")
@Composable
private fun AddressInfoContent(
    address: String,
    networkCount: Int,
    onCopy: () -> Unit,
    onEditAddress: () -> Unit,
    onDeleteAddress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AddressHeader(address = address, networkCount = networkCount)
        SpacerH(8.dp)
        TangemButton(
            onClick = onCopy,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_copy_20),
            text = resourceReference(R.string.common_copy_address),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X9,
        )
        SpacerH(48.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.opaque.primary)
                .clickableSingle(onClick = onDeleteAddress),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier.padding(vertical = 14.dp),
                text = stringResourceSafe(R.string.address_book_remove_address),
                style = TangemTheme.typography3.body.medium,
                color = TangemTheme.colors3.text.status.error,
            )
        }
        SpacerH(8.dp)
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onEditAddress,
            text = resourceReference(R.string.address_book_edit_address),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
        )
    }
}

@Composable
private fun AddressHeader(address: String, networkCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemIcon(
            tangemIconUM = TangemIconUM.Ident(text = address),
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape),
        )
        SpacerH(32.dp)
        Text(
            text = address,
            style = TangemTheme.typography3.heading.small,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.MiddleEllipsis,
            maxLines = 1,
        )
        SpacerH(8.dp)
        Text(
            text = pluralReference(
                id = R.plurals.common_networks_count,
                count = networkCount,
                formatArgs = wrappedList(networkCount),
            ).resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.secondary,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun Preview_AddressInfoContent() {
    TangemThemePreviewRedesign {
        AddressInfoContent(
            address = "0x7a250d5630B4cF539739dF2C5dAcb4c659F2488D",
            networkCount = 3,
            onCopy = {},
            onEditAddress = {},
            onDeleteAddress = {},
        )
    }
}