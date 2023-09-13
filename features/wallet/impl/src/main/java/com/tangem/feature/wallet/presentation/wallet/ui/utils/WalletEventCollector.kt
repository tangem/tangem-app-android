package com.tangem.feature.wallet.presentation.wallet.ui.utils

import android.content.res.Resources
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector

internal class WalletEventCollector(
    private val walletsListState: LazyListState,
    private val snackbarHostState: SnackbarHostState,
    private val resources: Resources,
    private val onAutoScrollHappened: () -> Unit,
) : FlowCollector<WalletEvent?> {

    override suspend fun emit(value: WalletEvent?) {
        when (value) {
            is WalletEvent.ChangeWallet -> {
                onAutoScrollHappened()
                delay(timeMillis = 800)
                walletsListState.animateScrollToItem(index = value.index)
            }
            is WalletEvent.ShowError -> {
                snackbarHostState.showSnackbar(message = value.text.resolveReference(resources))
            }
            null -> Unit
        }
    }
}
