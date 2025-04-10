package com.tangem.features.onramp.swap.availablepairs.model

import arrow.core.getOrElse
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.extensions.capitalize
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceError
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.TokenList
import com.tangem.feature.swap.domain.GetAvailablePairsUseCase
import com.tangem.feature.swap.domain.models.domain.LeastTokenInfo
import com.tangem.feature.swap.domain.models.domain.SwapPairLeast
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.swap.availablepairs.AvailableSwapPairsComponent
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetErrorWarningTransformer
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetLoadingTokenItemsTransformer
import com.tangem.features.onramp.swap.availablepairs.entity.transformers.SetNoAvailablePairsTransformer
import com.tangem.features.onramp.tokenlist.entity.TokenListUM
import com.tangem.features.onramp.tokenlist.entity.TokenListUMController
import com.tangem.features.onramp.tokenlist.entity.TokenListUMTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.SetNothingToFoundStateTransformer
import com.tangem.features.onramp.tokenlist.entity.transformer.UpdateTokenItemsTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarActiveStateTransformer
import com.tangem.features.onramp.utils.UpdateSearchBarCallbacksTransformer
import com.tangem.features.onramp.utils.UpdateSearchQueryTransformer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

private typealias AvailablePairsState = Lce<Throwable, List<SwapPairLeast>>

@Suppress("LongParameterList")
internal class AvailableSwapPairsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val tokenListUMController: TokenListUMController,
    private val searchManager: InputManager,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getAvailablePairsUseCase: GetAvailablePairsUseCase,
) : Model() {

    val state: StateFlow<TokenListUM> = tokenListUMController.state

    private var params: AvailableSwapPairsComponent.Params = paramsContainer.require()

    private val tokenListFlow = getTokenListUseCaseFlow()

    private val availablePairsByNetworkFlow = MutableStateFlow<Map<LeastTokenInfo, AvailablePairsState>>(emptyMap())

    init {
        initializeSearchBarCallbacks()

        subscribeOnUpdateState()
        subscribeOnAvailablePairsUpdates()
    }

    private fun getTokenListUseCaseFlow(): SharedFlow<List<CryptoCurrencyStatus>> {
        return getTokenListUseCase.launch(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .map { maybeTokenList ->
                maybeTokenList.getOrElse(
                    ifLoading = { it ?: TokenList.Empty },
                    ifError = { TokenList.Empty },
                )
                    .flattenCurrencies()
            }
            .shareIn(scope = modelScope, started = SharingStarted.Eagerly, replay = 1)
    }

    private fun initializeSearchBarCallbacks() {
        tokenListUMController.update(
            transformer = UpdateSearchBarCallbacksTransformer(
                onQueryChange = ::onSearchQueryChange,
                onActiveChange = ::onSearchBarActiveChange,
            ),
        )
    }

    private fun subscribeOnUpdateState() {
        combine(
            flow = tokenListFlow,
            flow2 = getAppCurrencyAndBalanceHidingFlow(),
            flow3 = params.selectedStatus,
            flow4 = searchManager.query,
            flow5 = availablePairsByNetworkFlow
                .map { it[params.selectedStatus.value?.toLeastTokenInfo()] }
                .distinctUntilChanged(),
        ) { currencies, appCurrencyAndBalanceHiding, selectedStatus, query, availablePairsState ->
            availablePairsState?.fold(
                ifLoading = { SetLoadingTokenItemsTransformer(currencies) },
                ifContent = { pairs ->
                    handleContentState(
                        appCurrencyAndBalanceHiding = appCurrencyAndBalanceHiding,
                        currencies = currencies,
                        selectedStatus = selectedStatus,
                        query = query,
                        availablePairs = pairs,
                    )
                },
                ifError = {
                    handleErrorState(
                        cause = it,
                        networkInfo = params.selectedStatus.value?.toLeastTokenInfo(),
                        currencies = currencies,
                    )
                },
            )
                ?: SetLoadingTokenItemsTransformer(currencies)
        }
            .onEach(tokenListUMController::update)
            .flowOn(dispatchers.main)
            .launchIn(modelScope)
    }

    private fun handleContentState(
        appCurrencyAndBalanceHiding: Pair<AppCurrency, Boolean>,
        currencies: List<CryptoCurrencyStatus>,
        selectedStatus: CryptoCurrencyStatus?,
        query: String,
        availablePairs: List<SwapPairLeast>,
    ): TokenListUMTransformer {
        val (appCurrency, isBalanceHidden) = appCurrencyAndBalanceHiding

        if (availablePairs.isEmpty()) {
            return SetNoAvailablePairsTransformer(
                appCurrency = appCurrency,
                unavailableStatuses = currencies,
                isBalanceHidden = isBalanceHidden,
                unavailableTokensHeaderReference = resourceReference(
                    id = R.string.tokens_list_unavailable_to_swap_header,
                    wrappedList(selectedStatus?.currency?.name?.capitalize() ?: ""),
                ),
            )
        }

        val filterByQueryTokenList = currencies
            .filter { it.currency != selectedStatus?.currency }
            .filterByQuery(query = query)

        return if (query.isNotEmpty() && filterByQueryTokenList.isEmpty()) {
            SetNothingToFoundStateTransformer(
                isBalanceHidden = isBalanceHidden,
                emptySearchMessageReference = resourceReference(
                    id = R.string.action_buttons_swap_empty_search_message,
                ),
            )
        } else {
            UpdateTokenItemsTransformer(
                appCurrency = appCurrency,
                onItemClick = params.onTokenClick,
                statuses = filterByQueryTokenList.filterByAvailability(availablePairs = availablePairs),
                isBalanceHidden = isBalanceHidden,
                unavailableTokensHeaderReference = resourceReference(
                    id = R.string.tokens_list_unavailable_to_swap_header,
                    wrappedList(selectedStatus?.currency?.name?.capitalize() ?: ""),
                ),
            )
        }
    }

    private fun handleErrorState(
        cause: Throwable,
        networkInfo: LeastTokenInfo?,
        currencies: List<CryptoCurrencyStatus>,
    ): SetErrorWarningTransformer {
        return SetErrorWarningTransformer(
            cause = cause,
            onRefresh = {
                modelScope.launch {
                    if (networkInfo != null) {
                        updateAvailablePairs(networkInfo, currencies)
                    }
                }
            },
        )
    }

    private fun subscribeOnAvailablePairsUpdates() {
        modelScope.launch {
            params.selectedStatus
                .filterNotNull()
                .collectLatest { selectedStatus ->
                    val networkInfo = selectedStatus.toLeastTokenInfo()

                    val isAlreadyLoaded = availablePairsByNetworkFlow.value[networkInfo]?.isContent() ?: false
                    if (isAlreadyLoaded) return@collectLatest

                    val statuses = tokenListFlow.firstOrNull() ?: return@collectLatest

                    updateAvailablePairs(networkInfo = networkInfo, statuses = statuses)
                }
        }
    }

    private suspend fun updateAvailablePairs(networkInfo: LeastTokenInfo, statuses: List<CryptoCurrencyStatus>) {
        runCatching {
            availablePairsByNetworkFlow.update(networkInfo = networkInfo, state = lceLoading())

            getAvailablePairsUseCase(
                initialCurrency = networkInfo,
                currencies = statuses.map(CryptoCurrencyStatus::currency),
            )
        }
            .onSuccess { pairs ->
                availablePairsByNetworkFlow.update(networkInfo = networkInfo, state = pairs.lceContent())
            }
            .onFailure { cause ->
                availablePairsByNetworkFlow.update(networkInfo = networkInfo, state = cause.lceError())
            }
    }

    private fun MutableStateFlow<Map<LeastTokenInfo, AvailablePairsState>>.update(
        networkInfo: LeastTokenInfo,
        state: AvailablePairsState,
    ) {
        update {
            it.toMutableMap().apply {
                put(networkInfo, state)
            }
        }
    }

    private fun getAppCurrencyAndBalanceHidingFlow(): Flow<Pair<AppCurrency, Boolean>> {
        return combine(
            flow = getSelectedAppCurrencyUseCase().map { it.getOrElse { AppCurrency.Default } }.distinctUntilChanged(),
            flow2 = getBalanceHidingSettingsUseCase().map { it.isBalanceHidden }.distinctUntilChanged(),
            transform = ::Pair,
        )
    }

    private fun onSearchQueryChange(newQuery: String) {
        if (state.value.searchBarUM.query == newQuery) return

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
                status.value !is CryptoCurrencyStatus.Unreachable &&
                !status.currency.isCustom
        }
    }

    private fun CryptoCurrencyStatus.toLeastTokenInfo(): LeastTokenInfo {
        return LeastTokenInfo(
            contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress ?: "0",
            network = currency.network.backendId,
        )
    }
}