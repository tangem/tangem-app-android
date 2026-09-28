package com.tangem.feature.tester.presentation.walletids.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.hashedStringValue
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.feature.tester.presentation.navigation.InnerTesterRouter
import com.tangem.feature.tester.presentation.walletids.state.WalletIdsScreenUM
import com.tangem.feature.tester.presentation.walletids.state.WalletIdsUM
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
internal class WalletIdsViewModel @Inject constructor(
    getWalletsUseCase: GetWalletsUseCase,
    private val clipboardManager: ClipboardManager,
) : ViewModel() {

    val uiState: StateFlow<WalletIdsScreenUM>
        field = MutableStateFlow(
            WalletIdsScreenUM(
                wallets = persistentListOf(),
                onBackClick = {},
            ),
        )

    init {
        getWalletsUseCase()
            .onEach { wallets ->
                uiState.update { state -> state.copy(wallets = wallets.map(::toWalletIdsUM).toImmutableList()) }
            }
            .launchIn(viewModelScope)
    }

    fun setupNavigation(router: InnerTesterRouter) {
        uiState.update { state -> state.copy(onBackClick = router::back) }
    }

    private fun toWalletIdsUM(wallet: UserWallet): WalletIdsUM {
        val id = wallet.walletId.stringValue
        val hash = wallet.walletId.hashedStringValue

        return WalletIdsUM(
            name = wallet.name,
            id = id,
            hash = hash,
            onCopyIdClick = { copy(id) },
            onCopyHashClick = { copy(hash) },
        )
    }

    private fun copy(value: String) {
        clipboardManager.setText(text = value, isSensitive = false)
    }
}