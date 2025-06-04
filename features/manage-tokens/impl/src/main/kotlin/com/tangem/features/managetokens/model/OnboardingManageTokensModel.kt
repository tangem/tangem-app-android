package com.tangem.features.managetokens.model

import arrow.core.flatten
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.card.HasMissedDerivationsUseCase
import com.tangem.domain.managetokens.SaveManagedTokensUseCase
import com.tangem.domain.redux.OnboardingManageTokensAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.features.managetokens.analytics.ManageTokensAnalyticEvent
import com.tangem.features.managetokens.component.ManageTokensSource
import com.tangem.features.managetokens.component.OnboardingManageTokensComponent
import com.tangem.features.managetokens.entity.item.CurrencyItemUM
import com.tangem.features.managetokens.entity.managetokens.OnboardingManageTokensUM
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
internal class OnboardingManageTokensModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val messageSender: UiMessageSender,
    private val reduxStateHolder: ReduxStateHolder,
    private val saveManagedTokensUseCase: SaveManagedTokensUseCase,
    private val hasMissedDerivationsUseCase: HasMissedDerivationsUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    manageTokensListManagerFactory: ManageTokensListManager.Factory,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: OnboardingManageTokensComponent.Params = paramsContainer.require()
    private val manageTokensListManager = manageTokensListManagerFactory.create()

    val state: MutableStateFlow<OnboardingManageTokensUM> = MutableStateFlow(getInitialState())
    val returnToParentComponentFlow = MutableSharedFlow<Unit>()

    init {
        manageTokensListManager.uiItems
            .onEach { items -> updateItems(items) }
            .launchIn(modelScope)

        manageTokensListManager.paginationStatus
            .onEach { status -> updatePaginationStatus(status) }
            .launchIn(modelScope)

        combine(
            flow = manageTokensListManager.currenciesToAdd,
            flow2 = manageTokensListManager.currenciesToRemove,
            transform = ::handleChangedCurrencies,
        ).launchIn(modelScope)

        observeSearchQueryChanges()

        modelScope.launch {
            manageTokensListManager.launchPagination(
                source = ManageTokensSource.ONBOARDING,
                userWalletId = params.userWalletId,
            )
        }
    }

    private fun getInitialState(): OnboardingManageTokensUM {
        analyticsEventHandler.send(ManageTokensAnalyticEvent.ScreenOpened(source = ManageTokensSource.ONBOARDING))

        return OnboardingManageTokensUM(
            isInitialBatchLoading = true,
            isNextBatchLoading = false,
            items = getLoadingItems(),
            loadMore = ::loadMoreItems,
            onBack = {},
            search = SearchBarUM(
                placeholderText = resourceReference(R.string.common_search),
                query = "",
                onQueryChange = ::searchCurrencies,
                isActive = false,
                onActiveChange = ::toggleSearchBar,
            ),
            actionButtonConfig = OnboardingManageTokensUM.ActionButtonConfig.Later(
                onClick = ::onLaterClick,
                showProgress = false,
            ),
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
            .onEach { query -> manageTokensListManager.search(userWalletId = params.userWalletId, query = query) }
            .launchIn(modelScope)
    }

    private fun updateItems(items: ImmutableList<CurrencyItemUM>) {
        val updatedState = state.updateAndGet { state -> state.copy(items = items) }

        if (updatedState.items.isEmpty() && updatedState.search.isActive) {
            val event = ManageTokensAnalyticEvent.TokensIsNotFound(
                query = updatedState.search.query,
                source = ManageTokensSource.ONBOARDING,
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
                        state.copy(items = getLoadingItems())
                    } else {
                        state.copy(items = getLoadingItems(), isInitialBatchLoading = true)
                    }
                }
                is PaginationStatus.NextBatchLoading -> state.copy(isNextBatchLoading = true)
                is PaginationStatus.InitialLoadingError -> {
                    val message = SnackbarMessage(
                        message = status.throwable.localizedMessage
                            ?.let(::stringReference)
                            ?: resourceReference(R.string.common_error),
                    )
                    messageSender.send(message)

                    state.copy(
                        isInitialBatchLoading = false,
                        isNextBatchLoading = false,
                    )
                }
                is PaginationStatus.Paginating -> {
                    (status.lastResult as? BatchFetchResult.Error)?.let { fetchError ->
                        Timber.e(fetchError.throwable)
                    }

                    state.copy(
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
                is PaginationStatus.EndOfPagination -> state.copy(
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
        state.update { state -> state.copy(scrollToTop = consumedEvent()) }
    }

    private suspend fun handleChangedCurrencies(
        currenciesToAdd: ChangedCurrencies,
        currenciesToRemove: ChangedCurrencies,
    ) {
        if (currenciesToAdd.isEmpty() && currenciesToRemove.isEmpty()) {
            state.update { state ->
                state.copy(
                    actionButtonConfig = OnboardingManageTokensUM.ActionButtonConfig.Later(onClick = ::onLaterClick),
                )
            }
        } else {
            val hasMissedDerivations = hasMissedDerivationsUseCase.invoke(
                userWalletId = params.userWalletId,
                networksWithDerivationPath = currenciesToAdd.values
                    .flatten()
                    .toSet()
                    .associate { it.backendId to null },
            )
            state.update { state ->
                state.copy(
                    actionButtonConfig = OnboardingManageTokensUM.ActionButtonConfig.Continue(
                        onClick = ::saveChanges,
                        showTangemIcon = hasMissedDerivations,
                    ),
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

    private fun saveChanges() = resource(
        acquire = {
            state.update { state ->
                state.copy(actionButtonConfig = state.actionButtonConfig.copySealed(showProgress = true))
            }
        },
        release = {
            state.update { state ->
                state.copy(actionButtonConfig = state.actionButtonConfig.copySealed(showProgress = false))
            }
        },
    ) {
        val event = ManageTokensAnalyticEvent.TokenAdded(
            tokensCount = manageTokensListManager.currenciesToAdd.value.values.sumOf { it.size },
            source = ManageTokensSource.ONBOARDING,
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

        returnToParentComponent()
    }

    private fun onLaterClick() = resource(
        acquire = {
            state.update { state ->
                state.copy(actionButtonConfig = state.actionButtonConfig.copySealed(showProgress = true))
            }
        },
        release = {
            state.update { state ->
                state.copy(actionButtonConfig = state.actionButtonConfig.copySealed(showProgress = false))
            }
        },
    ) {
        analyticsEventHandler.send(ManageTokensAnalyticEvent.ButtonLater)

        saveManagedTokensUseCase(
            userWalletId = requireNotNull(params.userWalletId),
            currenciesToAdd = manageTokensListManager.currenciesToAdd.value,
            currenciesToRemove = manageTokensListManager.currenciesToRemove.value,
        ).getOrElse {
            Timber.e(it, "Failed to save changes")
            return@resource
        }

        returnToParentComponent()
    }

    private fun returnToParentComponent() {
        // old onboarding
        reduxStateHolder.dispatch(OnboardingManageTokensAction.CurrenciesSaved)

        // new one
        modelScope.launch {
            returnToParentComponentFlow.emit(Unit)
        }
    }

    private fun searchCurrencies(query: String) {
        state.update { state ->
            state.copy(
                search = state.search.copy(
                    query = query,
                    isActive = true,
                ),
            )
        }
    }

    private fun toggleSearchBar(isActive: Boolean) {
        state.update { state ->
            state.copy(
                search = state.search.copy(
                    isActive = isActive,
                ),
            )
        }
    }
}