package com.tangem.tap.features.customtoken.legacy.compose

import android.os.Handler
import android.os.Looper
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.postDelayed
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.compose.TangemTextFieldsDefault
import com.tangem.tap.common.extensions.ValueCallback

/**
[REDACTED_AUTHOR]
 */
@Suppress("MagicNumber")
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun <T> OutlinedSpinner(
    label: String,
    itemList: List<T>,
    selectedItem: Field.Data<T>,
    onItemSelected: ValueCallback<T>,
    modifier: Modifier = Modifier,
    textFieldConverter: (T) -> String = { it.toString() },
    dropdownItemView: @Composable ((T) -> Unit)? = null,
    isEnabled: Boolean = true,
    onClose: VoidCallback = {},
    closePopupTrigger: ClosePopupTrigger = ClosePopupTrigger(),
) {
    val rIsExpanded = remember { mutableStateOf(false) }
    val stateSelectedItem = remember { mutableStateOf(selectedItem.value) }
    if (!selectedItem.isUserInput) {
        stateSelectedItem.value = selectedItem.value
    }

    val onDropDownItemSelectedInternal: (T) -> Unit = {
        stateSelectedItem.value = it
        rIsExpanded.value = false
        onItemSelected(it)
    }
    val onDismissRequest = {
        rIsExpanded.value = false
        onClose()
    }

    closePopupTrigger.close = {
        onDismissRequest()
        Handler(Looper.getMainLooper()).postDelayed(100) {
            closePopupTrigger.onCloseComplete()
        }
    }

    ExposedDropdownMenuBox(
        expanded = rIsExpanded.value,
        onExpandedChange = { rIsExpanded.value = !rIsExpanded.value },
    ) {
        OutlinedTextField(
            modifier = modifier,
            readOnly = true,
            enabled = isEnabled,
            value = textFieldConverter(stateSelectedItem.value),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rIsExpanded.value) },
            colors = TangemTextFieldsDefault.defaultTextFieldColors,
        )

        if (isEnabled) {
            ExposedDropdownMenu(expanded = rIsExpanded.value, onDismissRequest = onDismissRequest) {
                itemList.forEach { item ->
                    key(item) {
                        DropdownMenuItem(onClick = { onDropDownItemSelectedInternal(item) }) {
                            if (dropdownItemView == null) Text(textFieldConverter(item)) else dropdownItemView(item)
                        }
                    }
                }
            }
        }
    }
}

class ClosePopupTrigger {
    var close: () -> Unit = {}
    var onCloseComplete: () -> Unit = {}
}

@Preview
@Composable
private fun TestSpinnerPreview() {
    OutlinedSpinner(
        label = "Blockchain name",
        itemList = listOf(Blockchain.values()),
        selectedItem = Field.Data(Blockchain.Avalanche, false),
        onItemSelected = {},
    )
}