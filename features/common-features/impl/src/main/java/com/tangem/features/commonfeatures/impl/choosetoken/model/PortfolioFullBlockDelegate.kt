package com.tangem.features.commonfeatures.impl.choosetoken.model

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.usecase.GetSelectedWalletUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery
import com.tangem.features.commonfeatures.api.choosetoken.ChooseTokenBridgeInternal.SearchQuery.Companion.isSearchingState
import com.tangem.features.commonfeatures.api.choosetoken.model.ChooseTokenPortfolioFullBlockUM
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletTabUM
import com.tangem.features.commonfeatures.impl.choosetoken.SettingContextUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class PortfolioFullBlockDelegate @AssistedInject constructor(
    private val settingContextUseCase: SettingContextUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    private val selectedWalletUseCase: GetSelectedWalletUseCase,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val portfolioListBlockDelegate: PortfolioListBlockDelegate,
    @Assisted private val searchQueryState: StateFlow<SearchQuery>,
) {

    private val isSearchingState: Boolean get() = searchQueryState.isSearchingState
    private val onWalletSelected = Channel<UserWalletId>(capacity = Channel.BUFFERED)

    val selectedWalletFlow: SharedFlow<UserWallet> = onWalletSelected.receiveAsFlow()
        .distinctUntilChanged()
        .mapNotNull { walletId -> getUserWalletUseCase.invoke(walletId).getOrNull() }
        .shareIn(modelScope, SharingStarted.Eagerly, replay = 1)

    val fullPortfolioBlock: StateFlow<ChooseTokenPortfolioFullBlockUM?> = buildFlow()
        .flowOn(dispatchers.default)
        .stateIn(modelScope, SharingStarted.Eagerly, initialValue = null)

    init {
        val globalSelectedWallet = selectedWalletUseCase.sync().getOrNull()
        val allWallets = getWalletsUseCase.invokeSync().filter { it.isMultiCurrency }
        val firstSelectedWallet = when {
            globalSelectedWallet?.isMultiCurrency == true -> globalSelectedWallet
            allWallets.isNotEmpty() -> allWallets.first()
            else -> null
        }
        if (firstSelectedWallet != null) selectWalletTab(firstSelectedWallet.walletId)
    }

    private fun buildFlow() = flow {
        val walletsFlow = getWalletsUseCase.invokeAsMap(filterLocked = true)
        val fullPortfolioBlockFlow = combine(
            flow = walletsFlow,
            flow2 = portfolioListBlockDelegate.portfolioList,
            flow3 = selectedWalletFlow.map { wallet -> wallet.walletId }.distinctUntilChanged(),
            flow4 = settingContextUseCase.invoke(),
            transform = { allWallets, portfolioList, selectedWalletId, settings ->
                val tokensListData = portfolioList[selectedWalletId] ?: return@combine null
                val walletsUM = allWallets.entries
                    .map { (walletId, wallet) ->
                        val searchResultCount: TextReference? = portfolioList[walletId]?.totalTokensCount
                            ?.toString()
                            ?.let(::stringReference)
                            ?.takeIf { isSearchingState }
                        WalletTabUM(
                            text = stringReference(wallet.name),
                            onClick = { selectWalletTab(walletId) },
                            isSelected = selectedWalletId == walletId,
                            count = searchResultCount,
                        )
                    }
                val walletListUM = if (walletsUM.size != 1) {
                    WalletListUM(walletsUM.toPersistentList())
                } else {
                    WalletListUM(persistentListOf())
                }
                ChooseTokenPortfolioFullBlockUM(
                    walletList = walletListUM,
                    isBalanceHidden = settings.isBalanceHidden,
                    isSearching = isSearchingState,
                    tokensListData = tokensListData,
                )
            },
        )
        emitAll(fullPortfolioBlockFlow)
    }

    fun selectWalletTab(walletId: UserWalletId) {
        onWalletSelected.trySend(walletId)
    }

    @AssistedFactory
    interface Factory {
        fun create(
            modelScope: CoroutineScope,
            portfolioListBlockDelegate: PortfolioListBlockDelegate,
            searchQueryState: StateFlow<SearchQuery>,
        ): PortfolioFullBlockDelegate
    }
}