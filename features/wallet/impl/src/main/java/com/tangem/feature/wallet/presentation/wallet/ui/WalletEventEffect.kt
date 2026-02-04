package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.utils.requestPermission
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.ui.utils.animateScrollByIndex
import com.tangem.feature.wallet.presentation.wallet.ui.utils.demonstrateScrolling
import com.tangem.features.pushnotifications.api.utils.PUSH_PERMISSION

@Composable
internal fun WalletEventEffect(
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    event: StateEvent<WalletEvent>,
    onAutoScrollSet: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val resources = LocalContext.current.resources

    var showPermissionRequest by remember { mutableStateOf<Pair<() -> Unit, () -> Unit>?>(null) }
    HandlePermissionRequest(
        permissionRequestParams = showPermissionRequest,
        onPermissionRequestResult = { showPermissionRequest = null },
    )

    EventEffect(
        event = event,
        onTrigger = { value ->
            when (value) {
                is WalletEvent.ChangeWallet -> {
                    onAutoScrollSet()
                    walletsListState.animateScrollByIndex(prevIndex = value.prevIndex, newIndex = value.newIndex)
                }
                is WalletEvent.ChangeWalletWithoutScroll -> {
                    onAutoScrollSet()
                    walletsListState.scrollToItem(value.newIndex)
                }
                is WalletEvent.ShowError -> {
                    snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
                }
                is WalletEvent.CopyAddress -> {
                    snackbarHostState.showSnackbar(
                        message = resources.getStringSafe(R.string.wallet_notification_address_copied),
                        duration = SnackbarDuration.Short,
                    )
                }
                is WalletEvent.DemonstrateWalletsScrollPreview -> {
                    walletsListState.demonstrateScrolling(coroutineScope = coroutineScope, direction = value.direction)
                }
                is WalletEvent.RequestPushPermissions -> {
                    showPermissionRequest = value.onAllow to value.onDeny
                }
            }
        },
    )
}

@Composable
private fun HandlePermissionRequest(
    permissionRequestParams: Pair<() -> Unit, () -> Unit>?,
    onPermissionRequestResult: () -> Unit,
) {
    val actualPermissionLauncher = permissionRequestParams?.let { (onAllowExternal, onDenyExternal) ->
        requestPermission(
            onAllow = {
                onAllowExternal()
                onPermissionRequestResult()
            },
            onDeny = {
                onDenyExternal()
                onPermissionRequestResult()
            },
            permission = PUSH_PERMISSION,
        )
    }

    LaunchedEffect(permissionRequestParams) {
        actualPermissionLauncher?.invoke()
    }
}