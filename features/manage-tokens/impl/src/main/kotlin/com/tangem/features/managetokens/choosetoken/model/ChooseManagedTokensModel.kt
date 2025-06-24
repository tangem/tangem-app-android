package com.tangem.features.managetokens.choosetoken.model

import androidx.annotation.StringRes
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.features.managetokens.choosetoken.entity.ChooseManageTokensBottomSheetConfig
import com.tangem.features.managetokens.choosetoken.entity.ChooseManagedTokenUM
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent
import com.tangem.features.managetokens.component.ChooseManagedTokensComponent.Source
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.managetokens.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.list.ManageTokensListManager
import com.tangem.features.managetokens.utils.list.getLoadingItems
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
import kotlin.collections.isNotEmpty

@ModelScoped
internal class ChooseManagedTokensModel @Inject constructor(
    private val router: Router,
    override val dispatchers: CoroutineDispatcherProvider,
    private val uiMessageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
    manageTokensListManagerFactory: ManageTokensListManager.Factory,
) : Model() {

    private val params: ChooseManagedTokensComponent.Params = paramsContainer.require()

    private val manageTokensListManager = manageTokensListManagerFactory.create(
        onCurrencySelect = { token ->
            bottomSheetNavigation.activate(
                ChooseManageTokensBottomSheetConfig.SwapTokensBottomSheetConfig(
                    userWalletId = params.userWalletId,
                    initialCurrency = params.initialCurrency,
                    token = token,
                ),
            )
        },
    )

    val bottomSheetNavigation: SlotNavigation<ChooseManageTokensBottomSheetConfig> = SlotNavigation()
    val uiState: StateFlow<ChooseManagedTokenUM>
    field = MutableStateFlow<ChooseManagedTokenUM>(createReadContentModel())

    init {
        manageTokensListManager.uiItems
            .onEach { items -> updateItems(items) }
            .launchIn(modelScope)

        manageTokensListManager.paginationStatus
            .onEach { status -> updatePaginationStatus(status) }
            .launchIn(modelScope)

        observeSearchQueryChanges()

        modelScope.launch {
            manageTokensListManager.launchPagination(source = ManageTokensSource.SEND_VIA_SWAP, userWalletId = null)
        }
    }

    private fun createReadContentModel(): ChooseManagedTokenUM {
        return ChooseManagedTokenUM(
            notificationUM = getNotification(),
            readContent = ManageTokensUM.ReadContent(
                popBack = router::pop,
                isInitialBatchLoading = true,
                isNextBatchLoading = false,
                items = getLoadingItems(),
                topBar = ManageTokensTopBarUM.ReadContent(
                    title = resourceReference(R.string.common_choose_token),
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
            ),
        )
    }

    private fun getNotification(): NotificationUM? {
        return when (params.source) {
            Source.SendViaSwap -> ChooseManagedTokensNotificationUM.SendViaSwap(
                onCloseClick = ::removeNotification,
            )
        }
    }

    private fun removeNotification() {
        uiState.update {
            it.copy(
                notificationUM = null,
            )
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQueryChanges() {
        uiState
            .distinctUntilChanged { old, new ->
                // It's also used to skip search activation to avoid searching an empty query
                old.readContent.search.query == new.readContent.search.query && new.readContent.search.isActive
            }
            .transform { state ->
                val query = state.readContent.search.query

                if (state.readContent.search.isActive) {
                    emit(query)
                }
            }
            .sample(periodMillis = 1_000)
            .onEach { query -> manageTokensListManager.search(userWalletId = null, query = query) }
            .launchIn(modelScope)
    }

    private fun updateItems(items: ImmutableList<CurrencyItemUM>) {
        uiState.update { state ->
            state.copy(
                readContent = state.readContent.copy(
                    items = items.filterNot {
                        it.id.value == params.initialCurrency.id.rawCurrencyId?.value
                    }.toPersistentList(),
                ),
            )
        }
    }

    private fun consumeScrollToTopEvent() {
        uiState.update { state ->
            state.copy(
                readContent = state.readContent.copy(scrollToTop = consumedEvent()),
            )
        }
    }

    private fun updatePaginationStatus(status: PaginationStatus<*>) {
        uiState.update { state ->
            val readContent = state.readContent
            when (status) {
                is PaginationStatus.None,
                is PaginationStatus.InitialLoading,
                -> {
                    if (readContent.search.isActive) {
                        state.copy(
                            readContent = readContent.copy(items = getLoadingItems()),
                        )
                    } else {
                        state.copy(
                            readContent = readContent.copy(
                                items = getLoadingItems(),
                                isInitialBatchLoading = true,
                            ),
                        )
                    }
                }
                is PaginationStatus.NextBatchLoading -> state.copy(
                    readContent = readContent.copy(isNextBatchLoading = true),
                )
                is PaginationStatus.InitialLoadingError -> {
                    val message = SnackbarMessage(
                        message = status.throwable.localizedMessage
                            ?.let(::stringReference)
                            ?: resourceReference(R.string.common_error),
                    )
                    uiMessageSender.send(message)

                    state.copy(
                        readContent = readContent.copy(
                            isInitialBatchLoading = false,
                            isNextBatchLoading = false,
                        ),
                    )
                }
                is PaginationStatus.Paginating -> {
                    (status.lastResult as? BatchFetchResult.Error)?.let { fetchError ->
                        Timber.e(fetchError.throwable)
                    }

                    state.copy(
                        readContent = readContent.copy(
                            isInitialBatchLoading = false,
                            isNextBatchLoading = false,
                            scrollToTop = if (readContent.isInitialBatchLoading && readContent.items.isNotEmpty()) {
                                triggeredEvent(
                                    data = Unit,
                                    onConsume = ::consumeScrollToTopEvent,
                                )
                            } else {
                                readContent.scrollToTop
                            },
                        ),
                    )
                }
                is PaginationStatus.EndOfPagination -> {
                    state.copy(
                        readContent = readContent.copy(
                            isInitialBatchLoading = false,
                            isNextBatchLoading = false,
                        ),
                    )
                }
            }
        }
    }

    private fun searchCurrencies(query: String) {
        uiState.update { state ->
            state.copy(
                readContent = state.readContent.copy(
                    search = state.readContent.search.copy(
                        query = query,
                        isActive = true,
                    ),
                ),
            )
        }
    }

    private fun toggleSearchBar(isActive: Boolean) {
        uiState.update { state ->
            @StringRes val placeholderTextRes = if (isActive) {
                R.string.manage_tokens_search_placeholder
            } else {
                R.string.common_search
            }

            state.copy(
                readContent = state.readContent.copy(
                    search = state.readContent.search.copy(
                        placeholderText = resourceReference(placeholderTextRes),
                        isActive = isActive,
                    ),
                ),
            )
        }
    }

    private fun loadMoreItems(): Boolean {
        val state = uiState.value
        if (state.readContent.isInitialBatchLoading || state.readContent.isNextBatchLoading) return false

        modelScope.launch {
            manageTokensListManager.loadMore(userWalletId = null, query = state.readContent.search.query)
        }

        return true
    }
}