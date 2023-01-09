package com.tangem.tap.common.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tangem.core.ui.components.SpacerH16
import com.tangem.domain.DomainDialog
import com.tangem.domain.redux.domainStore
import com.tangem.domain.redux.global.DomainGlobalAction
import com.tangem.domain.redux.global.DomainGlobalState
import com.tangem.tap.domain.moduleMessage.ModuleMessageConverter
import com.tangem.tap.features.tokens.addCustomToken.compose.SelectTokenNetworkDialog
import com.tangem.wallet.R
import org.rekotlin.StoreSubscriber

@Composable
fun ComposeDialogManager() {
    val dialogSate = remember { mutableStateOf<DomainDialog?>(null) }
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
private fun ShowTheDialog(dialogState: MutableState<DomainDialog?>) {
    if (dialogState.value == null) return

    val context = LocalContext.current
    val errorConverter = remember { ModuleMessageConverter(context) }
    val onDismissRequest = { domainStore.dispatch(DomainGlobalAction.ShowDialog(null)) }

    when (val dialog = dialogState.value) {
        is DomainDialog.DialogError -> ErrorDialog(
            title = stringResource(id = R.string.common_error),
            body = errorConverter.convert(dialog.error).message,
            onDismissRequest
        )
        is DomainDialog.SelectTokenDialog -> SelectTokenNetworkDialog(dialog, onDismissRequest)
    }
}

/**
 * Dialog with single item selection
 */
@Composable
fun <T> SimpleDialog(
    title: String,
    items: List<T>,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    Dialog(
        properties = DialogProperties(false, false),
        onDismissRequest = { }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                DialogTitle(title = title)
                LazyColumn {
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
                        ) { itemContent(item) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogTitle(title: String) {
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
}

@Composable
fun ErrorDialog(
    title: String,
    body: String,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        title = { DialogTitle(title) },
        text = { Text(body) },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(onClick = onDismissRequest) {
                Text(text = stringResource(id = R.string.common_ok))
            }
        }
    )
}
