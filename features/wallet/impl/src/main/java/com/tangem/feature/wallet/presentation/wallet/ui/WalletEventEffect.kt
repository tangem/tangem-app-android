package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.ui.utils.ReviewManagerRequester
import com.tangem.feature.wallet.presentation.wallet.ui.utils.animateScrollByIndex
import com.tangem.feature.wallet.presentation.wallet.ui.utils.demonstrateScrolling
import kotlinx.coroutines.delay

@Suppress("LongParameterList")
@Composable
internal fun WalletEventEffect(
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    event: StateEvent<WalletEvent>,
    selectedWalletIndex: Int,
    onAutoScrollSet: () -> Unit,
    onAlertConfigSet: (WalletAlertState) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalContext.current.resources
    val clipboardManager = LocalClipboardManager.current
    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is WalletEvent.ChangeWallet -> {
                    delay(timeMillis = 1000) // to let compose time to start drawing otherwise animation doesnt work
                    onAutoScrollSet()
                    walletsListState.animateScrollByIndex(prevIndex = selectedWalletIndex, newIndex = value.index)
                }
                is WalletEvent.ShowError -> {
                    snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
                }
                is WalletEvent.CopyAddress -> {
                    clipboardManager.setText(AnnotatedString(value.address))
                    snackbarHostState.showSnackbar(
                        message = resources.getString(R.string.wallet_notification_address_copied),
                        duration = SnackbarDuration.Short,
                    )
                }
                is WalletEvent.ShowAlert -> onAlertConfigSet(value.state)
                is WalletEvent.RateApp -> {
                    ReviewManagerRequester.request(context = context, onDismissClick = value.onDismissClick)
                }
                is WalletEvent.DemonstrateWalletsScrollPreview -> {
                    walletsListState.demonstrateScrolling(coroutineScope = coroutineScope, direction = value.direction)
                }
            }
        },
    )
}