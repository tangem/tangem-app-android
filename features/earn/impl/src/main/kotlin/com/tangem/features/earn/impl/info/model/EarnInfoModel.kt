package com.tangem.features.earn.impl.info.model

import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.components.containers.pullToRefresh.PullToRefreshConfig
import com.tangem.core.ui.message.modalMessage
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusOperations.getCryptoCurrencyStatus
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.ui.EarnHowItWorksUM
import com.tangem.features.earn.impl.InnerEarnNavigator
import com.tangem.features.earn.impl.info.EarnInfoChartConfig
import com.tangem.features.earn.impl.info.EarnInfoComponent
import com.tangem.features.earn.impl.info.ui.state.EarnInfoUM
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.features.earn.impl.redirect
import com.tangem.features.earn.impl.showReason
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorSheetComponent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorSheetConfig
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import com.tangem.features.earn.product.api.navigation.EarnInfoRoute
import com.tangem.features.earn.product.api.navigation.EarnPickerRoute
import com.tangem.features.earn.product.api.product.EarnActionHandler
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.toUM
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class EarnInfoModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    registry: EarnProductsRegistry,
    singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    getSelectedAppCurrency: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val converter: EarnInfoConverter,
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val urlOpener: UrlOpener,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
    private val appScope: AppCoroutineScope,
    private val blogUrlBuilder: TangemBlogUrlBuilder,
) : Model(), EarnApyChartComponent.ModelCallback, EarnCalculatorSheetComponent.ModelCallback {

    private val params: EarnInfoComponent.Params = paramsContainer.require()

    private val productParams = EarnProduct.Params(params.userWalletId, params.cryptoCurrency)

    private val product: EarnProduct? = registry.find(params.productId)

    private val actions: EarnActionHandler? = product?.actionHandler(productParams, InnerEarnNavigator(router))

    private val isRedirected = AtomicBoolean(false)

    private val chartActivated = AtomicBoolean(false)

    /** Kept to re-render the amounts under a changed hiding setting when a frame brings no data to render. */
    private var shown: ShownPosition? = null

    private val readyState = MutableStateFlow<EarnProductState.Ready?>(null)

    private val currencyStatus: Flow<CryptoCurrencyStatus?> = singleAccountStatusListSupplier(params.userWalletId)
        .map { it.getCryptoCurrencyStatus(params.cryptoCurrency).getOrNull() }
        .onStart { emit(null) }
        .distinctUntilChanged()

    private val appCurrency: Flow<AppCurrency> = getSelectedAppCurrency.invokeOrDefault()

    private val isBalanceHidden: Flow<Boolean> = getBalanceHidingSettingsUseCase.isBalanceHidden()

    val uiState: StateFlow<EarnInfoUM?>
        field = MutableStateFlow<EarnInfoUM?>(null)

    val isChartActivated: Boolean
        get() = chartActivated.get()

    val chartNavigation: SlotNavigation<EarnInfoChartConfig> = SlotNavigation()

    val calculatorSheetNavigation: SlotNavigation<EarnCalculatorSheetConfig> = SlotNavigation()

    val howItWorks: StateFlow<EarnHowItWorksUM?>
        field = MutableStateFlow(null)

    val pullToRefresh: StateFlow<PullToRefreshConfig>
        field = MutableStateFlow(PullToRefreshConfig(isRefreshing = false, onRefresh = { onRefresh() }))

    val chartReloads: SharedFlow<Unit>
        field = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val chartRate: StateFlow<EarnProductRate> by lazy {
        val initial = requireNotNull(readyState.value) { "The chart is activated once the product is Ready" }.rate

        readyState.filterNotNull().map { it.rate }.stateIn(modelScope, SharingStarted.Eagerly, initial)
    }

    val projection: Flow<EarnProjection?> = readyState
        .map { it?.projection }
        .distinctUntilChanged()

    val incentiveRate: Flow<EarnProductRate?> = readyState
        .map { it?.incentiveRate }
        .distinctUntilChanged()

    private val callbacks = EarnInfoConverter.Callbacks(
        onActionClick = { actions?.onActionClick(it) },
        onBlocked = uiMessageSender::showReason,
        onInfoClick = ::onInfoClick,
        onLinkClick = urlOpener::openUrl,
        onHowItWorksClick = ::onHowItWorksClick,
        onClose = ::onClose,
    )

    init {
        if (product == null) {
            onUnknownProduct()
        } else {
            observe(product)
        }
    }

    fun chartParams(): EarnApyChartComponent.Params {
        val product = requireNotNull(product) { "The chart is activated for a registered product only" }
        val ready = requireNotNull(readyState.value) { "The chart is activated once the product is Ready" }
        val periods = ready.apyPeriods

        return EarnApyChartComponent.Params(
            periods = periods,
            defaultPeriod = periods.last(),
            history = { period -> product.apyHistory(productParams, period) },
            currentRate = chartRate,
            reloads = chartReloads,
            callback = this.takeIf { ready.projection != null },
        )
    }

    override fun onCalculatorClick() {
        calculatorSheetNavigation.activate(EarnCalculatorSheetConfig)
    }

    override fun onDismiss() {
        calculatorSheetNavigation.dismiss()
    }

    private fun onRefresh() {
        val product = product ?: return
        if (pullToRefresh.value.isRefreshing) return

        setRefreshing(isRefreshing = true)
        modelScope.launch {
            try {
                // A refresh cut short leaves the stored statuses on CACHE, so it outlives the screen
                appScope.async { refresh(product) }.await()
            } finally {
                setRefreshing(isRefreshing = false)
            }
        }
    }

    private suspend fun refresh(product: EarnProduct) = coroutineScope {
        val quote = async { refreshQuote() }

        product.refresh(productParams)
            .onLeft { error -> TangemLogger.e("Earn info: refresh of ${product.id.value} failed", error) }
            .onRight { chartReloads.tryEmit(Unit) }

        quote.await()
    }

    private suspend fun refreshQuote() {
        val rawCurrencyId = params.cryptoCurrency.id.rawCurrencyId ?: return

        singleQuoteStatusFetcher(SingleQuoteStatusFetcher.Params(rawCurrencyId = rawCurrencyId, appCurrencyId = null))
            .onLeft { error -> TangemLogger.e("Earn info: quote refresh failed", error) }
    }

    private fun setRefreshing(isRefreshing: Boolean) {
        pullToRefresh.update { config -> config.copy(isRefreshing = isRefreshing) }
    }

    private fun observe(product: EarnProduct) {
        combine(
            flow = productState(product),
            flow2 = currencyStatus,
            flow3 = appCurrency,
            flow4 = isBalanceHidden,
            transform = ::Frame,
        )
            .flowOn(dispatchers.default)
            .onEach { onFrame(product, it) }
            .launchIn(modelScope)
    }

    private fun productState(product: EarnProduct): Flow<EarnProductState> = product.state(productParams)
        .catch { error ->
            TangemLogger.e("Earn info: state of ${product.id.value} failed", error)
            emit(EarnProductState.Unavailable)
        }

    private fun onFrame(product: EarnProduct, frame: Frame) {
        when (val state = frame.state) {
            EarnProductState.Loading -> applyBalanceHidingToShown(frame.isBalanceHidden)
            EarnProductState.Unavailable -> redirectToPicker()
            is EarnProductState.Ready -> onReady(product, state, frame)
        }
    }

    private fun onReady(product: EarnProduct, ready: EarnProductState.Ready, frame: Frame) {
        if (ready.position !is EarnPositionState.Known) {
            redirectToPicker()
            return
        }

        val status = frame.status
        if (status == null) {
            applyBalanceHidingToShown(frame.isBalanceHidden)
        } else {
            showPosition(
                ShownPosition(
                    product = product,
                    ready = ready,
                    status = status,
                    appCurrency = frame.appCurrency,
                    isBalanceHidden = frame.isBalanceHidden,
                ),
            )
        }
    }

    private fun applyBalanceHidingToShown(isBalanceHidden: Boolean) {
        val current = shown ?: return
        if (current.isBalanceHidden != isBalanceHidden) {
            showPosition(current.copy(isBalanceHidden = isBalanceHidden))
        }
    }

    private fun showPosition(position: ShownPosition) {
        val info = converter.convert(
            product = position.product,
            status = position.status,
            appCurrency = position.appCurrency,
            isBalanceHidden = position.isBalanceHidden,
            ready = position.ready,
            callbacks = callbacks,
        )

        if (info == null) {
            redirectToPicker()
            return
        }

        shown = position
        readyState.value = position.ready
        uiState.value = info
        activateChartOnce(position.ready)
    }

    private fun activateChartOnce(ready: EarnProductState.Ready) {
        if (ready.apyPeriods.isNotEmpty() && chartActivated.compareAndSet(false, true)) {
            chartNavigation.activate(EarnInfoChartConfig)
        }
    }

    private fun redirectToPicker() {
        if (isRedirected.compareAndSet(false, true)) {
            router.redirect(
                current = EarnInfoRoute(params.productId),
                replacement = EarnPickerRoute(preselected = params.productId, shouldRedirectToActive = false),
            )
        }
    }

    private fun onUnknownProduct() {
        TangemLogger.e("Earn info: unknown product ${params.productId.value}")
        modelScope.launch(dispatchers.main) { router.pop() }
    }

    private fun onInfoClick(block: EarnTextBlock) {
        uiMessageSender.send(
            modalMessage {
                informer { item(title = block.title, description = block.description) }
            },
        )
    }

    fun onHowItWorksDismiss() {
        howItWorks.value = null
    }

    private fun onHowItWorksClick(content: EarnHowItWorks) {
        howItWorks.value = content.toUM(onLearnMoreClick = ::openBlogPost)
    }

    private fun openBlogPost(post: TangemBlogUrlBuilder.Post) {
        modelScope.launch { urlOpener.openUrl(blogUrlBuilder.build(post)) }
    }

    private fun onClose() {
        router.pop()
    }

    private data class Frame(
        val state: EarnProductState,
        val status: CryptoCurrencyStatus?,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
    )

    private data class ShownPosition(
        val product: EarnProduct,
        val ready: EarnProductState.Ready,
        val status: CryptoCurrencyStatus,
        val appCurrency: AppCurrency,
        val isBalanceHidden: Boolean,
    )
}