package com.tangem.features.polymarket.impl.history.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.res.R
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.ds2.tabnavigation.TangemTabItemUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.polymarket.model.PolymarketActivity
import com.tangem.domain.polymarket.model.PolymarketActivityFilter
import com.tangem.domain.polymarket.model.PolymarketActivityState
import com.tangem.domain.polymarket.usecase.CreatePolymarketActivitySessionUseCase
import com.tangem.features.polymarket.impl.history.PolymarketHistoryComponent
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.Content
import com.tangem.features.polymarket.impl.history.ui.state.PolymarketHistoryUM.NextPage
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.pagination.BatchFetchResult
import com.tangem.pagination.PaginationStatus
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import java.io.File

@ModelScoped
internal class PolymarketHistoryModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    paramsContainer: ParamsContainer,
    createSession: CreatePolymarketActivitySessionUseCase,
    private val exportActivity: PolymarketActivityExport,
    private val messageSender: UiMessageSender,
) : Model() {

    private val params = paramsContainer.require<PolymarketHistoryComponent.Params>()
    private val session = createSession(params.userWalletId)
    private val converter = PolymarketActivityUMConverter()
    private val exportFiles = Channel<File>(Channel.BUFFERED)
    val exports = exportFiles.receiveAsFlow()
    private var exportJob: Job? = null
    private val detailsConverter = PolymarketActivityDetailsConverter()
    private val searchPlaceholder = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        isActive = false,
        onQueryChange = {},
        onActiveChange = {},
        onCloseClick = {},
    )

    val uiState: StateFlow<PolymarketHistoryUM>
        field = MutableStateFlow(
            PolymarketHistoryUM(
                content = Content.Loading,
                onBackClick = ::onBackClick,
                onDismissDetails = ::dismissDetails,
            ),
        )

    init {
        modelScope.launch {
            session.state.collectLatest { state ->
                val content = content(state)
                uiState.update { currentState ->
                    currentState.copy(
                        content = content,
                        tabs = tabs(state.filter),
                        filterKey = state.filter.toString(),
                        pullToRefreshConfig = PullToRefreshConfig(state.isRefreshing) { session.refresh() },
                    )
                }
            }
        }
    }

    fun onResume() = session.resume(modelScope)

    fun onPause() = session.pause()

    private suspend fun content(state: PolymarketActivityState): Content {
        if (state.items.isEmpty()) return emptyContent(state)
        return Content.Data(
            groups = withContext(dispatchers.default) { converter.convert(state.items, ::openDetails) },
            search = searchPlaceholder,
            onDownloadCsvClick = ::exportCsv,
            nextPage = nextPage(state),
        )
    }

    private fun emptyContent(state: PolymarketActivityState): Content = when (val status = state.status) {
        PaginationStatus.InitialLoading, PaginationStatus.None, PaginationStatus.NextBatchLoading -> Content.Loading
        is PaginationStatus.InitialLoadingError -> Content.Error(session::retry)
        is PaginationStatus.Paginating -> if (status.lastResult is BatchFetchResult.Error) {
            Content.Error(session::retry)
        } else {
            Content.Loading
        }
        PaginationStatus.EndOfPagination -> if (state.filter == PolymarketActivityFilter()) {
            Content.Empty(::onBackClick)
        } else {
            Content.NoResults
        }
    }

    private fun nextPage(state: PolymarketActivityState): NextPage {
        if (state.isRefreshing) return NextPage.End
        return when (val status = state.status) {
            is PaginationStatus.InitialLoadingError -> NextPage.Error(session::retry)
            PaginationStatus.NextBatchLoading -> NextPage.Loading
            is PaginationStatus.Paginating -> if (status.lastResult is BatchFetchResult.Error) {
                NextPage.Error(session::retry)
            } else {
                NextPage.Ready(session::loadMore)
            }
            else -> NextPage.End
        }
    }

    private fun tabs(selected: PolymarketActivityFilter) = listOf(
        null to R.string.common_all,
        PolymarketActivity.Type.BUY to R.string.prediction_history_bought,
        PolymarketActivity.Type.SELL to R.string.prediction_history_sold,
        PolymarketActivity.Type.DEPOSIT to R.string.prediction_history_funds_added,
        PolymarketActivity.Type.WITHDRAW to R.string.transaction_history_withdrawn,
    ).map { (type, title) ->
        val filter = PolymarketActivityFilter(type)
        TangemTabItemUM.Content(
            id = type?.name ?: "ALL",
            label = resourceReference(title),
            isSelected = selected == filter,
            onClick = { session.selectFilter(filter) },
        )
    }.toImmutableList()

    private fun onBackClick() {
        exportJob?.cancel()
        router.pop()
    }

    private fun exportCsv() {
        if (uiState.value.isExporting) return
        val filter = session.state.value.filter
        uiState.update { it.copy(isExporting = true) }
        exportJob = modelScope.launch {
            try {
                runSuspendCatching { exportActivity(params.userWalletId, filter) }
                    .onSuccess { exportFiles.send(it) }
                    .onFailure { onExportShareFailed() }
            } finally {
                uiState.update { it.copy(isExporting = false) }
            }
        }
    }

    fun onExportShareFailed() {
        messageSender.send(SnackbarMessage(message = resourceReference(R.string.common_something_went_wrong)))
    }

    private fun openDetails(activity: PolymarketActivity) {
        val openPrediction = activity.market?.event?.eventId
            ?.takeIf(String::isNotBlank)
            ?.let { eventId ->
                {
                    dismissDetails()
                    router.push(
                        PolymarketRoute.EventDetails(
                            eventId = eventId,
                            userWalletId = params.userWalletId,
                            marketId = activity.market?.marketId,
                            assetId = activity.order?.assetId,
                        ),
                    )
                }
            }
        uiState.update { it.copy(details = detailsConverter.convert(activity, openPrediction)) }
    }

    private fun dismissDetails() {
        uiState.update { it.copy(details = null) }
    }
}