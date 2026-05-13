package com.tangem.features.feed.model.search

import androidx.compose.runtime.Stable
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.feed.components.search.SearchTokenSelectorComponent
import com.tangem.features.feed.model.search.state.TokenSelectorStateController
import com.tangem.features.feed.model.search.state.transformers.BuildTokenSelectorSectionsTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class SearchTokenSelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val stateController: TokenSelectorStateController,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) : Model() {

    private val params = paramsContainer.require<SearchTokenSelectorComponent.Params>()

    val state: StateFlow<TokenSelectorContentUM>
        get() = stateController.uiState

    init {
        modelScope.launch(dispatchers.default) {
            val requiredWalletIds = params.entries.map { it.userWalletId }.toSet()
            val walletIcons = userWalletsListRepository.userWallets
                .filterNotNull()
                .first()
                .filter { it.walletId in requiredWalletIds }
                .associate { wallet ->
                    wallet.walletId to walletIconUMConverter.convert(getWalletIconUseCase(wallet))
                }

            stateController.update(
                BuildTokenSelectorSectionsTransformer(
                    entries = params.entries,
                    appCurrency = params.appCurrency,
                    isBalanceHidden = params.isBalanceHidden,
                    walletIcons = walletIcons,
                    onTokenSelected = params.onTokenSelected,
                ),
            )
        }
    }
}