package com.tangem.tap.common.compose

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.extensions.ValueCallback

/**
* [REDACTED_AUTHOR]
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun <T> OutlinedSpinner(
    modifier: Modifier = Modifier,
    label: String,
    itemList: List<T>,
    selectedItem: Field.Data<T>,
    onItemSelected: ValueCallback<T>,
    textFieldConverter: (T) -> String = { it.toString() },
    dropdownItemView: @Composable ((T) -> Unit)? = null,
    isEnabled: Boolean = true,
    onClose: VoidCallback = {}
) {
    val rIsExpanded = remember { mutableStateOf(false) }
    val rSelectedItem = remember { mutableStateOf(selectedItem.value) }
    if (!selectedItem.isUserInput) {
        rSelectedItem.value = selectedItem.value
    }

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
        ProvideTextStyle(value = TextStyle(color = Color.Blue)) {
            OutlinedTextField(
                modifier = modifier,
                readOnly = true,
                enabled = isEnabled,
                value = textFieldConverter(rSelectedItem.value),
                onValueChange = {},
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rIsExpanded.value) },
            )
        }
        ExposedDropdownMenu(
            expanded = rIsExpanded.value,
            onDismissRequest = onDismissRequest,
        ) {
            itemList.forEach { item ->
                DropdownMenuItem(onClick = { onItemSelectedInternal(item) }) {
                    when (dropdownItemView) {
                        null -> Text(textFieldConverter(item))
                        else -> dropdownItemView(item)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TestSpinnerPreview() {
    Scaffold() {
        OutlinedSpinner(
            label = "Blockchain name",
            itemList = listOf(Blockchain.values()),
            selectedItem = Field.Data(Blockchain.Avalanche),
            onItemSelected = {},
        )
    }
}

