package com.tangem.features.walletconnect.connections.model

import androidx.compose.runtime.Stable
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.wallet.utils.UserWalletsFetcher
import com.tangem.features.walletconnect.connections.components.WcSelectWalletComponent.WcSelectWalletParams
import com.tangem.features.walletconnect.connections.entity.WcAppInfoWalletUM
import com.tangem.features.walletconnect.connections.utils.WcUserWalletsFetcher
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class WcSelectWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    messageSender: UiMessageSender,
    userWalletsFetcherFactory: UserWalletsFetcher.Factory,
    getTokenListUseCase: GetTokenListUseCase,
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<WcSelectWalletParams>()

    internal val state: StateFlow<WcAppInfoWalletUM>
    field = MutableStateFlow<WcAppInfoWalletUM>(
        WcAppInfoWalletUM(
            wallets = persistentListOf(),
            selectedUserWalletId = params.selectedWalletId,
        ),
    )

    private val userWalletsFetcher = WcUserWalletsFetcher(
        userWalletsFetcherFactory = userWalletsFetcherFactory,
        getTokenListUseCase = getTokenListUseCase,
        messageSender = messageSender,
        onWalletSelected = ::onWalletSelected,
    )

    init {
        userWalletsFetcher
            .userWallets
            .onEach { state.update { state -> state.copy(wallets = it) } }
            .launchIn(modelScope)
    }

    private fun onWalletSelected(userWalletId: UserWalletId) {
        params.callback.onWalletSelected(userWalletId)
        router.pop()
    }
}