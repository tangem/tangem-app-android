package com.tangem.tap.common.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tangem.domain.DomainDialog
import com.tangem.domain.DomainStateDialog
import com.tangem.domain.redux.domainStore
import com.tangem.domain.redux.global.DomainGlobalAction
import com.tangem.domain.redux.global.DomainGlobalState
import org.rekotlin.StoreSubscriber

@Composable
fun ComposeDialogManager() {
    val dialogSate = remember { mutableStateOf<DomainStateDialog?>(null) }
    val subscriber = remember {
        object : StoreSubscriber<DomainGlobalState> {
            override fun newState(state: DomainGlobalState) {
                dialogSate.value = state.dialog
            }
        }
    }

    ShowTheDialog(dialogSate)

    LaunchedEffect(key1 = Unit, block = {
        domainStore.subscribe(subscriber) { state ->
            state.skipRepeats { oldState, newState ->
                oldState.globalState == newState.globalState
            }.select { it.globalState }
        }
    })
    DisposableEffect(key1 = Unit, effect = {
        onDispose { domainStore.unsubscribe(subscriber) }
    })
}

@Composable
fun ShowTheDialog(dialogState: MutableState<DomainStateDialog?>) {
    if (dialogState.value == null) return

    val onDismissRequest = { domainStore.dispatch(DomainGlobalAction.ShowDialog(null)) }

    when (val dialog = dialogState.value) {
        is DomainDialog.SelectTokenDialog -> {
            SimpleDialog(
                title = "Select a token",
                items = dialog.items,
                itemNameConverter = dialog.itemNameConverter,
                onSelect = dialog.onSelect,
                onDismissRequest = onDismissRequest
            )
        }
    }
}

/**
 * Dialog with single item selection
 */
@Composable
fun <T> SimpleDialog(
    title: String,
    items: List<T>,
    itemNameConverter: (T) -> String,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    Dialog(
        properties = DialogProperties(false, false),
        onDismissRequest = { }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = title,
                    style = LocalTextStyle.provides(
                        TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    ).value
                )

                SpacerH16()
                LazyColumn() {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable {
                                    onSelect(item)
                                    onDismissRequest()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = itemNameConverter(item),
                            )
                        }
                    }
                }
            }
        }
    }
}