package com.tangem.features.managetokens.model

import com.tangem.core.decompose.di.ComponentScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.managetokens.component.ManageTokensComponent
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.ManageTokensTopBarUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import com.tangem.features.managetokens.utils.list.ChangedCurrencies
import com.tangem.features.managetokens.utils.list.ManageTokensListManager
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@ComponentScoped
internal class ManageTokensModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val manageTokensListManager: ManageTokensListManager,
    private val messageSender: UiMessageSender,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: ManageTokensComponent.Params = paramsContainer.require()

    val state: MutableStateFlow<ManageTokensUM> = MutableStateFlow(getInitialState(params.userWalletId))

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

        modelScope.launch {
            manageTokensListManager.launchPagination(params.userWalletId)
        }
    }

    private fun getInitialState(userWalletId: UserWalletId?): ManageTokensUM {
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
            items = getInitialItems(),
            topBar = ManageTokensTopBarUM.ReadContent(
                title = resourceReference(R.string.common_search_tokens),
                onBackButtonClick = router::pop,
            ),
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

    private fun createManageContentModel(): ManageTokensUM.ManageContent {
        return ManageTokensUM.ManageContent(
            popBack = router::pop,
            isInitialBatchLoading = true,
            isNextBatchLoading = false,
            items = getInitialItems(),
            topBar = ManageTokensTopBarUM.ManageContent(
                title = resourceReference(id = R.string.main_manage_tokens),
                onBackButtonClick = router::pop,
                endButton = TopAppBarButtonUM(
                    iconRes = R.drawable.ic_plus_24,
                    onIconClicked = ::onAddCustomToken,
                ),
            ),
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            hasChanges = false,
            saveChanges = ::saveChanges,
            loadMore = ::loadMoreItems,
        )
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
                        state
                    } else {
                        state.copySealed(
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
                is PaginationStatus.Paginating,
                is PaginationStatus.EndOfPagination,
                -> state.copySealed(
                    isInitialBatchLoading = false,
                    isNextBatchLoading = false,
                )
            }
        }
    }

    private fun updateChangedItems(currenciesToAdd: ChangedCurrencies, currenciesToRemove: ChangedCurrencies) {
        state.update { state ->
            state.copySealed(
                hasChanges = currenciesToAdd.isNotEmpty() || currenciesToRemove.isNotEmpty(),
            )
        }
    }

    private fun loadMoreItems(): Boolean {
        val state = state.value
        if (state.isInitialBatchLoading || state.isNextBatchLoading) return false

        modelScope.launch {
            manageTokensListManager.loadMore(
                userWalletId = params.userWalletId,
                query = state.search.query,
            )
        }

        return true
    }

    private fun getInitialItems(): ImmutableList<CurrencyItemUM> {
        return persistentListOf()
    }

    private fun onAddCustomToken() {
        // TODO: [REDACTED_JIRA]
    }

    private fun saveChanges() {
        // TODO: [REDACTED_JIRA]
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

        modelScope.launch {
            manageTokensListManager.search(params.userWalletId, query)
        }
    }

    private fun toggleSearchBar(isActive: Boolean) {
        state.update { state ->
            state.copySealed(
                search = state.search.copy(
                    query = if (isActive) state.search.query else "",
                    isActive = isActive,
                ),
            )
        }

        modelScope.launch {
            if (!isActive) {
                manageTokensListManager.reload(params.userWalletId)
            }
        }
    }
}