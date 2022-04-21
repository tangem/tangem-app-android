package com.tangem.tap.common.compose

import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.extensions.VoidCallback
import com.tangem.domain.common.form.Field
import com.tangem.tap.common.extensions.ValueCallback
import timber.log.Timber

/**
[REDACTED_AUTHOR]
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
    val compositionCounter = remember { CompositionCounter(label) }
    val counter = compositionCounter.increase(label)

    val rIsExpanded = remember { mutableStateOf(false) }
    val stateSelectedItem = remember { mutableStateOf(selectedItem.value) }
    osLog(label, "recompose ------------------------------------", counter)
    osLog(label, "selectedItem: $selectedItem", counter)
    osLog(label, "stateSelectedItem: ${stateSelectedItem.value}", counter)
    if (!selectedItem.isUserInput) {
        osLog(label, "stateSelectedItem.value: update = selectedItem.value", counter)
        stateSelectedItem.value = selectedItem.value
    }
    osLog(label, "stateSelectedItem: ${stateSelectedItem.value}", counter)

    val onDropDownItemSelectedInternal: (T) -> Unit = {
        osLog(label, "onDropDownItemSelectedInternal: value: [${it.toString()}]", counter)
        stateSelectedItem.value = it
        osLog(label, "onDropDownItemSelectedInternal: stateSelectedItem: ${stateSelectedItem.value}", counter)
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
            enabled = isEnabled,
            value = textFieldConverter(stateSelectedItem.value),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rIsExpanded.value) },
        )

        if (!isEnabled) return@ExposedDropdownMenuBox
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

private fun osLog(id: String, log: String, counter: CompositionCounter) {
    if (id == "Сеть") Timber.d(
        "OutlinedSpinner[$id]:[${counter.count}] - $log"
    )
}

class CompositionCounter(
    val id: String,
    count: Int = 0
) {
    var count: Int = count
        private set

    fun increase(id: String): CompositionCounter {
        if (this.id != id) return this

        count += 1
        return CompositionCounter(id, count)
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
