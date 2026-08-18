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
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.pay.repository.OnboardingRepository
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
    private val onboardingRepository: OnboardingRepository,
    private val analytics: AnalyticsEventHandler,
) : Model() {

    private val params: TangemPayCashbackComponent.Params = paramsContainer.require()
    private val userWalletId get() = params.userWalletId

    val bottomSheetNavigation: SlotNavigation<TangemPayCashbackNavigation> = SlotNavigation()

    private val cashbackConverter = TangemPayCashbackUmConverter()
    private val histogramConverter = TangemPayCashbackHistogramConverter()
    private val tiersConverter = TangemPayCashbackTiersConverter()
    private val additionalCashbackConverter = TangemPayAdditionalCashbackConverter()
    private val infoTilesConverter = TangemPayCashbackInfoTilesConverter(
        onRateClick = ::onConditionsTileClick,
        onAccrualsClick = ::onAccrualsTileClick,
    )
    private val detailsConverter = TangemPayCashbackDetailsConverter()
    private val accrualsConverter = TangemPayCashbackAccrualsConverter(onDocClick = ::onDocClick)

    val detailsSheet: StateFlow<TangemPayCashbackDetailsUM>
        field = MutableStateFlow(
            detailsConverter.convert(tiers = emptyList(), payoutCurrency = null, monthlyCap = null),
        )

    val accrualsSheet: StateFlow<TangemPayCashbackAccrualsUM>
        field = MutableStateFlow(accrualsConverter.convert(emptyList()))

    val uiState: StateFlow<TangemPayCashbackScreenUM>
        field = MutableStateFlow<TangemPayCashbackScreenUM>(
            TangemPayCashbackScreenUM.Loading(onCloseClick = router::pop),
        )

    private var loadJob: Job? = null

    init {
        analytics.send(TangemPayAnalyticsEvents.Cashback.DetailsScreenOpened())
        loadCashback()
    }

    private fun loadCashback() {
        loadJob?.cancel()
        uiState.value = TangemPayCashbackScreenUM.Loading(onCloseClick = router::pop)
        loadJob = modelScope.launch {
            val summaryDeferred = async { loadSummary() }
            val promotionsDeferred = async { loadPromotions() }
            val docsDeferred = async { loadDocs() }
            val planDeferred = async { loadPlan() }

            val summary = summaryDeferred.await()
            val promotions = promotionsDeferred.await()

            if (summary == null && promotions == null) {
                uiState.value = TangemPayCashbackScreenUM.Error(
                    onCloseClick = router::pop,
                    onReloadClick = ::loadCashback,
                )
                return@launch
            }

            val history = if (summary is CashbackSummary.Enabled) loadHistory() else null
            val plan = planDeferred.await()
            val tiers = promotions?.let(tiersConverter::convert).orEmpty()
            val payoutCurrency = (summary as? CashbackSummary.Enabled)?.cashback?.payoutCurrency
                ?: TangemPayCurrencyFactory.TOKEN_NAME

            val cashback = cashbackConverter.convert((summary as? CashbackSummary.Enabled)?.cashback)

            uiState.value = TangemPayCashbackScreenUM.Content(
                onCloseClick = router::pop,
                cashback = cashback,
                infoTiles = promotions?.let {
                    infoTilesConverter.convert(tiers = tiers, currentPlan = plan)
                },
                histogram = history?.takeIf { it.months.isNotEmpty() }?.let(histogramConverter::convert),
                additionalCashback = promotions
                    ?.let { additionalCashbackConverter.convert(it.additionalCashback) }
                    ?.takeIf { it.items.isNotEmpty() },
            )
            detailsSheet.value = detailsConverter.convert(
                tiers = tiers,
                payoutCurrency = payoutCurrency,
                monthlyCap = promotions?.monthlyCap,
            )
            accrualsSheet.value = accrualsConverter.convert(docsDeferred.await())

            sendBannerAnalytics(cashback.banner)
        }
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

    private fun sendBannerAnalytics(banner: TangemPayCashbackUM.Banner?) {
        val event = when (banner?.type) {
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

    private suspend fun loadDocs(): List<CashbackDocument> =
        runSuspendCatching { cashbackRepository.getCashbackAccrualDocs(userWalletId).getOrNull() }
            .getOrNull().orEmpty()

    private suspend fun loadPlan(): TangemPayTariffPlan? {
        return runSuspendCatching {
            onboardingRepository.getCustomerInfo(userWalletId).getOrNull()
        }.getOrNull()?.tariffPlan?.plan
    }
}