package com.tangem.features.tangempay.eligibility.wallet

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.features.tangempay.eligibility.TangemPayWalletSelectorComponent
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

    val uiState: StateFlow<TangemPayWalletSelectorUM>
        field = MutableStateFlow(getInitialState())

    init {
        fetchUserWallets()
    }

    fun onDismiss() {
        params.listener.onWalletSelectorDismiss()
    }

    private fun fetchUserWallets() {
        modelScope.launch {
            val eligibleWalletsIds = params.walletsIds.map { it.stringValue }.toSet()
            userWalletsFetcher.userWallets.collectLatest { userWallets ->
                uiState.update {
                    TangemPayWalletSelectorUM(
                        userWallets = userWallets
                            .filter { wallet -> wallet.id in eligibleWalletsIds }
                            .toImmutableList(),
                        onDismiss = ::onDismiss,
                    )
                }
            }
        }
    }

    private fun getInitialState(): TangemPayWalletSelectorUM {
        return TangemPayWalletSelectorUM(
            userWallets = persistentListOf(),
            onDismiss = ::onDismiss,
        )
    }
}