package com.tangem.features.addressbook.addaddress.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.tangem.core.ui.components.fields.SimpleTextField
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
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM

@Composable
internal fun MemoRow(memoField: AddAddressUM.MemoFieldUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .background(TangemTheme.colors3.bg.secondary),
    ) {
        Text(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
            text = memoField.label.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = if (memoField.isError) {
                TangemTheme.colors3.text.status.error
            } else {
                TangemTheme.colors3.text.secondary
            },
        )
        TangemRow(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = TangemRowVerticalAlignment.Center,
            contentLead = TangemRowContentLead.Start,
            titleSlot = {
                SimpleTextField(
                    modifier = Modifier.weight(1f),
                    value = memoField.value,
                    onValueChange = memoField.onValueChange,
                    placeholder = resourceReference(R.string.send_optional_field),
                )
            },
            endSlot = {
                if (memoField.value.isNotEmpty()) {
                    Icon(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable(onClick = { memoField.onValueChange("") }),
                        imageVector = Icons.ic_cross_circle_20_filled,
                        tint = TangemTheme.colors3.icon.tertiary,
                        contentDescription = null,
                    )
                } else {
                    TangemButton(
                        size = TangemButton.Size.X9,
                        text = TextReference.Res(id = R.string.common_paste),
                        onClick = memoField.onPasteClick,
                    )
                }
            },
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_MemoRow() {
    TangemThemePreviewRedesign {
        MemoRow(
            memoField = AddAddressUM.MemoFieldUM(
                isVisible = true,
                value = "123456",
                label = resourceReference(R.string.send_destination_tag_field),
                isError = false,
                onValueChange = {},
                onPasteClick = {},
            ),
        )
    }
}