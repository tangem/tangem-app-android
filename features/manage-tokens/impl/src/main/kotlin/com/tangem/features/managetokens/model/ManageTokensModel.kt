package com.tangem.features.managetokens.model

import androidx.annotation.StringRes
import arrow.core.getOrElse
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.HasMissedDerivationsUseCase
import com.tangem.domain.managetokens.SaveManagedTokensUseCase
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.analytics.CustomTokenAnalyticsEvent
import com.tangem.features.managetokens.analytics.ManageTokensAnalyticEvent
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensBottomSheetConfig
import com.tangem.features.managetokens.entity.managetokens.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.list.ChangedCurrencies
import com.tangem.features.managetokens.utils.list.ManageTokensListManager
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class ManageTokensModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val saveManagedTokensUseCase: SaveManagedTokensUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    manageTokensListManagerFactory: ManageTokensListManager.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ManageTokensComponent.Params = paramsContainer.require()

    private val manageTokensListManager = manageTokensListManagerFactory.create()

    val state: MutableStateFlow<ManageTokensUM> = MutableStateFlow(getInitialState(params.userWalletId))
    val bottomSheetNavigation: SlotNavigation<ManageTokensBottomSheetConfig> = SlotNavigation()

    init {
        manageTokensListManager.uiItems
            .onEach { items -> updateItems(items) }
            .launchIn(modelScope)

        manageTokensListManager.paginationStatus
            .onEach { status -> updatePaginationStatus(status) }
            .launchIn(modelScope)

        combine(
            manageTokensListManager.currenciesToAdd,
            manageTokensListManager.currenciesToRemove,
            ::updateChangedItems,
        ).launchIn(modelScope)

        observeSearchQueryChanges()

        modelScope.launch {
            manageTokensListManager.launchPagination(source = params.source, userWalletId = params.userWalletId)
        }
    }

    fun reloadList() {
        modelScope.launch {
            manageTokensListManager.reload(params.userWalletId)
        }
    }

    private fun getInitialState(userWalletId: UserWalletId?): ManageTokensUM {
        analyticsEventHandler.send(ManageTokensAnalyticEvent.ScreenOpened(params.source))

        return if (userWalletId == null) {
            createReadContentModel()
        } else {
            createManageContentModel()
        }
    }

    private fun createReadContentModel(): ManageTokensUM.ReadContent {
        return ManageTokensUM.ReadContent(
            popBack = router::pop,
            isInitialBatchLoading = true,
            isNextBatchLoading = false,
            items = getLoadingItems(),
            topBar = ManageTokensTopBarUM.ReadContent(
                title = resourceReference(R.string.common_search_tokens),
                onBackButtonClick = router::pop,
            ),
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.common_search),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            loadMore = ::loadMoreItems,
        )
    }

    private fun createManageContentModel(): ManageTokensUM.ManageContent {
        return ManageTokensUM.ManageContent(
            popBack = router::pop,
            isInitialBatchLoading = true,
            isNextBatchLoading = false,
            items = getLoadingItems(),
            topBar = ManageTokensTopBarUM.ManageContent(
                title = resourceReference(id = R.string.main_manage_tokens),
                onBackButtonClick = router::pop,
                endButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_plus_24,
                    onIconClicked = ::navigateToAddCustomToken,
                ),
            ),
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.common_search),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            hasChanges = false,
            saveChanges = ::saveChanges,
            loadMore = ::loadMoreItems,
            needToAddDerivations = false,
            isSavingInProgress = false,
        )
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQueryChanges() {
        state
            .distinctUntilChanged { old, new ->
                // It's also used to skip search activation to avoid searching an empty query
                old.search.query == new.search.query && new.search.isActive
            }
            .transform { state ->
                val query = state.search.query

                if (state.search.isActive) {
                    emit(query)
                }
            }
            .sample(periodMillis = 1_000)
            .onEach { query -> manageTokensListManager.search(userWalletId = params.userWalletId, query = query) }
            .launchIn(modelScope)
    }

    private fun updateItems(items: ImmutableList<CurrencyItemUM>) {
        val updatedState = state.updateAndGet { state ->
            state.copySealed(
                items = items,
            )
        }

        if (updatedState.items.isEmpty() && updatedState.search.isActive) {
            val event = ManageTokensAnalyticEvent.TokensIsNotFound(
                query = updatedState.search.query,
                source = params.source,
            )
            analyticsEventHandler.send(event)
        }
    }

    private fun updatePaginationStatus(status: PaginationStatus<*>) {
        state.update { state ->
            when (status) {
                is PaginationStatus.None,
                is PaginationStatus.InitialLoading,
                -> {
                    if (state.search.isActive) {
                        state.copySealed(
                            items = getLoadingItems(),
                        )
                    } else {
                        state.copySealed(
                            items = getLoadingItems(),
                            isInitialBatchLoading = true,
                        )
                    }
                }
                is PaginationStatus.NextBatchLoading -> state.copySealed(
                    isNextBatchLoading = true,
                )
                is PaginationStatus.InitialLoadingError -> {
                    val message = SnackbarMessage(
                        message = status.throwable.localizedMessage
                            ?.let(::stringReference)
                            ?: resourceReference(R.string.common_error),
                    )
                    messageSender.send(message)

                    state.copySealed(
                        isInitialBatchLoading = false,
                        isNextBatchLoading = false,
                    )
                }
                is PaginationStatus.Paginating -> {
                    (status.lastResult as? BatchFetchResult.Error)?.let { fetchError ->
                        Timber.e(fetchError.throwable)
                    }

                    state.copySealed(
                        isInitialBatchLoading = false,
                        isNextBatchLoading = false,
                        scrollToTop = if (state.isInitialBatchLoading && state.items.isNotEmpty()) {
                            triggeredEvent(
                                data = Unit,
                                onConsume = ::consumeScrollToTopEvent,
                            )
                        } else {
                            state.scrollToTop
                        },
                    )
                }
                is PaginationStatus.EndOfPagination -> {
                    state.copySealed(
                        isInitialBatchLoading = false,
                        isNextBatchLoading = false,
                    )
                }
            }
        }
    }

    private fun getLoadingItems(): ImmutableList<CurrencyItemUM> {
        return List(size = 10) { index ->
            CurrencyItemUM.Loading(index)
        }.toPersistentList()
    }

    private fun consumeScrollToTopEvent() {
        state.update { state ->
            state.copySealed(
                scrollToTop = consumedEvent(),
            )
        }
    }

    private fun updateChangedItems(currenciesToAdd: ChangedCurrencies, currenciesToRemove: ChangedCurrencies) {
        modelScope.launch {
            val hasMissedDerivations = params.userWalletId?.let { walletId ->
                val networks = currenciesToAdd.values
                    .flatten()
                    .toSet()
                    .associate { it.backendId to null }

                hasMissedDerivationsUseCase(walletId, networks)
            }

            state.update { state ->
                state.copySealed(
                    hasChanges = currenciesToAdd.isNotEmpty() || currenciesToRemove.isNotEmpty(),
                    needToAddDerivations = hasMissedDerivations ?: false,
                )
            }
        }
    }

    private fun loadMoreItems(): Boolean {
        val state = state.value
        if (state.isInitialBatchLoading || state.isNextBatchLoading) return false

        modelScope.launch {
            manageTokensListManager.loadMore(userWalletId = params.userWalletId, query = state.search.query)
        }

        return true
    }

    private fun navigateToAddCustomToken() {
        analyticsEventHandler.send(CustomTokenAnalyticsEvent.ButtonCustomToken(params.source))

        params.userWalletId?.let {
            bottomSheetNavigation.activate(ManageTokensBottomSheetConfig.AddCustomToken(it))
        }
    }

    private fun saveChanges() = resource(
        acquire = { state.update { state -> state.copySealed(isSavingInProgress = true) } },
        release = { state.update { state -> state.copySealed(isSavingInProgress = false) } },
    ) {
        val event = ManageTokensAnalyticEvent.TokenAdded(
            tokensCount = manageTokensListManager.currenciesToAdd.value.values.sumOf { it.size },
            source = params.source,
        )
        analyticsEventHandler.send(event)

        saveManagedTokensUseCase(
            userWalletId = requireNotNull(params.userWalletId),
            currenciesToAdd = manageTokensListManager.currenciesToAdd.value,
            currenciesToRemove = manageTokensListManager.currenciesToRemove.value,
        ).getOrElse {
            Timber.e(it, "Failed to save changes")
            return@resource
        }

        router.pop()
    }

    private fun searchCurrencies(query: String) {
        state.update { state ->
            state.copySealed(
                search = state.search.copy(
                    query = query,
                    isActive = true,
                ),
            )
        }
    }

    private fun toggleSearchBar(isActive: Boolean) {
        state.update { state ->
            @StringRes val placeholderTextRes = if (isActive) {
                R.string.manage_tokens_search_placeholder
            } else {
                R.string.common_search
            }

            state.copySealed(
                search = state.search.copy(
                    placeholderText = resourceReference(placeholderTextRes),
                    isActive = isActive,
                ),
            )
        }
    }
}