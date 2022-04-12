package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.tangem.blockchain.common.Blockchain
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.compose.Button
import com.tangem.tap.common.compose.OutlinedSpinner
import com.tangem.tap.common.extensions.ValueCallback
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
fun TokenNetworkSpinner(
    title: Int,
    itemList: List<Blockchain>,
    selectedItem: Field.Data<Blockchain>,
    isEnabled: Boolean = true,
    itemNameConverter: (Blockchain) -> String,
    onItemSelected: ValueCallback<Blockchain>,
) {

    OutlinedSpinner(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(id = title),
        itemList = itemList,
        selectedItem = selectedItem,
        itemNameConverter = itemNameConverter,
        isEnabled = isEnabled,
        onItemSelected = onItemSelected
    )
}

@Composable
fun AddButton(
    modifier: Modifier = Modifier,
    isEnabled: Boolean,
    textId: Int = R.string.common_add,
    onClick: () -> Unit,
) {
    Button(
        textId = textId,
        isEnabled = isEnabled,
        modifier = modifier
            .height(52.dp)
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        leadingView = {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add",
            )
        },
        onClick = onClick
    )
}