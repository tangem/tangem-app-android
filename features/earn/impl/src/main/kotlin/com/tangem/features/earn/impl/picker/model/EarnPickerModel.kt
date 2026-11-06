package com.tangem.features.earn.impl.picker.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.modalMessage
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.impl.InnerEarnNavigator
import com.tangem.features.earn.impl.overview.EarnHeadlineSelector
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.picker.EarnYieldPickerComponent
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.showReason
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
@ModelScoped
internal class EarnPickerModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val registry: EarnProductsRegistry,
    summarySupplier: EarnProductSummarySupplier,
    private val headlineSelector: EarnHeadlineSelector,
    private val converter: EarnPickerConverter,
    singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val overviewFetcher: EarnOverviewFetcher,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
) : Model() {

    private val params: EarnYieldPickerComponent.Params = paramsContainer.require()

    private val productParams = EarnProduct.Params(params.userWalletId, params.cryptoCurrency)

    private val navigator = InnerEarnNavigator(router)

    /** One handler per product for the lifetime of the screen: a handler may keep state between clicks. */
    private val handlers = mutableMapOf<EarnProductId, EarnActionHandler>()

    private val isRedirected = AtomicBoolean(false)

    @Volatile
    private var isSelectionResolved = false

    private val selectedId = MutableStateFlow(params.preselected)

    private val summaries: Flow<EarnProductSummaries> = summarySupplier(params.userWalletId)

    /** Bumped by Retry: a `state()` that failed has completed, so only a new collection can see the refreshed data. */
    private val stateRestarts = MutableStateFlow(0)

    /** One collection of `state()` serves the screen and the calculator. */
    private val selection: StateFlow<Selection?> = selectedId
        .combine(stateRestarts) { id, _ -> id }
        .flatMapLatest { id ->
            val product = id?.let(registry::find)
            if (product == null) {
                flowOf(null)
            } else {
                product.state(productParams)
                    .map { Selection(product, it) }
                    .catch { error ->
                        TangemLogger.e("Earn picker: state of ${product.id.value} failed", error)
                        emit(Selection(product, EarnProductState.Unavailable))
                    }
            }
        }
        .stateIn(modelScope, SharingStarted.Eagerly, null)

    /** `null` until the balance is known, and when the wallet has no status for the currency. */
    private val ethBalance: Flow<BigDecimal?> = singleAccountStatusListSupplier(params.userWalletId)
        .map { it.getCryptoCurrencyStatus(params.cryptoCurrency).getOrNull()?.value?.amount }
        .onStart { emit(null) }
        .distinctUntilChanged()

    val projection: Flow<EarnProjection?> = selection
        .map { (it?.state as? EarnProductState.Ready)?.projection }
        .distinctUntilChanged()

    val incentiveRate: Flow<EarnProductRate?> = selection
        .map { (it?.state as? EarnProductState.Ready)?.incentiveRate }
        .distinctUntilChanged()

    val uiState: StateFlow<EarnPickerUM>
        field = MutableStateFlow(converter.initial(onClose = ::onClose))

    private val callbacks = EarnPickerConverter.Callbacks(
        onProductClick = { selectedId.value = it },
        onActionClick = ::onActionClick,
        onBlocked = { uiMessageSender.showReason(it) },
        onAddFunds = ::onAddFunds,
        onRetryClick = ::onRetryClick,
        onInfoClick = ::onInfoClick,
        onLinkClick = urlOpener::openUrl,
        onClose = ::onClose,
    )

    init {
        summaries
            .onEach(::onSummariesFrame)
            .launchIn(modelScope)

        combine(
            flow = summaries,
            flow2 = selection,
            flow3 = ethBalance,
            flow4 = selectedId,
            transform = ::createUiState,
        )
            .flowOn(dispatchers.default)
            .onEach { uiState.value = it }
            .launchIn(modelScope)
    }

    private fun onSummariesFrame(frame: EarnProductSummaries) {
        val currencySummaries = frame[params.cryptoCurrency.id]
        resolveSelection(currencySummaries, frame.source)
        redirectToActive(currencySummaries)
    }

    /**
     * The first complete answer decides: later frames reorder the cards but never move the selection. A frame is
     * usually partial at first (`CACHE`: the fastest product only), so nothing is chosen from it, and a `preselected`
     * product that is already there is kept.
     */
    private fun resolveSelection(currencySummaries: List<EarnProductSummary>, source: StatusSource) {
        if (isSelectionResolved || currencySummaries.isEmpty()) return

        val current = selectedId.value
        when {
            current != null && currencySummaries.any { it.productId == current } -> isSelectionResolved = true
            source == StatusSource.CACHE -> Unit
            else -> {
                isSelectionResolved = true
                selectedId.value = currencySummaries.first().productId
            }
        }
    }

    private fun redirectToActive(currencySummaries: List<EarnProductSummary>) {
        if (!params.shouldRedirectToActive) return

        val best = headlineSelector.bestActive(currencySummaries) ?: return
        if (isRedirected.compareAndSet(false, true)) {
            router.replaceCurrent(EarnInfoRoute(best.productId))
        }
    }

    /**
     * The cards and the content both come from `selectedId`, so a screen never shows a product that is not the selected
     * card. Until the chosen product's `state()` answers, its card is selected and the content is `Loading`.
     */
    private fun createUiState(
        frame: EarnProductSummaries,
        selection: Selection?,
        ethBalance: BigDecimal?,
        selectedId: EarnProductId?,
    ): EarnPickerUM {
        val currencySummaries = frame[params.cryptoCurrency.id]
        val chosen = selectedId?.let(registry::find)

        return when {
            isRedirecting(currencySummaries, frame.source) -> converter.initial(onClose = ::onClose)
            currencySummaries.isEmpty() && frame.source != StatusSource.CACHE -> converter.unavailable(
                onRetryClick = ::onEmptyRetryClick,
                onClose = ::onClose,
            )
            chosen == null || isAwaitingSelection(chosen, currencySummaries) ->
                converter.initial(onClose = ::onClose)
            else -> converter.convert(
                product = chosen,
                state = selection?.takeIf { it.product.id == chosen.id }?.state ?: EarnProductState.Loading,
                summaries = currencySummaries,
                selectedId = chosen.id,
                ethBalance = ethBalance,
                callbacks = callbacks,
            )
        }
    }

    /**
     * `Auto` never shows the picker: it waits while it is not known whether the currency has an active product, and
     * once there is one the redirect is on its way (or done), whichever thread gets here first.
     */
    private fun isRedirecting(currencySummaries: List<EarnProductSummary>, source: StatusSource): Boolean =
        isRedirected.get() ||
            params.shouldRedirectToActive &&
            (source == StatusSource.CACHE || currencySummaries.any { it.position != null })

    /** The preselected product has no card yet: showing it would draw a screen with nothing selected. */
    private fun isAwaitingSelection(chosen: EarnProduct, currencySummaries: List<EarnProductSummary>): Boolean =
        !isSelectionResolved && currencySummaries.none { it.productId == chosen.id }

    private fun onActionClick(product: EarnProduct, action: EarnAction) {
        handlers
            .getOrPut(product.id) { product.actionHandler(productParams, navigator) }
            .onActionClick(action)
    }

    private fun onRetryClick(product: EarnProduct) {
        modelScope.launch {
            product.refresh(productParams).onLeft {
                TangemLogger.e("Earn picker: refresh of ${product.id.value} failed", it)
            }
            stateRestarts.update { it + 1 }
        }
    }

    private fun onEmptyRetryClick() {
        modelScope.launch {
            overviewFetcher(EarnOverviewFetcher.Params(params.userWalletId, params.cryptoCurrency)).onLeft {
                TangemLogger.e("Earn picker: overview fetch failed", it)
            }
        }
    }

    private fun onInfoClick(block: EarnTextBlock) {
        uiMessageSender.send(
            modalMessage {
                informer { item(title = block.title, description = block.description) }
            },
        )
    }

    private fun onAddFunds() {
        TangemLogger.i("Earn picker: Add funds has no destination yet")
    }

    private fun onClose() {
        router.pop()
    }

    private data class Selection(val product: EarnProduct, val state: EarnProductState)
}