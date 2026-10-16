package com.tangem.features.tangempay.cashback.impl.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.model.TangemPayCashback
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackAccrualsUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackDetailsUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackScreenUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CASHBACK_HISTORY_MONTHS = 5

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class TangemPayCashbackModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val cashbackRepository: CashbackRepository,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params: TangemPayCashbackComponent.Params = paramsContainer.require()
    private val userWalletId get() = params.userWalletId

    val bottomSheetNavigation: SlotNavigation<TangemPayCashbackNavigation> = SlotNavigation()

    private val cashbackConverter = TangemPayCashbackUmConverter()
    private val histogramConverter = TangemPayCashbackHistogramConverter()
    private val cardsConverter = TangemPayCashbackCardsConverter()
    private val additionalCashbackConverter = TangemPayAdditionalCashbackConverter(onLinkClick = urlOpener::openUrl)
    private val infoTilesConverter = TangemPayCashbackInfoTilesConverter(
        onRateClick = ::onConditionsTileClick,
        onAccrualsClick = ::onAccrualsTileClick,
    )
    private val detailsConverter = TangemPayCashbackDetailsConverter()
    private val accrualsConverter = TangemPayCashbackAccrualsConverter(onDocClick = ::onDocClick)

    val detailsSheet: StateFlow<TangemPayCashbackDetailsUM>
        field = MutableStateFlow(
            detailsConverter.convert(cards = emptyList(), payoutCurrency = null, accountMonthlyCap = null),
        )

    val accrualsSheet: StateFlow<TangemPayCashbackAccrualsUM>
        field = MutableStateFlow(accrualsConverter.convert(emptyList()))

    val uiState: StateFlow<TangemPayCashbackScreenUM>
        field = MutableStateFlow<TangemPayCashbackScreenUM>(
            TangemPayCashbackScreenUM.Loading(onCloseClick = router::pop),
        )

    private val loadedCashback = MutableStateFlow<LoadedCashback?>(null)

    private var loadJob: Job? = null

    init {
        analytics.send(TangemPayAnalyticsEvents.Cashback.DetailsScreenOpened())
        observeLoadedCashback()
        loadCashback()
    }

    private fun loadCashback() {
        loadJob?.cancel()
        uiState.value = TangemPayCashbackScreenUM.Loading(onCloseClick = router::pop)
        loadJob = modelScope.launch {
            val summaryDeferred = async { loadSummary() }
            val promotionsDeferred = async { loadPromotions() }
            val docsDeferred = async { loadDocs() }

            val summary = summaryDeferred.await()
            val promotions = promotionsDeferred.await()
            val docs = docsDeferred.await()

            if (summary == null || promotions == null || docs == null) {
                showError()
                return@launch
            }

            val cashbackHistory = if (summary is CashbackSummary.Enabled) {
                loadHistory() ?: run {
                    showError()
                    return@launch
                }
            } else {
                null
            }
            val cards = cardsConverter.convert(promotions)
            val cashback = (summary as? CashbackSummary.Enabled)?.cashback

            loadedCashback.value = LoadedCashback(
                cashback = cashback,
                history = cashbackHistory,
                cards = cards,
                additionalCashback = promotions.additionalCashback,
            )
            detailsSheet.value = detailsConverter.convert(
                cards = cards,
                payoutCurrency = cashback?.payoutCurrency,
                accountMonthlyCap = promotions.accountMonthlyCap,
            )
            accrualsSheet.value = accrualsConverter.convert(docs)
        }
    }

    private fun observeLoadedCashback() {
        combine(
            loadedCashback.filterNotNull(),
            getBalanceHidingSettingsUseCase.isBalanceHidden(),
            ::createContent,
        )
            .onEach { content -> uiState.value = content }
            .map { content -> content.cashback.banner?.type }
            .distinctUntilChanged()
            .onEach(::sendBannerAnalytics)
            .launchIn(modelScope)
    }

    private fun createContent(loaded: LoadedCashback, isBalanceHidden: Boolean) = TangemPayCashbackScreenUM.Content(
        onCloseClick = router::pop,
        cashback = cashbackConverter.convert(value = loaded.cashback, isBalanceHidden = isBalanceHidden),
        infoTiles = infoTilesConverter.convert(loaded.cards),
        histogram = loaded.history
            ?.takeIf { it.months.isNotEmpty() }
            ?.let { history ->
                histogramConverter.convert(
                    history = history,
                    totalEarnedAmount = loaded.cashback?.totalEarnedAmount,
                    totalCurrency = loaded.cashback?.currency,
                    isBalanceHidden = isBalanceHidden,
                )
            },
        additionalCashback = additionalCashbackConverter.convert(loaded.additionalCashback)
            .takeIf { it.items.isNotEmpty() },
    )

    private fun showError() {
        uiState.value = TangemPayCashbackScreenUM.Error(
            onCloseClick = router::pop,
            onReloadClick = ::loadCashback,
        )
    }

    private fun onConditionsTileClick() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.ConditionsTileClicked())
        bottomSheetNavigation.activate(TangemPayCashbackNavigation.Details)
    }

    private fun onAccrualsTileClick() {
        analytics.send(TangemPayAnalyticsEvents.Cashback.AccrualsTileClicked())
        bottomSheetNavigation.activate(TangemPayCashbackNavigation.Accruals)
    }

    private fun onDocClick(doc: CashbackDocument) {
        analytics.send(TangemPayAnalyticsEvents.Cashback.TermsDocClicked(title = doc.title))
        urlOpener.openUrl(doc.url)
    }

    private fun sendBannerAnalytics(bannerType: TangemPayCashbackUM.Banner.Type?) {
        val event = when (bannerType) {
            TangemPayCashbackUM.Banner.Type.Info -> TangemPayAnalyticsEvents.Cashback.UpcomingAccrualBannerShowed()
            TangemPayCashbackUM.Banner.Type.Error -> TangemPayAnalyticsEvents.Cashback.NegativeBannerShowed()
            null -> null
        }
        event?.let { analytics.send(it) }
    }

    private suspend fun loadSummary(): CashbackSummary? =
        runSuspendCatching { cashbackRepository.getCashbackSummary(userWalletId).getOrNull() }.getOrNull()

    private suspend fun loadHistory(): CashbackHistory? = runSuspendCatching {
        cashbackRepository.getCashbackHistory(userWalletId, CASHBACK_HISTORY_MONTHS).getOrNull()
    }.getOrNull()

    private suspend fun loadPromotions(): CashbackPromotions? =
        runSuspendCatching { cashbackRepository.getCashbackPromotions(userWalletId).getOrNull() }.getOrNull()

    private suspend fun loadDocs(): List<CashbackDocument>? =
        runSuspendCatching { cashbackRepository.getCashbackAccrualDocs(userWalletId).getOrNull() }.getOrNull()

    private data class LoadedCashback(
        val cashback: TangemPayCashback?,
        val history: CashbackHistory?,
        val cards: List<CashbackCard>,
        val additionalCashback: List<CashbackPromotions.AdditionalCashback>,
    )
}