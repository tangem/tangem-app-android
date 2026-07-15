package com.tangem.features.tangempay.cashback.impl.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.pay.model.CashbackDocument
import com.tangem.domain.pay.model.CashbackHistory
import com.tangem.domain.pay.model.CashbackPromotions
import com.tangem.domain.pay.model.CashbackSummary
import com.tangem.domain.pay.repository.CashbackRepository
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.features.tangempay.cashback.api.TangemPayCashbackComponent
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackAccrualsUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackDetailsUM
import com.tangem.features.tangempay.cashback.impl.ui.state.TangemPayCashbackScreenUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CASHBACK_HISTORY_MONTHS = 5

@Stable
@ModelScoped
internal class TangemPayCashbackModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val cashbackRepository: CashbackRepository,
    private val onboardingRepository: OnboardingRepository,
) : Model() {

    private val params: TangemPayCashbackComponent.Params = paramsContainer.require()
    private val userWalletId get() = params.userWalletId

    val bottomSheetNavigation: SlotNavigation<TangemPayCashbackNavigation> = SlotNavigation()

    private val cashbackConverter = TangemPayCashbackUmConverter()
    private val histogramConverter = TangemPayCashbackHistogramConverter()
    private val tiersConverter = TangemPayCashbackTiersConverter()
    private val additionalCashbackConverter = TangemPayAdditionalCashbackConverter()
    private val infoTilesConverter = TangemPayCashbackInfoTilesConverter(
        onRateClick = { bottomSheetNavigation.activate(TangemPayCashbackNavigation.Details) },
        onAccrualsClick = { bottomSheetNavigation.activate(TangemPayCashbackNavigation.Accruals) },
    )
    private val detailsConverter = TangemPayCashbackDetailsConverter()
    private val accrualsConverter = TangemPayCashbackAccrualsConverter(onDocClick = urlOpener::openUrl)

    val detailsSheet: StateFlow<TangemPayCashbackDetailsUM>
        field = MutableStateFlow(detailsConverter.convert(emptyList()))

    val accrualsSheet: StateFlow<TangemPayCashbackAccrualsUM>
        field = MutableStateFlow(accrualsConverter.convert(emptyList()))

    val uiState: StateFlow<TangemPayCashbackScreenUM>
        field = MutableStateFlow<TangemPayCashbackScreenUM>(
            TangemPayCashbackScreenUM.Loading(onCloseClick = router::pop),
        )

    private var loadJob: Job? = null

    init {
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

            uiState.value = TangemPayCashbackScreenUM.Content(
                onCloseClick = router::pop,
                cashback = cashbackConverter.convert((summary as? CashbackSummary.Enabled)?.cashback),
                infoTiles = promotions?.let {
                    infoTilesConverter.convert(tiers = tiers, currentPlan = plan)
                },
                histogram = history?.takeIf { it.months.isNotEmpty() }?.let(histogramConverter::convert),
                additionalCashback = promotions
                    ?.let { additionalCashbackConverter.convert(it.additionalCashback) }
                    ?.takeIf { it.items.isNotEmpty() },
            )
            detailsSheet.value = detailsConverter.convert(tiers)
            accrualsSheet.value = accrualsConverter.convert(docsDeferred.await())
        }
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