package com.tangem.tap.common.compose

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.tap.common.extensions.ValueCallback

/**
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> OutlinedSpinner(
    title: String,
    itemList: List<T>,
    selectedItem: T,
    onItemSelected: ValueCallback<T>,
    modifier: Modifier = Modifier,
    itemNameConverter: (T) -> String = { it.toString() },
    onClose: VoidCallback = {}
) {
    val rSelectedItem = remember { mutableStateOf(selectedItem) }
    val rIsExpanded = remember { mutableStateOf(false) }

    val onItemSelectedInternal: (T) -> Unit = {
        rSelectedItem.value = it
        rIsExpanded.value = false
        onItemSelected(it)
    }
    val onDismissRequest = {
        rIsExpanded.value = false
        onClose()
    }

    ExposedDropdownMenuBox(
        expanded = rIsExpanded.value,
        onExpandedChange = { rIsExpanded.value = !rIsExpanded.value },
    ) {
        OutlinedTextField(
            modifier = modifier,
            readOnly = true,
            value = itemNameConverter(rSelectedItem.value),
            onValueChange = {},
            label = { Text(title) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rIsExpanded.value) },
        )
        ExposedDropdownMenu(
            expanded = rIsExpanded.value,
            onDismissRequest = onDismissRequest,
        ) {
            itemList.forEach { item ->
                DropdownMenuItem(
                    onClick = { onItemSelectedInternal(item) }
                ) {
                    Text(itemNameConverter(item))
                }
            }
        }
    }
}

@Preview
@Composable
fun TestSpinnerPreview(){
    Scaffold() {
        OutlinedSpinner(
            title = "Blockchain name",
            itemList = listOf(Blockchain.values()),
            selectedItem = Blockchain.Avalanche,
            onItemSelected = {},
        )
    }
}
