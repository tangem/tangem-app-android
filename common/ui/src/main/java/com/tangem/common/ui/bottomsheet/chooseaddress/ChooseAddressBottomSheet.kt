package com.tangem.common.ui.bottomsheet.chooseaddress

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.bottomsheet.receive.AddressModel
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import kotlinx.collections.immutable.persistentListOf

@Composable
fun ChooseAddressBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet(config) { content: ChooseAddressBottomSheetConfig ->
        ChooseAddressBottomSheetContent(content = content)
    }
}

@Composable
private fun ChooseAddressBottomSheetContent(content: ChooseAddressBottomSheetConfig) {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
    ) {
        content.addressModels.forEach { addressModel ->
            SimpleSettingsRow(
                title = addressModel.displayName.resolveReference(),
                icon = R.drawable.ic_arrow_top_right_24,
                onItemsClick = { content.onClick(addressModel) },
            )
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ChooseAddressBottomSheet_Preview(
    @PreviewParameter(ChooseAddressBottomSheetPreviewProvider::class) params: ChooseAddressBottomSheetConfig,
) {
    TangemThemePreview {
        ChooseAddressBottomSheetContent(params)
    }
}

private class ChooseAddressBottomSheetPreviewProvider : PreviewParameterProvider<ChooseAddressBottomSheetConfig> {
    override val values: Sequence<ChooseAddressBottomSheetConfig>
        get() = sequenceOf(
            ChooseAddressBottomSheetConfig(
                addressModels = persistentListOf(
                    AddressModel(
                        displayName = stringReference("Main address"),
                        fullName = stringReference("Bitcoin"),
                        value = "0x1234...abcd",
                        type = AddressModel.Type.Default,
                    ),
                    AddressModel(
                        displayName = stringReference("Legacy address"),
                        fullName = stringReference("Bitcoin"),
                        value = "0x1234...abcd",
                        type = AddressModel.Type.Default,
                    ),
                ),
                onClick = {},
            ),
        )
}
// endregion