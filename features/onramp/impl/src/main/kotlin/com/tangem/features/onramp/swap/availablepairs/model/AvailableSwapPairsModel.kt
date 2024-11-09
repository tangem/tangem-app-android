package com.tangem.features.onramp.swap.availablepairs.model

import arrow.core.getOrElse
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.swap.domain.GetAvailablePairsUseCase
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetLoadingTokenItemsTransformer
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMController
import com.tangem.features.onramp.tokenlist.entity.transformer.UpdateTokenItemsTransformer
import com.tangem.features.onramp.utils.SearchManager
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
internal class AvailableSwapPairsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    getTokenListUseCase: GetTokenListUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tokenListUMController: TokenListUMController,
    private val searchManager: SearchManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getAvailablePairsUseCase: GetAvailablePairsUseCase,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private var params: AvailableSwapPairsComponent.Params = paramsContainer.require()

    private val tokenListFlow = getTokenListUseCase.launch(userWalletId = params.userWalletId)
        .distinctUntilChanged()
        .map { maybeTokenList ->
            maybeTokenList.getOrElse(
                ifLoading = { it ?: TokenList.Empty },
                ifError = { TokenList.Empty },
            )
                .flattenCurrencies()
        }
        .shareIn(scope = modelScope, started = SharingStarted.Eagerly, replay = 1)

    private val availablePairsByNetworkFlow = MutableStateFlow<Map<LeastTokenInfo, List<SwapPairLeast>>>(emptyMap())

    init {
        subscribeOnUpdateState()
        subscribeOnAvailablePairsUpdates()
    }

    private fun subscribeOnUpdateState() {
        combine(
            flow = tokenListFlow,
            flow2 = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } }.distinctUntilChanged(),
            flow3 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
            flow4 = searchManager.query,
            flow5 = availablePairsByNetworkFlow
                .map { it[params.selectedStatus.value?.toLeastTokenInfo()].orEmpty() }
                .distinctUntilChanged(),
        ) { currencies, appCurrency, isBalanceHidden, query, availablePairs ->
            if (availablePairs.isEmpty()) {
                SetLoadingTokenItemsTransformer(currencies)
            } else {
                val filterTokenList = currencies
                    .filterByQuery(query = query)
                    .filterByAvailability(availablePairs = availablePairs)

                Timber.e("${filterTokenList[true].orEmpty().size}")

                UpdateTokenItemsTransformer(
                    appCurrency = appCurrency,
                    onItemClick = params.onTokenClick,
                    statuses = filterTokenList,
                    isBalanceHidden = isBalanceHidden,
                    hasSearchBar = currencies.isNotEmpty(),
                    onQueryChange = ::onSearchQueryChange,
                    onActiveChange = ::onSearchBarActiveChange,
                )
            }
        }
            .onEach(tokenListUMController::update)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun subscribeOnAvailablePairsUpdates() {
        modelScope.launch {
            params.selectedStatus
                .filterNotNull()
                .collectLatest { selectedStatus ->
                    val initialCurrency = selectedStatus.toLeastTokenInfo()
                    val tokenList = tokenListFlow.firstOrNull() ?: return@collectLatest

                    val availablePairs = availablePairsByNetworkFlow.value[initialCurrency]
                    if (!availablePairs.isNullOrEmpty()) return@collectLatest

                    val pairs = getAvailablePairsUseCase(
                        initialCurrency = initialCurrency,
                        currencies = tokenList.map(CryptoCurrencyStatus::currency),
                    )

                    Timber.e("$initialCurrency = ${pairs.size}")

                    availablePairsByNetworkFlow.update {
                        it.toMutableMap().apply {
                            put(initialCurrency, pairs)
                        }
                    }
                }
        }
    }

    private fun onSearchQueryChange(newQuery: String) {
        val searchBar = tokenListUMController.getSearchBar()
        if (searchBar?.searchBarUM?.query == newQuery) return

        modelScope.launch {
            tokenListUMController.update(transformer = UpdateSearchQueryTransformer(newQuery))

            searchManager.update(newQuery)
        }
    }

    private fun onSearchBarActiveChange(isActive: Boolean) {
        tokenListUMController.update(
            transformer = UpdateSearchBarActiveStateTransformer(
                isActive = isActive,
                placeHolder = resourceReference(id = R.string.common_search),
            ),
        )
    }

    private fun List<CryptoCurrencyStatus>.filterByQuery(query: String): List<CryptoCurrencyStatus> {
        return filter {
            it.currency.name.contains(other = query, ignoreCase = true) ||
                it.currency.symbol.contains(other = query, ignoreCase = true)
        }
    }

    private fun List<CryptoCurrencyStatus>.filterByAvailability(
        availablePairs: List<SwapPairLeast>,
    ): Map<Boolean, List<CryptoCurrencyStatus>> {
        return groupBy { status ->
            val isAvailable = availablePairs.map(SwapPairLeast::to).contains(status.toLeastTokenInfo())

            isAvailable &&
                status.value !is CryptoCurrencyStatus.MissedDerivation &&
                status.value !is CryptoCurrencyStatus.Unreachable
        }
    }

    private fun CryptoCurrencyStatus.toLeastTokenInfo(): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = currency.network.backendId,
        )
    }
}