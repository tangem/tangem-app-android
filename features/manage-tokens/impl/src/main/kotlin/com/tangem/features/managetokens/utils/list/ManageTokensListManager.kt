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
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.*
import com.tangem.domain.managetokens.model.*
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.managetokens.analytics.ManageTokensAnalyticEvent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.ui.toggleExpanded
import com.tangem.pagination.Batch
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.Provider
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
    private val getManagedTokensUseCase: GetManagedTokensUseCase,
    private val getDistinctManagedTokensUseCase: GetDistinctManagedCurrenciesUseCase,
    private val checkHasLinkedTokensUseCase: CheckHasLinkedTokensUseCase,
    private val removeCustomCurrencyUseCase: RemoveCustomManagedCryptoCurrencyUseCase,
    private val checkCurrencyUnsupportedUseCase: CheckCurrencyUnsupportedUseCase,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val clipboardManager: ClipboardManager,
    @Assisted private val onCurrencySelect: (ManagedCryptoCurrency.Token) -> Unit = {},
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

        scope.launch {
            state.update { state ->
                val newBatches = getDistinctManagedTokensUseCase(batchListState.data)
                val currentBatches = state.currencyBatches

                // Distinct until changed
                if (newBatches.size == currentBatches.size &&
                    newBatches.map { it.key } == currentBatches.map { it.key } &&
                    newBatches.flatMap { it.data } == currentBatches.flatMap { it.data }
                ) {
                    return@launch
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
            val userWalletId = state.value.userWalletId
            val unsupportedState = userWalletId?.let { checkCurrencyUnsupportedState(it, source) }
            if (unsupportedState != null) {
                showUnsupportedWarning(unsupportedState)
            } else {
                addCurrency(batchKey, currency, source.network)
            }
        } else {
            if (checkNeedToShowRemoveNetworkWarning(currency, source.network)) {
                showRemoveNetworkWarning(
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

    private suspend fun showRemoveNetworkWarning(
        currency: ManagedCryptoCurrency,
        network: Network,
        isCoin: Boolean,
        onConfirm: () -> Unit,
    ) {
        val userWalletId = state.value.userWalletId
        val hasLinkedTokens = if (userWalletId == null || !isCoin) {
            false
        } else {
            checkHasLinkedTokens(userWalletId, network)
        }
        val canHideWithoutConfirming = source == ManageTokensSource.ONBOARDING

        if (hasLinkedTokens) {
            showLinkedTokensWarning(currency, network)
        } else if (canHideWithoutConfirming) {
            onConfirm()
        } else {
            showHideTokenWarning(currency, onConfirm)
        }
    }

    private fun showLinkedTokensWarning(currency: ManagedCryptoCurrency, network: Network) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_unable_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(
                id = R.string.token_details_unable_hide_alert_message,
                formatArgs = wrappedList(
                    currency.name,
                    currency.symbol,
                    network.name,
                ),
            ),
        )
        messageSender.send(message)
    }

    private fun showHideTokenWarning(currency: ManagedCryptoCurrency, onConfirm: () -> Unit) {
        val message = DialogMessage(
            title = resourceReference(
                id = R.string.token_details_hide_alert_title,
                formatArgs = wrappedList(currency.name),
            ),
            message = resourceReference(R.string.token_details_hide_alert_message),
            firstActionBuilder = {
                EventMessageAction(
                    title = resourceReference(R.string.token_details_hide_alert_hide),
                    warning = true,
                    onClick = onConfirm,
                )
            },
            secondActionBuilder = { cancelAction() },
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
        fun create(onCurrencySelect: (ManagedCryptoCurrency.Token) -> Unit = {}): ManageTokensListManager
    }
}