package com.tangem.features.managetokens.utils.list

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.managetokens.GetManagedTokensUseCase
import com.tangem.domain.managetokens.model.ManageTokensListBatchingContext
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManageTokensUpdateAction
import com.tangem.domain.managetokens.model.ManagedCryptoCurrency
import com.tangem.domain.tokens.CheckHasLinkedTokensUseCase
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.impl.R
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchListState
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@ComponentScoped
internal class ManageTokensListManager @Inject constructor(
    private val getManagedTokensUseCase: GetManagedTokensUseCase,
    private val checkHasLinkedTokensUseCase: CheckHasLinkedTokensUseCase,
    private val messageSender: UiMessageSender,
    private val dispatchers: CoroutineDispatcherProvider,
) : ChangedCurrenciesManager,
    ManageTokensUiManager(
        messageSender = messageSender,
        dispatchers = dispatchers,
    ) {

    override lateinit var scope: CoroutineScope

    private val jobHolder = JobHolder()
    private val actionsFlow = MutableSharedFlow<ManageTokensBatchAction>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    override val state: MutableStateFlow<ManageTokensListState> = MutableStateFlow(ManageTokensListState())

    val paginationStatus: MutableStateFlow<PaginationStatus<*>> = MutableStateFlow(PaginationStatus.None)

    override val currenciesToAdd: MutableStateFlow<ChangedCurrencies> = MutableStateFlow(emptyMap())
    override val currenciesToRemove: MutableStateFlow<ChangedCurrencies> = MutableStateFlow(emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiItems: Flow<ImmutableList<CurrencyItemUM>> = state
        .mapLatest { state ->
            state.uiBatches.asSequence()
                .flatMap { it.data }
                .toImmutableList()
        }
        .distinctUntilChanged()

    suspend fun launch(userWalletId: UserWalletId?) = coroutineScope {
        scope = this

        val batchFlow = getManagedTokensUseCase(
            context = ManageTokensListBatchingContext(
                actionsFlow = actionsFlow,
                coroutineScope = this,
            ),
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
        state.value = ManageTokensListState()
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
        paginationStatus.value = batchListState.status

        state.update { state ->
            val newBatches = batchListState.data
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
                uiBatches = getUiBatches(newBatches, canEditItems),
                canEditItems = canEditItems,
            )
        }
    }

    override fun addCurrency(batchKey: Int, currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID) {
        updateChangedItems(currencyId, networkId, currenciesToRemove, currenciesToAdd)

        sendSelectCurrencyAction(batchKey, currencyId, networkId, isSelected = true)
    }

    override fun removeCurrency(batchKey: Int, currencyId: ManagedCryptoCurrency.ID, networkId: Network.ID) {
        updateChangedItems(currencyId, networkId, currenciesToAdd, currenciesToRemove)

        sendSelectCurrencyAction(batchKey, currencyId, networkId, isSelected = false)
    }

    override fun checkNeedToShowRemoveNetworkWarning(
        currencyId: ManagedCryptoCurrency.ID,
        networkId: Network.ID,
    ): Boolean = networkId !in currenciesToRemove.value[currencyId].orEmpty() &&
        networkId !in currenciesToAdd.value[currencyId].orEmpty()

    private fun sendSelectCurrencyAction(
        batchKey: Int,
        currencyId: ManagedCryptoCurrency.ID,
        networkId: Network.ID,
        isSelected: Boolean,
    ) {
        val request = ManageTokensUpdateAction.AddCurrency(
            currencyId = currencyId,
            networkId = networkId,
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
        return checkHasLinkedTokensUseCase(userWalletId, network).getOrElse {
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
}
