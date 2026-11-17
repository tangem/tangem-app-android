package com.tangem.features.earn.impl.picker.model

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.media.LoopingVideoController
import com.tangem.core.media.LoopingVideoState
import com.tangem.core.media.RemoteClipCache
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnOverviewFetcher
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.ui.EarnRowInfoUM
import com.tangem.features.earn.impl.InnerEarnNavigator
import com.tangem.features.earn.impl.analytics.EarnAddFundsSource
import com.tangem.features.earn.impl.analytics.EarnNoFundsNoticeAnalytics
import com.tangem.features.earn.impl.analytics.EarnPickerAnalyticsEvent
import com.tangem.features.earn.impl.getfunds.EarnGetTokenSheetRequest
import com.tangem.features.earn.impl.di.EarnHeroClip
import com.tangem.features.earn.impl.incentive.EarnIncentiveSheetRequest
import com.tangem.features.earn.impl.overview.EarnHeadlineSelector
import com.tangem.features.earn.impl.overview.EarnProductSummaries
import com.tangem.features.earn.impl.overview.EarnProductSummarySupplier
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.picker.EarnPickerHeroVideo
import com.tangem.features.earn.impl.picker.EarnYieldPickerComponent
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerTab
import com.tangem.features.earn.impl.picker.ui.state.EarnPickerUM
import com.tangem.features.earn.impl.redirect
import com.tangem.features.earn.impl.showReason
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.product.api.analytics.toAnalyticsApy
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.features.earn.product.api.product.EarnRowInfo
import com.tangem.features.earn.product.api.product.EarnRowKind
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
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
    private val appScope: AppCoroutineScope,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    videoFactory: LoopingVideoController.Factory,
    @EarnHeroClip heroClipCache: RemoteClipCache,
    private val cta: EarnPickerCta,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model(), EarnPickerClickIntents, EarnCalculatorComponent.ModelCallback {

    private val params: EarnYieldPickerComponent.Params = paramsContainer.require()

    private val productParams = EarnProduct.Params(params.userWalletId, params.cryptoCurrency)

    private val navigator = InnerEarnNavigator(router)

    /** One handler per product for the lifetime of the screen: a handler may keep state between clicks. */
    private val handlers = mutableMapOf<EarnProductId, EarnActionHandler>()

    private val isRedirected = AtomicBoolean(false)

    private val hasSettled = AtomicBoolean(false)

    private val selectedId = MutableStateFlow(params.preselected)

    private val noFundsNotice = EarnNoFundsNoticeAnalytics(analyticsEventHandler)

    private var isScreenOpenedSent = false

    private var lastShown: Shown? = null

    private val summaries: Flow<EarnProductSummaries> = summarySupplier(params.userWalletId)

    /** Bumped by Retry: a `state()` that failed has completed, so only a new collection can see the refreshed data. */
    private val stateRestarts = MutableStateFlow(0)

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

    val incentiveSheetRequests: SharedFlow<EarnIncentiveSheetRequest>
        field = MutableSharedFlow<EarnIncentiveSheetRequest>(extraBufferCapacity = 1)

    val getTokenSheetRequests: SharedFlow<EarnGetTokenSheetRequest>
        field = MutableSharedFlow<EarnGetTokenSheetRequest>(extraBufferCapacity = 1)

    val uiState: StateFlow<EarnPickerUM>
        field = MutableStateFlow(converter.initial(intents = this))

    private val heroVideo = videoFactory.createRemote(
        scope = modelScope,
        url = EarnPickerHeroVideo.URL,
        cache = heroClipCache,
        frameSize = EarnPickerHeroVideo.FRAME_SIZE,
        diagnosticsSubject = EarnPickerHeroVideo.DIAGNOSTICS_SUBJECT,
    )

    val heroVideoState: StateFlow<LoopingVideoState>
        get() = heroVideo.state

    val rowInfo: StateFlow<EarnRowInfoUM?>
        field = MutableStateFlow(null)

    private var isScreenResumed = false
    private var isHeroVisible = true

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
            .onEach { frame ->
                uiState.value = frame.um
                onShown(frame.shown)
            }
            .launchIn(modelScope)
    }

    fun setScreenResumed(isResumed: Boolean) {
        isScreenResumed = isResumed
        updateHeroVideoRunning()
    }

    fun setHeroVisible(isVisible: Boolean) {
        isHeroVisible = isVisible
        updateHeroVideoRunning()
    }

    override fun onDestroy() {
        heroVideo.release()
        super.onDestroy()
    }

    private fun updateHeroVideoRunning() {
        heroVideo.setRunning(isRunning = isScreenResumed && isHeroVisible)
    }

    private fun onSummariesFrame(frame: EarnProductSummaries) {
        val currencySummaries = frame[params.cryptoCurrency.id]
        val isPending = frame.isPending(params.cryptoCurrency.id)
        if (!isPending) hasSettled.set(true)
        resolveSelection(currencySummaries, isPending)
        redirectToActive(currencySummaries)
    }

    /**
     * A frame is usually partial at first (pending: the fastest product only), so nothing is chosen from it. A selection
     * that is still on a card stays; otherwise the first settled frame decides.
     */
    private fun resolveSelection(currencySummaries: List<EarnProductSummary>, isPending: Boolean) {
        if (currencySummaries.isEmpty() || isPending) return

        val isChosenPresent = currencySummaries.any { it.productId == selectedId.value }
        if (!isChosenPresent) selectedId.value = currencySummaries.first().productId
    }

    private fun redirectToActive(currencySummaries: List<EarnProductSummary>) {
        if (!params.shouldRedirectToActive) return

        val best = headlineSelector.bestActive(currencySummaries) ?: return
        if (isRedirected.compareAndSet(false, true)) {
            router.redirect(
                current = EarnPickerRoute(preselected = params.preselected, shouldRedirectToActive = true),
                replacement = EarnInfoRoute(best.productId),
            )
        }
    }

    /** Cards and content both come from `selectedId`; until its `state()` answers, the content is `Loading`. */
    private fun createUiState(
        frame: EarnProductSummaries,
        selection: Selection?,
        ethBalance: BigDecimal?,
        selectedId: EarnProductId?,
    ): Frame {
        val currencySummaries = frame[params.cryptoCurrency.id]
        val isPending = frame.isPending(params.cryptoCurrency.id)
        val chosen = selectedId?.let(registry::find)

        return when {
            isRedirecting(currencySummaries, isPending) -> Frame(converter.initial(intents = this))
            currencySummaries.isEmpty() && !isPending -> Frame(converter.unavailable(intents = this))
            chosen == null || isAwaitingSelection(chosen, currencySummaries) ->
                Frame(converter.initial(intents = this))
            else -> {
                val state = selection?.takeIf { it.product.id == chosen.id }?.state ?: EarnProductState.Loading
                val um = converter.convert(
                    product = chosen,
                    state = state,
                    summaries = currencySummaries,
                    selectedId = chosen.id,
                    ethBalance = ethBalance,
                    intents = this,
                )
                Frame(
                    um = um,
                    shown = Shown(
                        product = chosen,
                        state = state,
                        summaries = currencySummaries,
                        ethBalance = ethBalance,
                    ),
                )
            }
        }
    }

    /**
     * `Auto` never shows the picker: it waits while it is not known whether the currency has an active product, and
     * once there is one the redirect is on its way (or done), whichever thread gets here first. A refresh after the
     * first settled frame does not take the shown picker back.
     */
    private fun isRedirecting(currencySummaries: List<EarnProductSummary>, isPending: Boolean): Boolean =
        isRedirected.get() ||
            params.shouldRedirectToActive &&
            (isPending && !hasSettled.get() || currencySummaries.any { it.position != null })

    private fun isAwaitingSelection(chosen: EarnProduct, currencySummaries: List<EarnProductSummary>): Boolean =
        currencySummaries.none { it.productId == chosen.id }

    private fun onShown(shown: Shown?) {
        if (shown == null) {
            noFundsNotice.onCta(null)
            return
        }

        lastShown = shown
        if (!isScreenOpenedSent) {
            isScreenOpenedSent = true
            analyticsEventHandler.send(
                EarnPickerAnalyticsEvent.ScreenOpened(
                    source = params.source,
                    token = params.cryptoCurrency.symbol,
                    product = shown.product.analytics.product,
                    currentApy = shown.summaries.apyOf(shown.product.id),
                    products = shown.summaries.size,
                ),
            )
        }

        val isNoFunds = cta.kind(shown.state, shown.ethBalance) == EarnPickerCta.Kind.AddFunds
        noFundsNotice.onCta(
            EarnNoFundsNoticeAnalytics.Notice(
                token = params.cryptoCurrency.symbol,
                product = shown.product.analytics.product,
            ).takeIf { isNoFunds },
        )
    }

    private fun List<EarnProductSummary>.apyOf(id: EarnProductId): String? =
        firstOrNull { it.productId == id }?.rate?.toAnalyticsApy()

    private fun sendForSelectedProduct(event: (product: String) -> EarnPickerAnalyticsEvent) {
        val product = selectedId.value?.let(registry::find)?.analytics?.product ?: return
        analyticsEventHandler.send(event(product))
    }

    private fun sendProductChip(id: EarnProductId) {
        val summaries = lastShown?.summaries ?: return
        val position = summaries.indexOfFirst { it.productId == id }
        val product = registry.find(id)?.analytics?.product
        if (position < 0 || product == null) return

        analyticsEventHandler.send(
            EarnPickerAnalyticsEvent.ProductChip(
                product = product,
                position = position + 1,
                currentApy = summaries.apyOf(id),
            ),
        )
    }

    override fun onProductClick(id: EarnProductId) {
        if (id == selectedId.value) return

        sendProductChip(id)
        selectedId.value = id
    }

    override fun onActionClick(product: EarnProduct, action: EarnAction) {
        analyticsEventHandler.send(
            EarnPickerAnalyticsEvent.StartEarning(
                product = product.analytics.product,
                currentApy = lastShown?.summaries?.apyOf(product.id),
            ),
        )
        handlers
            .getOrPut(product.id) { product.actionHandler(productParams, navigator) }
            .onActionClick(action)
    }

    override fun onBlockedClick(block: EarnBlock) {
        uiMessageSender.showReason(block)
    }

    override fun onRetryClick(product: EarnProduct) {
        modelScope.launch {
            appScope.async {
                product.refresh(productParams).onLeft {
                    TangemLogger.e("Earn picker: refresh of ${product.id.value} failed", it)
                }
            }.await()
            stateRestarts.update { it + 1 }
        }
    }

    override fun onOverviewRetryClick() {
        appScope.launch {
            overviewFetcher(EarnOverviewFetcher.Params(params.userWalletId, params.cryptoCurrency)).onLeft {
                TangemLogger.e("Earn picker: overview fetch failed", it)
            }
        }
    }

    override fun onIncentiveClick() {
        val current = selection.value ?: return
        val ready = current.state as? EarnProductState.Ready ?: return
        val product = current.product

        if (ready.apyPeriods.isEmpty()) {
            TangemLogger.e("Earn picker: ${product.id.value} has an incentive but no APY periods")
        } else {
            incentiveSheetRequests.tryEmit(
                EarnIncentiveSheetRequest(
                    rate = ready.rate,
                    periods = ready.apyPeriods,
                    history = { period -> product.apyHistory(productParams, period) },
                ),
            )
        }
    }

    override fun onTabClick(tab: EarnPickerTab) {
        sendForSelectedProduct { EarnPickerAnalyticsEvent.Tab(product = it, tab = tab) }
    }

    override fun onHorizonChange(horizon: EarnHorizon) {
        sendForSelectedProduct { EarnPickerAnalyticsEvent.CalculatorHorizon(product = it, horizon = horizon) }
    }

    override fun onInputChange(input: EarnCalculatorComponent.Input) {
        sendForSelectedProduct { EarnPickerAnalyticsEvent.CalculatorInput(product = it, input = input) }
    }

    override fun onInfoClick(info: EarnRowInfo, row: EarnRowKind?) {
        sendForSelectedProduct { EarnPickerAnalyticsEvent.DetailRow(product = it, row = row) }
        rowInfo.value = info.toUM()
    }

    fun onRowInfoDismiss() {
        rowInfo.value = null
    }

    override fun onLinkClick(url: String, row: EarnRowKind?) {
        sendForSelectedProduct { EarnPickerAnalyticsEvent.DetailRow(product = it, row = row) }
        urlOpener.openUrl(url)
    }

    override fun onAddFundsClick() {
        analyticsEventHandler.send(
            EarnPickerAnalyticsEvent.AddFunds(
                token = params.cryptoCurrency.symbol,
                source = EarnAddFundsSource.YieldPicker,
            ),
        )
        getTokenSheetRequests.tryEmit(EarnGetTokenSheetRequest(params.userWalletId, params.cryptoCurrency))
    }

    override fun onCloseClick() {
        router.pop()
    }

    private data class Selection(val product: EarnProduct, val state: EarnProductState)

    private data class Shown(
        val product: EarnProduct,
        val state: EarnProductState,
        val summaries: List<EarnProductSummary>,
        val ethBalance: BigDecimal?,
    )

    private data class Frame(val um: EarnPickerUM, val shown: Shown? = null)
}