package com.tangem.feature.wallet.presentation.wallet.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.tangem.feature.wallet.presentation.wallet.ui.utils.WalletEventCollector
import com.tangem.feature.wallet.presentation.wallet.viewmodels.WalletViewModel

@Composable
internal fun WalletEventEffect(
    viewModel: WalletViewModel,
    walletsListState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onAutoScrollSet: () -> Unit,
) {
    val resources = LocalContext.current.resources

    LaunchedEffect(key1 = Unit) {
        viewModel.event.collect(
            collector = WalletEventCollector(
                walletsListState = walletsListState,
                snackbarHostState = snackbarHostState,
                resources = resources,
                onAutoScrollHappened = onAutoScrollSet,
            ),
        )
    }
}
