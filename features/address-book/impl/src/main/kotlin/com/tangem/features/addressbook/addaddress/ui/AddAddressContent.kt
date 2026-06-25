package com.tangem.features.addressbook.addaddress.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.addaddress.ui.state.AddressFieldUM

@Composable
internal fun AddAddressContent(state: AddAddressUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemTopBar(
            title = resourceReference(R.string.address_book_add_address),
            startContent = {
                TangemButton(
                    iconStart = TangemIconUM.Icon(iconRes = R.drawable.ic_back_24),
                    onClick = state.onBackClick,
                    size = TangemButton.Size.X11,
                    variant = TangemButton.Variant.Material,
                )
            },
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            val minContentHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    modifier = Modifier
                        .heightIn(min = minContentHeight)
                        .imePadding(),
                ) {
                    RecipientRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        addressField = state.addressField,
                        onValueChange = state.onAddressChange,
                        onAddressClear = state.onAddressClear,
                        onQrClick = state.onQrClick,
                        onPasteClick = state.onPasteClick,
                    )
                    SpacerH(20.dp)
                    NetworkBlock(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .fillMaxWidth()
                            .background(color = TangemTheme.colors3.bg.secondary),
                        chosenNetworkStateUM = state.chosenNetworkStateUM,
                        onNetworkSelectClick = state.onNetworkClick,
                    )
                    PrimaryButton(state.buttonUM)
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.PrimaryButton(buttonUM: TangemButtonUM) {
    Spacer(modifier = Modifier.weight(1f))
    TangemButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        onClick = buttonUM.onClick,
        isEnabled = buttonUM.isEnabled,
        isLoading = buttonUM.isLoading,
        size = TangemButton.Size.X12,
        text = buttonUM.text,
    )
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_AddAddressContent() {
    TangemThemePreviewRedesign {
        AddAddressContent(
            state = AddAddressUM(
                addressField = AddressFieldUM(
                    value = "",
                    placeholder = resourceReference(R.string.address_book_enter_address),
                    label = resourceReference(R.string.common_address),
                ),
                buttonUM = TangemButtonUM(
                    text = TextReference.Res(R.string.address_book_add_address),
                    type = TangemButtonType.Primary,
                    isEnabled = false,
                    onClick = { },
                ),
                chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Empty,
                onAddressChange = {},
                onAddressClear = {},
                onPasteClick = {},
                onQrClick = {},
                onBackClick = {},
                onNetworkClick = {},
            ),
        )
    }
}