package com.tangem.features.managetokens.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ComponentScoped
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
import com.tangem.domain.managetokens.SaveManagedTokensUseCase
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

@ComponentScoped
internal class ManageTokensModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val manageTokensListManager: ManageTokensListManager,
    private val messageSender: UiMessageSender,
    private val saveManagedTokensUseCase: SaveManagedTokensUseCase,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ManageTokensComponent.Params = paramsContainer.require()
    private val userWalletId = (params.mode as? ManageTokensComponent.Mode.Manage)?.userWalletId

    val state: MutableStateFlow<ManageTokensUM> = MutableStateFlow(getInitialState(params.mode))
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
            manageTokensListManager.launchPagination(userWalletId)
        }
    }

    private fun getInitialState(mode: ManageTokensComponent.Mode): ManageTokensUM {
        return when (mode) {
            is ManageTokensComponent.Mode.ReadOnly -> createReadContentModel(mode)
            is ManageTokensComponent.Mode.Manage -> createManageContentModel(mode)
        }
    }

    fun reloadList() {
        modelScope.launch {
            manageTokensListManager.reload(userWalletId)
        }
    }

    private fun createReadContentModel(mode: ManageTokensComponent.Mode.ReadOnly): ManageTokensUM.ReadContent {
        return ManageTokensUM.ReadContent(
            popBack = router::pop,
            isInitialBatchLoading = true,
            isNextBatchLoading = false,
            items = getLoadingItems(),
            topBar = if (mode.showToolbar) {
                ManageTokensTopBarUM.ReadContent(
                    title = resourceReference(R.string.common_search_tokens),
                    onBackButtonClick = router::pop,
                )
            } else {
                null
            },
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            loadMore = ::loadMoreItems,
        )
    }

    private fun createManageContentModel(mode: ManageTokensComponent.Mode.Manage): ManageTokensUM.ManageContent {
        return ManageTokensUM.ManageContent(
            popBack = router::pop,
            isInitialBatchLoading = true,
            isNextBatchLoading = false,
            items = getLoadingItems(),
            topBar = if (mode.showToolbar) {
                ManageTokensTopBarUM.ManageContent(
                    title = resourceReference(id = R.string.main_manage_tokens),
                    onBackButtonClick = router::pop,
                    endButton = TopAppBarButtonUM(
                        iconRes = R.drawable.ic_plus_24,
                        onIconClicked = ::navigateToAddCustomToken,
                    ),
                )
            } else {
                null
            },
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            hasChanges = mode.forceShowSaveButton,
            saveChanges = { saveChanges(mode.onSaved) },
            loadMore = ::loadMoreItems,
            isSavingInProgress = false,
        )
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQueryChanges() {
        state
            .distinctUntilChanged { old, new ->
                // It's also used to skip search activation to avoid searching an empty query
                old.search.query == new.search.query &&
                    (old.search.isActive == new.search.isActive || new.search.isActive)
            }
            .transform { state ->
                val query = state.search.query

                if (state.search.isActive) {
                    emit(query)
                }
            }
            .sample(periodMillis = 1_000)
            .onEach { query -> manageTokensListManager.search(userWalletId = userWalletId, query = query) }
            .launchIn(modelScope)
    }

    private fun updateItems(items: ImmutableList<CurrencyItemUM>) {
        state.update { state ->
            state.copySealed(
                items = items,
            )
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
                is PaginationStatus.EndOfPagination -> state.copySealed(
                    isInitialBatchLoading = false,
                    isNextBatchLoading = false,
                )
            }
        }
    }

    private fun getLoadingItems(): ImmutableList<CurrencyItemUM> {
        return List(size = 10) { index ->
            CurrencyItemUM.Loading(index)
        }.toPersistentList()
    }

    private fun consumeScrollToTopEvent() {
        this.state.update { state ->
            state.copySealed(
                scrollToTop = consumedEvent(),
            )
        }
    }

    private fun updateChangedItems(currenciesToAdd: ChangedCurrencies, currenciesToRemove: ChangedCurrencies) {
        val forceShowSaveButton = (params.mode as? ManageTokensComponent.Mode.Manage)?.forceShowSaveButton ?: false
        state.update { state ->
            state.copySealed(
                hasChanges = forceShowSaveButton || currenciesToAdd.isNotEmpty() || currenciesToRemove.isNotEmpty(),
            )
        }
    }

    private fun loadMoreItems(): Boolean {
        val state = state.value
        if (state.isInitialBatchLoading || state.isNextBatchLoading) return false

        modelScope.launch {
            manageTokensListManager.loadMore(userWalletId = userWalletId, query = state.search.query)
        }

        return true
    }

    private fun navigateToAddCustomToken() {
        userWalletId?.let {
            bottomSheetNavigation.activate(ManageTokensBottomSheetConfig.AddCustomToken(it))
        }
    }

    private fun saveChanges(onSavedCallback: () -> Unit) {
        modelScope.launch {
            state.update { state -> state.copySealed(isSavingInProgress = true) }
            saveManagedTokensUseCase.invoke(
                userWalletId = requireNotNull(userWalletId),
                currenciesToAdd = manageTokensListManager.currenciesToAdd.value,
                currenciesToRemove = manageTokensListManager.currenciesToRemove.value,
            ).fold(
                ifLeft = { Timber.e(it, "Failed to save changes") },
                ifRight = { onSavedCallback() },
            )
            state.update { state -> state.copySealed(isSavingInProgress = false) }
        }
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
            state.copySealed(
                search = state.search.copy(
                    isActive = isActive,
                ),
            )
        }
    }
}
