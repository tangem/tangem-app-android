package com.tangem.features.marketing.impl.model

import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.domain.marketing.DismissMarketingBannerUseCase
import com.tangem.domain.marketing.GetMarketingBannerUseCase
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.matchesUsdAmount
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@OptIn(FlowPreview::class)
@ModelScoped
internal class MarketingBannerModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getMarketingBanner: GetMarketingBannerUseCase,
    private val dismissMarketingBanner: DismissMarketingBannerUseCase,
    private val deeplinkLauncher: DeeplinkLauncher,
) : Model() {

    private val params = paramsContainer.require<MarketingBannerComponent.Params>()
    private val dismissedIds = MutableStateFlow<Set<Int>>(emptySet())

    val uiState: StateFlow<MarketingBannerListUM>
        field = MutableStateFlow<MarketingBannerListUM>(MarketingBannerListUM.Hidden)

    init {
        observeBanners()
    }

    fun onBannerClick(deeplink: String?) {
        if (deeplink.isNullOrBlank()) return
        // Let the host route contextual deeplinks (swap/buy for the current token). Fall back to the
        // generic launcher for external links and when no interceptor is provided.
        val isHandledByHost = (params as? MarketingBannerComponent.Params.Standalone)
            ?.onDeeplinkClick?.invoke(deeplink) == true
        if (!isHandledByHost) deeplinkLauncher.launch(deeplink)
    }

    fun onDismiss(campaignId: Int) {
        dismissedIds.update { it + campaignId }
        modelScope.launch { dismissMarketingBanner(campaignId) }
    }

    private fun observeBanners() {
        val requestFlow: Flow<MarketingRequest?> = when (val p = params) {
            is MarketingBannerComponent.Params.Standalone ->
                p.requestFlow.map { request ->
                    request?.let { MarketingRequest(screen = it.screen, amountUsd = it.amountUsd) }
                }
            is MarketingBannerComponent.Params.Linked ->
                p.requestFlow.map { request ->
                    request?.let { MarketingRequest(screen = it.screen, amountUsd = it.amountUsd) }
                }
        }

        val campaigns: Flow<List<MarketingCampaign>> = requestFlow
            .map { it?.screen }
            .distinctUntilChanged()
            .debounce(REQUEST_DEBOUNCE_MS)
            .mapLatest { screen -> if (screen != null) fetch(screen) else emptyList() }

        val amountUsd: Flow<BigDecimal?> = requestFlow.map { it?.amountUsd }.distinctUntilChanged()

        modelScope.launch {
            combine(
                flow = campaigns,
                flow2 = amountUsd,
                flow3 = dismissedIds,
            ) { list, usd, dismissed ->
                list.asSequence()
                    .filterNot { it.id in dismissed }
                    .filter { it.matchesUsdAmount(usd) }
                    .filter { matchesUiType(it) }
                    .map { it.toUM() }
                    .toList()
            }.collect { banners ->
                uiState.value = if (banners.isEmpty()) {
                    MarketingBannerListUM.Hidden
                } else {
                    MarketingBannerListUM.Content(banners.toImmutableList())
                }
            }
        }
    }

    private suspend fun fetch(screen: MarketingScreen): List<MarketingCampaign> =
        getMarketingBanner(screen, amountUsd = null).getOrElse { emptyList() }

    private fun matchesUiType(campaign: MarketingCampaign): Boolean = when (params) {
        is MarketingBannerComponent.Params.Standalone ->
            campaign.banner.uiType == MarketingBanner.UiType.STANDALONE
        is MarketingBannerComponent.Params.Linked ->
            campaign.banner.uiType == MarketingBanner.UiType.LINKED_TO_PROVIDER
    }

    private data class MarketingRequest(
        val screen: MarketingScreen,
        val amountUsd: BigDecimal?,
    )

    private fun MarketingCampaign.toUM() = MarketingBannerUM(
        campaignId = id,
        text = banner.text,
        iconUrl = banner.iconUrl,
        // When the backend omits iconAlign, follow the design default: a dismissible banner keeps the icon
        // on the left (the close button occupies the right slot), a non-dismissible one moves it to the right.
        iconAlign = when (banner.iconAlign) {
            MarketingBanner.IconAlign.RIGHT -> MarketingBannerUM.IconAlign.RIGHT
            MarketingBanner.IconAlign.LEFT -> MarketingBannerUM.IconAlign.LEFT
            null -> if (banner.isDismissible) MarketingBannerUM.IconAlign.LEFT else MarketingBannerUM.IconAlign.RIGHT
        },
        isDismissible = banner.isDismissible,
        deeplink = banner.deeplink,
        providerIds = providerIds?.toSet().orEmpty(),
    )

    private companion object {
        const val REQUEST_DEBOUNCE_MS = 300L
    }
}