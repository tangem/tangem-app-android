package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.features.tangempay.components.TangemPayWalletSelectorComponent
import com.tangem.features.tangempay.ui.WalletSelectorBSContentUM
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayWalletSelectorModel @Inject constructor(
    paramsContainer: ParamsContainer,
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    messageSender: UiMessageSender,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<TangemPayWalletSelectorComponent.Params>()

    private val userWalletsFetcher = userWalletsFetcherFactory.create(
        messageSender = messageSender,
        onlyMultiCurrency = true,
        isAuthMode = false,
        isClickableIfLocked = false,
        onWalletClick = { params.listener.onWalletSelected(it) },
    )

    init {
        fetchUserWalletsUM()
    }

    val uiState: StateFlow<WalletSelectorBSContentUM>
        field = MutableStateFlow(getInitialState())

    private fun getInitialState(): WalletSelectorBSContentUM {
        return WalletSelectorBSContentUM(
            userWallets = persistentListOf(),
            onDismiss = { params.listener.onWalletSelectorDismiss() },
        )
    }

    private fun fetchUserWalletsUM() {
        modelScope.launch {
            val eligibleWalletsIds = params.walletsIds.map { it.stringValue }.toSet()
            userWalletsFetcher.userWallets.collectLatest { userWalletsListUM ->
                uiState.update {
                    WalletSelectorBSContentUM(
                        userWallets = userWalletsListUM
                            .filter { wallet -> wallet.id in eligibleWalletsIds }
                            .toImmutableList(),
                        onDismiss = { params.listener.onWalletSelectorDismiss() },
                    )
                }
            }
        }
    }

    fun onDismiss() {
        params.listener.onWalletSelectorDismiss()
    }
}