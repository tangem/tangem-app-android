package com.tangem.tap.common.compose

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.Field

@Composable
fun BlockchainSpinner(
    @StringRes title: Int,
    itemList: List<Blockchain>,
    selectedItem: Field.Data<Blockchain>,
    isEnabled: Boolean = true,
    textFieldConverter: (Blockchain) -> String,
    dropdownItemView: @Composable ((Blockchain) -> Unit)? = null,
    closePopupTrigger: ClosePopupTrigger = ClosePopupTrigger(),
    onItemSelect: (Blockchain) -> Unit,
) {
    OutlinedSpinner(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(id = title),
        itemList = itemList,
        selectedItem = selectedItem,
        textFieldConverter = textFieldConverter,
        dropdownItemView = dropdownItemView,
        isEnabled = isEnabled,
        onItemSelected = onItemSelect,
        closePopupTrigger = closePopupTrigger,
    )
}
