package com.tangem.features.managetokens.utils.list

import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.model.CurrencyUnsupportedState
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.model.ManageTokensUpdateAction
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.managetokens.analytics.ManageTokensAnalyticEvent
import com.tangem.features.managetokens.component.ManageTokensMode
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.ui.toggleExpanded
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@Suppress("LongParameterList", "LargeClass")
internal class ManageTokensListManager @AssistedInject constructor(
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val clipboardManager: ClipboardManager,
    manageTokensWarningDelegateFactory: ManageTokensWarningDelegate.Factory,
    @Assisted private val useCasesFacade: ManageTokensUseCasesFacade,
    @Assisted private val source: ManageTokensSource,
    @Assisted private val mode: ManageTokensMode,
    @Assisted private val scope: CoroutineScope,
    @Assisted private val onCurrencySelect: (ManagedCryptoCurrency.Token) -> Unit = {},
) : ManageTokensUiActions {

    private val jobHolder = JobHolder()
    private val actionsFlow: MutableSharedFlow<ManageTokensBatchAction> = MutableSharedFlow(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val state: MutableStateFlow<ManageTokensListState> =
        MutableStateFlow(ManageTokensListState(mode = mode))

    private val manageTokensWarningDelegate: ManageTokensWarningDelegate = manageTokensWarningDelegateFactory
        .create(mode, source, this)
    private val changedCurrenciesManager = ChangedCurrenciesManager()
    private val uiManager = ManageTokensUiManager(
        state = state,
        manageTokensWarningDelegate = manageTokensWarningDelegate,
        dispatchers = dispatchers,
        actions = this,
        scope = scope,
    )

    val currenciesToAdd: StateFlow<ChangedCurrencies> = changedCurrenciesManager.currenciesToAdd.asStateFlow()
    val currenciesToRemove: StateFlow<ChangedCurrencies> = changedCurrenciesManager.currenciesToRemove.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val paginationStatus: Flow<PaginationStatus<*>> = state
        .mapLatest { it.status }
        .distinctUntilChanged()
    val uiItems: Flow<ImmutableList<CurrencyItemUM>> = uiManager.items

    suspend fun launchPagination() = coroutineScope {
        val loadUserTokensFromRemote = when (mode) {
            is ManageTokensMode.Wallet -> source == ManageTokensSource.ONBOARDING
            is ManageTokensMode.Account,
            ManageTokensMode.None,
            -> false
        }
        val batchFlow = useCasesFacade.getManagedTokensUseCase(
            context = ManageTokensListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = this,
            ),
            // only for onboarding case, change carefully and check repository implementation
            loadUserTokensFromRemote = loadUserTokensFromRemote,
        )

        batchFlow.state
            .onEach { state -> updateState(state) }
            .flowOn(dispatchers.default)
            .launchIn(scope = this)
            .saveIn(jobHolder)

        // Initial load
        reload()
    }

    suspend fun reload() {
        state.value = ManageTokensListState(mode = mode)
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = useCasesFacade.manageTokensListConfig(searchText = null),
            ),
        )
    }

    suspend fun loadMore(query: String) {
        actionsFlow.emit(
            BatchAction.LoadMore(
                requestParams = useCasesFacade.manageTokensListConfig(query),
            ),
        )
    }

    suspend fun search(query: String) {
        state.value = ManageTokensListState(mode = mode, searchQuery = query)
        actionsFlow.emit(
            BatchAction.Reload(
                requestParams = useCasesFacade.manageTokensListConfig(
                    searchText = query,
                ),
            ),
        )
    }

    private fun updateState(batchListState: BatchListState<Int, List<ManagedCryptoCurrency>>) {
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

        scope.launch {
            state.update { state ->
                val newBatches = useCasesFacade.getDistinctManagedTokensUseCase(batchListState.data)
                val currentBatches = state.currencyBatches

                // Distinct until changed
                if (newBatches.size == currentBatches.size &&
                    newBatches.map { it.key } == currentBatches.map { it.key } &&
                    newBatches.flatMap { it.data } == currentBatches.flatMap { it.data }
                ) {
                    return@launch
                }

                val canEditItems = when (state.mode) {
                    is ManageTokensMode.Account,
                    is ManageTokensMode.Wallet,
                    -> true
                    ManageTokensMode.None -> false
                }
                state.copy(
                    currencyBatches = newBatches,
                    uiBatches = uiManager.createOrUpdateUiBatches(newBatches, canEditItems),
                    canEditItems = canEditItems,
                )
            }
        }
    }

    override fun onTokenClick(currency: ManagedCryptoCurrency.Token) {
        if (source == ManageTokensSource.SEND_VIA_SWAP) {
            onCurrencySelect(currency)
        } else {
            toggleCurrencyNetworksVisibility(currency)
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

    override fun removeCustomCurrency(currency: ManagedCryptoCurrency.Custom) {
        scope.launch {
            useCasesFacade.removeCustomCurrencyUseCase(currency)
                .onRight { reload() }
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

    override suspend fun checkHasLinkedTokens(network: Network): Boolean {
        return useCasesFacade.checkHasLinkedTokensUseCase(
            network = network,
            tempAddedTokens = changedCurrenciesManager.currenciesToAdd.value,
            tempRemovedTokens = changedCurrenciesManager.currenciesToRemove.value,
        ).getOrElse {
            Timber.e(
                it,
                """
                    Failed to check linked tokens
                    |- Mode: $mode
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
        sourceNetwork: ManagedCryptoCurrency.SourceNetwork,
    ): CurrencyUnsupportedState? {
        return useCasesFacade.checkCurrencyUnsupportedUseCase(
            sourceNetwork = sourceNetwork,
        ).getOrElse {
            Timber.e(
                it,
                """
                    Failed to check currency unsupported state
                    |- Mode: $mode
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

    private fun toggleCurrencyNetworksVisibility(currency: ManagedCryptoCurrency.Token) = scope.launch(
        dispatchers.default,
    ) {
        state.update { batches ->
            val batchIndex = batches.batchIndexByCurrencyId(currency.id)
            val currencyBatch = batches.currencyBatches[batchIndex]
            val currencyIndex = currencyBatch.currencyIndexById(currency.id)

            val uiBatch = batches.uiBatches[batchIndex]
            val updatedUiItem = uiBatch.data[currencyIndex].toggleExpanded(
                currency = currencyBatch.data[currencyIndex],
                isEditable = batches.canEditItems,
                onSelectCurrencyNetwork = { networkId, isSelected ->
                    selectNetwork(currencyBatch.key, currency, networkId, isSelected)
                },
                onLongTap = ::copyContractAddress,
            )

            batches.updateUiBatchesItem(
                indexToBatch = batchIndex to uiBatch,
                indexToItem = currencyIndex to updatedUiItem,
            )
        }
    }

    private fun copyContractAddress(source: ManagedCryptoCurrency.SourceNetwork) {
        if (source is ManagedCryptoCurrency.SourceNetwork.Default) {
            clipboardManager.setText(text = source.contractAddress, isSensitive = false)
            showSnackbarMessage(resourceReference(R.string.contract_address_copied_message))
        }
    }

    private fun showSnackbarMessage(messageText: TextReference) {
        val message = SnackbarMessage(message = messageText)
        messageSender.send(message)
    }

    private fun selectNetwork(
        batchKey: Int,
        currency: ManagedCryptoCurrency,
        source: ManagedCryptoCurrency.SourceNetwork,
        isSelected: Boolean,
    ) = scope.launch(dispatchers.default) {
        if (currency !is ManagedCryptoCurrency.Token) return@launch

        if (isSelected) {
            val unsupportedState = checkCurrencyUnsupportedState(source)
            if (unsupportedState != null) {
                showUnsupportedWarning(unsupportedState)
            } else {
                addCurrency(batchKey, currency, source.network)
            }
        } else {
            if (checkNeedToShowRemoveNetworkWarning(currency, source.network)) {
                manageTokensWarningDelegate.showRemoveNetworkWarning(
                    currency = currency,
                    network = source.network,
                    isCoin = source is ManagedCryptoCurrency.SourceNetwork.Main,
                    onConfirm = {
                        removeCurrency(batchKey, currency, source.network)
                    },
                )
            } else {
                removeCurrency(batchKey, currency, source.network)
            }
        }
    }

    private fun showUnsupportedWarning(unsupportedState: CurrencyUnsupportedState) {
        val message = DialogMessage(
            title = resourceReference(R.string.common_warning),
            message = when (unsupportedState) {
                is CurrencyUnsupportedState.Token.NetworkTokensUnsupported -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.Token.UnsupportedCurve -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
                is CurrencyUnsupportedState.UnsupportedNetwork -> resourceReference(
                    id = R.string.alert_manage_tokens_unsupported_curve_message,
                    formatArgs = wrappedList(unsupportedState.networkName),
                )
            },
        )

        messageSender.send(message)
    }

    private fun Batch<Int, List<ManagedCryptoCurrency>>.currencyIndexById(id: ManagedCryptoCurrency.ID): Int {
        return data
            .indexOfFirst { it.id == id }
            .takeIf { it != -1 }
            ?: error("Currency with currency '$id' not found in batch #$key")
    }

    @AssistedFactory
    interface Factory {
        fun create(
            scope: CoroutineScope,
            mode: ManageTokensMode,
            source: ManageTokensSource,
            useCasesFacade: ManageTokensUseCasesFacade,
            onCurrencySelect: (ManagedCryptoCurrency.Token) -> Unit = {},
        ): ManageTokensListManager
    }
}