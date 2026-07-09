package com.tangem.features.foryou.impl.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.core.ui.ds.tabs.TangemSegmentUM
import com.tangem.domain.account.status.supplier.MultiAccountStatusListSupplier
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.foryou.impl.components.state.MarketChartUM
import com.tangem.features.foryou.impl.entity.ForYouTokenListItemUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.entity.PortfolioReviewUM
import com.tangem.features.foryou.impl.model.transformer.SetPortfolioReviewTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
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
) : Model() {

    private val expandedAssetIds = MutableStateFlow<Set<String>>(value = emptySet())
    private val selectedAppCurrencyFlow: StateFlow<AppCurrency> = createSelectedAppCurrencyFlow()

    val uiState: StateFlow<ForYouUM>
        field = MutableStateFlow<ForYouUM>(
            ForYouUM(
                notifications = persistentListOf(),
                portfolioReviewUM = PortfolioReviewUM.Loading(
                    marketChartUM = MarketChartUM.NoData,
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
            flow = userWalletsListRepository.selectedUserWallet,
            flow2 = multiAccountStatusListSupplier.invokeAsMap(),
            flow3 = expandedAssetIds,
        ) { globalSelectedWallet, accountStatusList, expanded ->
            // TODO For You add choose portfolio flow
            uiState.update(
                SetPortfolioReviewTransformer(
                    accountStatusList = accountStatusList[globalSelectedWallet?.walletId],
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