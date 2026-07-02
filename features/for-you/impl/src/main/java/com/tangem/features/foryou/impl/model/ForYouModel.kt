package com.tangem.features.foryou.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletTabUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.transformer.SetPortfolioReviewTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.extensions.orZero
import com.tangem.utils.transformer.update
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@Stable
@ModelScoped
internal class ForYouModel @Inject constructor(
    userWalletsListRepository: UserWalletsListRepository,
    multiAccountStatusListSupplier: MultiAccountStatusListSupplier,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    private val getWalletIconUseCase: GetWalletIconUseCase,
) : Model() {

    private val locallySelectedWalletId = MutableStateFlow<UserWalletId?>(value = null)
    private val expandedAssetIds = MutableStateFlow<Set<String>>(value = emptySet())
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    val uiState: StateFlow<ForYouUM>
        field = MutableStateFlow<ForYouUM>(
            ForYouUM(
                walletListUM = WalletListUM(persistentListOf()),
                portfolioReviewUM = PortfolioReviewUM.Loading(
                    tokenList = buildList<ForYouTokenListItemUM> {
                        repeat(4) { index ->
                            add(
                                ForYouTokenListItemUM(
                                    tokenRowUM = TangemTokenRowUM.Loading(
                                        id = index.toString(),
                                    ),
                                    tokenList = persistentListOf(),
                                    isExpanded = false,
                                    isExpandable = false,
                                ),
                            )
                        }
                    }.toPersistentList(),
                ),
            ),
        )

    init {
        combine(
            flow = userWalletsListRepository.userWallets,
            flow2 = userWalletsListRepository.selectedUserWallet,
            flow3 = multiAccountStatusListSupplier.invokeAsMap(),
            flow4 = locallySelectedWalletId,
            flow5 = expandedAssetIds,
        ) { wallets, globalSelectedWallet, accountStatusList, locallySelected, expanded ->
            val selectedId = locallySelected ?: globalSelectedWallet?.walletId
            val tabs = wallets.orEmpty().map { wallet ->
                WalletTabUM(
                    text = stringReference(wallet.name),
                    count = null,
                    isSelected = wallet.walletId == selectedId,
                    onClick = { onTabClick(wallet.walletId) },
                    deviceIcon = walletIconUMConverter.convert(getWalletIconUseCase(wallet)),
                )
            }

            val selectedAccountStatusList = accountStatusList[selectedId]
            val currencies = selectedAccountStatusList?.flattenCurrencies().orEmpty()
            val loadedBalance = selectedAccountStatusList?.totalFiatBalance as? TotalFiatBalance.Loaded
            val totalFiatBalance = loadedBalance?.amount.orZero()

            uiState.update(
                SetPortfolioReviewTransformer(
                    walletListUM = WalletListUM(
                        items = if (tabs.size != 1) tabs.toPersistentList() else persistentListOf(),
                    ),
                    currencies = currencies,
                    totalFiatBalance = totalFiatBalance,
                    appCurrency = selectedAppCurrencyFlow.value,
                    expandedAssetIds = expanded,
                    expandClick = ::onExpandClick,
                    onPeriodClick = ::onPeriodClick,
                ),
            )
        }
            .flowOn(dispatchers.default)
            .launchIn(modelScope)
    }

    private fun createSelectedAppCurrencyFlow(): StateFlow<AppCurrency> {
        return getSelectedAppCurrencyUseCase().map { maybeAppCurrency ->
            maybeAppCurrency.getOrElse { AppCurrency.Default }
        }.stateIn(
            scope = modelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppCurrency.Default,
        )
    }

    private fun onTabClick(walletId: UserWalletId) {
        locallySelectedWalletId.value = walletId
    }

    private fun onExpandClick(assetId: String) {
        expandedAssetIds.update { ids ->
            if (assetId in ids) ids - assetId else ids + assetId
        }
    }

    private fun onPeriodClick(tangemSegmentUM: TangemSegmentUM) {
        uiState.update { state ->
            state.copy(
                portfolioReviewUM = (state.portfolioReviewUM as? PortfolioReviewUM.Content)?.copy(
                    periodPickerUM = state.portfolioReviewUM.periodPickerUM.copy(
                        initialSelectedItem = tangemSegmentUM,
                    ),
                ) ?: state.portfolioReviewUM,
            )
        }
    }
}