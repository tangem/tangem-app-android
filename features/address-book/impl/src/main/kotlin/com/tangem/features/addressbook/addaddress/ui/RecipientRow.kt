package com.tangem.features.addressbook.addaddress.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled
import com.tangem.core.ui.res.generated.icons.ic_scan_20
import com.tangem.features.addressbook.addaddress.ui.state.AddressFieldUM

@Composable
internal fun RecipientRow(
    addressField: AddressFieldUM,
    onValueChange: (String) -> Unit,
    onAddressClear: () -> Unit,
    onQrClick: () -> Unit,
    onPasteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            text = addressField.label.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = if (addressField.isError) {
                TangemTheme.colors3.text.status.error
            } else {
                TangemTheme.colors3.text.secondary
            },
        )
        TangemRow(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = TangemRowVerticalAlignment.Center,
            contentLead = TangemRowContentLead.Start,
            startSlot = {
                TangemIcon(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TangemTheme.colors3.bg.tertiary),
                    tangemIconUM = TangemIconUM.Ident(text = addressField.value),
                )
            },
            titleSlot = {
                SimpleTextField(
                    modifier = Modifier.weight(1f),
                    value = addressField.value,
                    onValueChange = onValueChange,
                    placeholder = addressField.placeholder,
                )
            },
            endSlot = {
                RecipientEndSlot(
                    hasValue = addressField.value.isNotEmpty(),
                    onAddressClear = onAddressClear,
                    onQrClick = onQrClick,
                    onPasteClick = onPasteClick,
                )
            },
        )
    }
}

@Composable
private fun RecipientEndSlot(
    hasValue: Boolean,
    onAddressClear: () -> Unit,
    onQrClick: () -> Unit,
    onPasteClick: () -> Unit,
) {
    if (hasValue) {
        Icon(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(onClick = onAddressClear),
            imageVector = Icons.ic_cross_circle_20_filled,
            tint = TangemTheme.colors3.icon.tertiary,
            contentDescription = null,
        )
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TangemButton(
                variant = TangemButton.Variant.Secondary,
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_scan_20),
                onClick = onQrClick,
            )
            TangemButton(
                text = TextReference.Res(id = R.string.common_paste),
                onClick = onPasteClick,
            )
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_RecipientRow() {
    TangemThemePreviewRedesign {
        Column {
            RecipientRow(
                addressField = AddressFieldUM(
                    value = "0xfb6916095ca1df60bb79ce92ce3ea74c37c5d359",
                    placeholder = resourceReference(R.string.address_book_enter_address),
                    label = resourceReference(R.string.common_address),
                ),
                onValueChange = {},
                onAddressClear = {},
                onQrClick = {},
                onPasteClick = {},
            )
            SpacerH12()
            RecipientRow(
                addressField = AddressFieldUM(
                    value = "",
                    placeholder = resourceReference(R.string.address_book_enter_address),
                    label = resourceReference(R.string.common_address),
                ),
                onValueChange = {},
                onAddressClear = {},
                onQrClick = {},
                onPasteClick = {},
            )
        }
    }
}