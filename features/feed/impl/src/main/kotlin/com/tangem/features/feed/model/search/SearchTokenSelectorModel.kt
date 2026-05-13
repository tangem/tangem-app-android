package com.tangem.features.feed.model.search

import androidx.compose.runtime.Stable
import com.tangem.common.ui.markets.tokenselector.TokenSelectorContentUM
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.feed.components.search.SearchTokenSelectorComponent
import com.tangem.features.feed.model.search.state.TokenSelectorStateController
import com.tangem.features.feed.model.search.state.transformers.BuildTokenSelectorSectionsTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class SearchTokenSelectorModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    userWalletsListRepository: UserWalletsListRepository,
    private val stateController: TokenSelectorStateController,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
) : Model() {

    private val params = paramsContainer.require<SearchTokenSelectorComponent.Params>()

    val state: StateFlow<TokenSelectorContentUM>
        get() = stateController.uiState

    init {
        val requiredWalletIds = params.entries.map { it.userWalletId }.toSet()
        val walletIconsFlow = userWalletsListRepository.userWallets
            .filterNotNull()
            .map { wallets ->
                wallets
                    .filter { it.walletId in requiredWalletIds }
                    .associate { wallet ->
                        wallet.walletId to walletIconUMConverter.convert(getWalletIconUseCase(wallet))
                    }
            }

        modelScope.launch(dispatchers.default) {
            combine(
                walletIconsFlow,
                getBalanceHidingSettingsUseCase.isBalanceHidden(),
                ::Pair,
            ).collect { (walletIcons, isBalanceHidden) ->
                rebuildSections(isBalanceHidden, walletIcons)
            }
        }
    }

    private fun rebuildSections(isBalanceHidden: Boolean, walletIcons: Map<UserWalletId, DeviceIconUM>) {
        stateController.update(
            BuildTokenSelectorSectionsTransformer(
                entries = params.entries,
                appCurrency = params.appCurrency,
                isBalanceHidden = isBalanceHidden,
                walletIcons = walletIcons,
                onTokenSelected = params.onTokenSelected,
            ),
        )
    }
}