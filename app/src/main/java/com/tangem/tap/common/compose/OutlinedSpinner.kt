package com.tangem.tap.common.compose

import android.os.Handler
import android.os.Looper
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.os.postDelayed
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.extensions.ValueCallback

/**
 * Created by Anton Zhilenkov on 23/03/2022.
 */
@Suppress("MagicNumber")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> OutlinedSpinner(
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
            ExposedDropdownMenu(
                expanded = rIsExpanded.value,
                onDismissRequest = onDismissRequest,
            ) {
                itemList.forEach { item ->
                    DropdownMenuItem(onClick = { onDropDownItemSelectedInternal(item) }) {
                        when (dropdownItemView) {
                            null -> Text(textFieldConverter(item))
                            else -> dropdownItemView(item)
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
    var isTriggered = false
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
