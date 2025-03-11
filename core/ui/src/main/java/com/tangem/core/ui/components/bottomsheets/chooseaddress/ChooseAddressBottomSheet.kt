package com.tangem.core.ui.components.bottomsheets.chooseaddress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SimpleSettingsRow
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme

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