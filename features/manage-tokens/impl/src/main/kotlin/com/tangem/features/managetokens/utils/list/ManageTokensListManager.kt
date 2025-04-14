package com.tangem.features.managetokens.utils.list

import androidx.compose.ui.util.fastForEachIndexed
import androidx.compose.ui.util.fastMap
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.CheckCurrencyUnsupportedUseCase
import com.tangem.domain.managetokens.CheckHasLinkedTokensUseCase
import com.tangem.domain.managetokens.GetManagedTokensUseCase
import com.tangem.domain.managetokens.RemoveCustomManagedCryptoCurrencyUseCase
import com.tangem.domain.managetokens.model.*
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.analytics.ManageTokensAnalyticEvent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ManageTokensListManager @Inject constructor(
    private val getManagedTokensUseCase: GetManagedTokensUseCase,
    private val checkHasLinkedTokensUseCase: CheckHasLinkedTokensUseCase,
    private val removeCustomCurrencyUseCase: RemoveCustomManagedCryptoCurrencyUseCase,
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    clipboardManager: ClipboardManager,
) : ManageTokensUiActions {

    private lateinit var scope: CoroutineScope
    private lateinit var source: ManageTokensSource

    private val jobHolder = JobHolder()
    private val actionsFlow: MutableSharedFlow<ManageTokensBatchAction> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val state: MutableStateFlow<ManageTokensListState> = MutableStateFlow(ManageTokensListState())

    private val changedCurrenciesManager = ChangedCurrenciesManager()
    private val uiManager = ManageTokensUiManager(
        state = state,
        messageSender = messageSender,
        dispatchers = dispatchers,
        actions = this,
        scopeProvider = Provider { scope },
        sourceProvider = Provider { source },
        clipboardManager = clipboardManager,
    )

    val currenciesToAdd: StateFlow<ChangedCurrencies> = changedCurrenciesManager.currenciesToAdd.asStateFlow()
    val currenciesToRemove: StateFlow<ChangedCurrencies> = changedCurrenciesManager.currenciesToRemove.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val paginationStatus: Flow<PaginationStatus<*>> = state
        .mapLatest { it.status }
        .distinctUntilChanged()
    val uiItems: Flow<ImmutableList<CurrencyItemUM>> = uiManager.items

    suspend fun launchPagination(source: ManageTokensSource, userWalletId: UserWalletId?) = coroutineScope {
        scope = this
        this@ManageTokensListManager.source = source

        val batchFlow = getManagedTokensUseCase(
            context = ManageTokensListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = this,
            ),
            loadUserTokensFromRemote = userWalletId != null && source == ManageTokensSource.ONBOARDING,
        )

        batchFlow.state
            .onEach { state -> updateState(state, userWalletId) }
            .flowOn(dispatchers.default)
            .launchIn(scope = this)
            .saveIn(jobHolder)

        // Initial load
        reload(userWalletId)
    }

    suspend fun reload(userWalletId: UserWalletId?) {
        state.value = ManageTokensListState()
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = ManageTokensListConfig(userWalletId, searchText = null),
            ),
        )
    }

    suspend fun loadMore(userWalletId: UserWalletId?, query: String) {
        actionsFlow.emit(
            BatchAction.LoadMore(
                requestParams = ManageTokensListConfig(userWalletId, query),
            ),
        )
    }

    suspend fun search(userWalletId: UserWalletId?, query: String) {
        state.value = ManageTokensListState(searchQuery = query)
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = ManageTokensListConfig(
                    userWalletId = userWalletId,
                    searchText = query,
                ),
            ),
        )
    }

    private fun updateState(
        batchListState: BatchListState<Int, List<ManagedCryptoCurrency>>,
        userWalletId: UserWalletId?,
    ) {
        state.update { state ->
            state.copy(
                status = batchListState.status,
            )
        }

        // Search nothing found
        if (
            state.value.searchQuery.isNullOrEmpty().not() &&
            batchListState.status is PaginationStatus.EndOfPagination &&
            batchListState.data.isEmpty()
        ) {
            state.update { state ->
                state.copy(
                    userWalletId = userWalletId,
                    currencyBatches = emptyList(),
                    uiBatches = listOf(
                        Batch(
                            key = Int.MAX_VALUE,
                            data = listOf(CurrencyItemUM.SearchNothingFound),
                        ),
                    ),
                )
            }

            return
        }

        state.update { state ->
            val newBatches = distinctCurrencies(batchListState.data)
            val currentBatches = state.currencyBatches

            // Distinct until changed
            if (newBatches.size == currentBatches.size &&
                newBatches.map { it.key } == currentBatches.map { it.key } &&
                newBatches.flatMap { it.data } == currentBatches.flatMap { it.data }
            ) {
                return
            }

            val canEditItems = userWalletId != null
            state.copy(
                userWalletId = userWalletId,
                currencyBatches = newBatches,
                uiBatches = uiManager.createOrUpdateUiBatches(newBatches, canEditItems),
                canEditItems = canEditItems,
            )
        }
    }

    // FIXME: Add interception functionality to BatchFlow state and do this on domain
    //  [REDACTED_JIRA]
    private fun distinctCurrencies(
        batches: List<Batch<Int, List<ManagedCryptoCurrency>>>,
    ): List<Batch<Int, List<ManagedCryptoCurrency>>> {
        val allCurrenciesIds = mutableListOf<ManagedCryptoCurrency.ID>()

        return batches.fastMap { batch ->
            val batchCurrencies = batch.data.toMutableList()

            batch.data.fastForEachIndexed { index, currency ->
                if (currency.id in allCurrenciesIds) {
                    batchCurrencies.removeAt(index)
                } else {
                    allCurrenciesIds.add(currency.id)
                }
            }

            batch.copy(data = batchCurrencies)
        }
    }

    override fun addCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network) {
        changedCurrenciesManager.addCurrency(currency, network)

        sendSelectCurrencyAction(batchKey, currency.id, network, isSelected = true)

        sendSelectCurrencyAnalyticsEvent(currency, isSelected = true)
    }

    override fun removeCurrency(batchKey: Int, currency: ManagedCryptoCurrency.Token, network: Network) {
        changedCurrenciesManager.removeCurrency(currency, network)

        sendSelectCurrencyAction(batchKey, currency.id, network, isSelected = false)

        sendSelectCurrencyAnalyticsEvent(currency, isSelected = false)
    }

    override fun removeCustomCurrency(userWalletId: UserWalletId, currency: ManagedCryptoCurrency.Custom) {
        scope.launch {
            removeCustomCurrencyUseCase.invoke(userWalletId, currency)
                .onRight { reload(userWalletId) }
                .onLeft { Timber.e(it) }
        }
    }

    override fun checkNeedToShowRemoveNetworkWarning(
        currency: ManagedCryptoCurrency.Token,
        network: Network,
    ): Boolean = !changedCurrenciesManager.containsCurrency(currency, network)

    private fun sendSelectCurrencyAnalyticsEvent(currency: ManagedCryptoCurrency.Token, isSelected: Boolean) {
        val event = ManageTokensAnalyticEvent.TokenSwitcherChanged(
            tokenSymbol = currency.symbol,
            isSelected = isSelected,
            source = source,
        )
        analyticsEventHandler.send(event)
    }

    private fun sendSelectCurrencyAction(
        batchKey: Int,
        currencyId: ManagedCryptoCurrency.ID,
        network: Network,
        isSelected: Boolean,
    ) {
        val request = ManageTokensUpdateAction.AddCurrency(
            currencyId = currencyId,
            network = network,
            isSelected = isSelected,
        )
        val action = BatchAction.UpdateBatches(
            keys = setOf(batchKey),
            async = true,
            updateRequest = request,
        )

        actionsFlow.tryEmit(action)
    }

    override suspend fun checkHasLinkedTokens(userWalletId: UserWalletId, network: Network): Boolean {
        return checkHasLinkedTokensUseCase(
            userWalletId = userWalletId,
            network = network,
            tempAddedTokens = changedCurrenciesManager.currenciesToAdd.value,
            tempRemovedTokens = changedCurrenciesManager.currenciesToRemove.value,
        ).getOrElse {
            Timber.e(
                it,
                """
                    Failed to check linked tokens
                    |- User wallet ID: $userWalletId
                    |- Network ID: ${network.id}
                """.trimIndent(),
            )

            val message = SnackbarMessage(
                message = it.localizedMessage
                    ?.let(::stringReference)
                    ?: resourceReference(R.string.common_error),
            )
            messageSender.send(message)

            false
        }
    }

    override suspend fun checkCurrencyUnsupportedState(
        userWalletId: UserWalletId,
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): CurrencyUnsupportedState? {
        return checkCurrencyUnsupportedUseCase(
            userWalletId = userWalletId,
            sourceNetwork = sourceNetwork,
        ).getOrElse {
            Timber.e(
                it,
                """
                    Failed to check currency unsupported state
                    |- User wallet ID: $userWalletId
                    |- Source Network: $sourceNetwork
                """.trimIndent(),
            )

            val message = SnackbarMessage(
                message = it.localizedMessage
                    ?.let(::stringReference)
                    ?: resourceReference(R.string.common_error),
            )
            messageSender.send(message)

            null
        }
    }
}