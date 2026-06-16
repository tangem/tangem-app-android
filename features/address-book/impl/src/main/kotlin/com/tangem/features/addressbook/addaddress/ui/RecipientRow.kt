package com.tangem.features.addressbook.addaddress.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.fields.SimpleTextField
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_circle_20_filled
import com.tangem.features.addressbook.addaddress.contract.AddressFieldUM

@Composable
internal fun RecipientRow(
    addressField: AddressFieldUM,
    onValueChange: (String) -> Unit,
    onAddressClear: () -> Unit,
    onQrClick: () -> Unit,
    onPasteClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp),
            text = stringResourceSafe(R.string.common_address),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors3.text.secondary,
        )
        TangemRow(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = TangemRowVerticalAlignment.Center,
            contentLead = TangemRowContentLead.Start,
            startSlot = {
                TangemIcon(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TangemTheme.colors3.bg.tertiary),
                    tangemIconUM = TangemIconUM.Ident(text = addressField.value),
                )
            },
            titleSlot = {
                SimpleTextField(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                    value = addressField.value,
                    onValueChange = onValueChange,
                    placeholder = TextReference.Res(R.string.address_book_enter_address),
                    singleLine = false,
                )
            },
            endSlot = {
                if (addressField.value.isNotEmpty()) {
                    Icon(
                        modifier = Modifier.clickable(onClick = onAddressClear),
                        imageVector = Icons.ic_cross_circle_20_filled,
                        tint = TangemTheme.colors3.icon.tertiary,
                        contentDescription = null,
                    )
                } else {
                    Row {
                        TangemButton(
                            variant = TangemButton.Variant.Secondary,
                            iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_qrcode_scaner_24),
                            onClick = onQrClick,
                        )
                        SpacerW8()
                        TangemButton(
                            variant = TangemButton.Variant.Primary,
                            text = TextReference.Res(id = R.string.common_paste),
                            onClick = onPasteClick,
                        )
                    }
                }
            },
        )
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