package com.tangem.features.marketing.impl.model

import android.net.Uri
import arrow.core.getOrElse
import com.tangem.common.routing.deeplink.DeeplinkConst
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.domain.marketing.DismissMarketingBannerUseCase
import com.tangem.domain.marketing.GetMarketingBannerUseCase
import com.tangem.domain.marketing.MarketingUidBindingRepository
import com.tangem.domain.marketing.models.MarketingBanner
import com.tangem.domain.marketing.models.MarketingCampaign
import com.tangem.domain.marketing.models.MarketingScreen
import com.tangem.domain.marketing.models.hasAmountBounds
import com.tangem.domain.marketing.models.isActiveAt
import com.tangem.domain.marketing.models.matchesUsdAmount
import com.tangem.domain.marketing.models.nextWindowTransition
import com.tangem.domain.marketing.models.validateDates
import com.tangem.domain.quotes.GetFiatUSDRateUseCase
import com.tangem.feature.usedesk.api.UseDeskFeatureToggles
import com.tangem.features.marketing.api.MarketingBannerAmount
import com.tangem.features.marketing.api.MarketingBannerComponent
import com.tangem.features.marketing.impl.ui.state.MarketingBannerListUM
import com.tangem.features.marketing.impl.ui.state.MarketingBannerUM
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class MarketingBannerModel(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val getMarketingBanner: GetMarketingBannerUseCase,
    private val dismissMarketingBanner: DismissMarketingBannerUseCase,
    private val deeplinkLauncher: DeeplinkLauncher,
    private val getFiatUSDRate: GetFiatUSDRateUseCase,
    private val marketingUidBindingRepository: MarketingUidBindingRepository,
    private val appCoroutineScope: AppCoroutineScope,
    private val urlOpener: UrlOpener,
    private val useDeskFeatureToggles: UseDeskFeatureToggles,
    private val clock: Clock = Clock.systemUTC(),
) : Model() {

    private val params = paramsContainer.require<MarketingBannerComponent.Params>()
    private val dismissedIds = MutableStateFlow<Set<Int>>(emptySet())
    private val resumeSignal = MutableStateFlow(0L)

    /** How each fetched CTA campaign's link opens, by link. Classified once per fetch, not per amount change. */
    private var ctaLinks: Map<String?, CtaBannerLink> = emptyMap()

    /**
     * One lookup per currency, run in [modelScope] so a new amount awaits it instead of restarting it. A failed
     * lookup is dropped, so that currency is retried on its next amount change.
     */
    private val fiatUsdRateLookups = mutableMapOf<String, Deferred<BigDecimal?>>()

    val uiState: StateFlow<MarketingBannerListUM>
        field = MutableStateFlow<MarketingBannerListUM>(MarketingBannerListUM.Hidden)

    init {
        observeBanners()
    }

    @Inject
    constructor(
        dispatchers: CoroutineDispatcherProvider,
        paramsContainer: ParamsContainer,
        getMarketingBanner: GetMarketingBannerUseCase,
        dismissMarketingBanner: DismissMarketingBannerUseCase,
        deeplinkLauncher: DeeplinkLauncher,
        getFiatUSDRate: GetFiatUSDRateUseCase,
        marketingUidBindingRepository: MarketingUidBindingRepository,
        appCoroutineScope: AppCoroutineScope,
        urlOpener: UrlOpener,
        useDeskFeatureToggles: UseDeskFeatureToggles,
    ) : this(
        dispatchers = dispatchers,
        paramsContainer = paramsContainer,
        getMarketingBanner = getMarketingBanner,
        dismissMarketingBanner = dismissMarketingBanner,
        deeplinkLauncher = deeplinkLauncher,
        getFiatUSDRate = getFiatUSDRate,
        marketingUidBindingRepository = marketingUidBindingRepository,
        appCoroutineScope = appCoroutineScope,
        urlOpener = urlOpener,
        useDeskFeatureToggles = useDeskFeatureToggles,
        clock = Clock.systemUTC(),
    )

    fun onBannerClick(deeplink: String?) {
        val link = deeplink?.trim()
        if (link.isNullOrEmpty()) return
        if (ctaLinks[deeplink] == CtaBannerLink.PARTNER_PAGE) {
            bindMarketingUid(link)
            urlOpener.openUrl(link)
            return
        }
        // Let the host route contextual deeplinks (swap/buy for the current token). Fall back to the
        // generic launcher for external links and when no interceptor is provided.
        val isHandledByHost = (params as? MarketingBannerComponent.Params.Standalone)
            ?.onDeeplinkClick?.invoke(link) == true
        if (isHandledByHost) bindMarketingUid(link) else deeplinkLauncher.launch(link)
    }

    // A link handled by the host or opened as a partner page never reaches MainActivity, which binds the uid of
    // every other link
    private fun bindMarketingUid(deeplink: String) {
        val uid = Uri.parse(deeplink).getQueryParameters(DeeplinkConst.UID_KEY)
            .firstOrNull(String::isNotEmpty) ?: return
        appCoroutineScope.launch { marketingUidBindingRepository.bind(uid) }
    }

    fun onDismiss(campaignId: Int) {
        dismissedIds.update { it + campaignId }
        modelScope.launch { dismissMarketingBanner(campaignId) }
    }

    fun onResume() {
        resumeSignal.update { it + 1 }
    }

    private fun observeBanners() {
        val requestFlow: Flow<MarketingRequest?> = when (val p = params) {
            is MarketingBannerComponent.Params.Standalone ->
                p.requestFlow.map { request ->
                    request?.let { MarketingRequest(screen = it.screen, amount = it.amount) }
                }
            is MarketingBannerComponent.Params.Linked ->
                p.requestFlow.map { request ->
                    request?.let { MarketingRequest(screen = it.screen, amount = it.amount) }
                }
            is MarketingBannerComponent.Params.Cta ->
                p.requestFlow.map { request ->
                    request ?: return@map null
                    MarketingRequest(
                        screen = request.screen,
                        amount = request.amount,
                        isConciergeWaitlistHidden = request.isDemandSurveyCompleted,
                    )
                }
        }

        val campaigns: Flow<List<MarketingCampaign>> = observeCampaigns(requestFlow)
            .shareIn(scope = modelScope, started = SharingStarted.Lazily, replay = 1)

        val amount: Flow<MarketingBannerAmount?> = requestFlow.map { it?.amount }.distinctUntilChanged()

        val amountMatchingCampaigns: Flow<List<MarketingCampaign>> = combine(campaigns, amount, ::Pair)
            .transformLatest { (list, amount) ->
                if (list.none(MarketingCampaign::hasAmountBounds)) {
                    emit(list)
                    return@transformLatest
                }
                if (!isUsdAvailableNow(amount)) emit(list.filterNot(MarketingCampaign::hasAmountBounds))
                val usd = amount?.let { toUsd(it) }
                emit(list.filter { it.matchesUsdAmount(usd) })
            }

        val timeWindowTick = observeWindowTicks(campaigns)

        // Applied after the USD conversion, so hiding never waits for a rate lookup. Null while there is no request.
        val isConciergeWaitlistHidden: Flow<Boolean?> = requestFlow
            .map { it?.isConciergeWaitlistHidden }
            .distinctUntilChanged()

        modelScope.launch {
            combine(
                flow = amountMatchingCampaigns,
                flow2 = isConciergeWaitlistHidden,
                flow3 = dismissedIds,
                flow4 = timeWindowTick,
            ) { list, isWaitlistHidden, dismissed, now ->
                if (isWaitlistHidden == null) return@combine emptyList()
                list.asSequence()
                    .filterNot { it.id in dismissed }
                    .filterNot { isWaitlistHidden && isConciergeWaitlist(it) }
                    .filter { it.isActiveAt(now) }
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

    private fun observeWindowTicks(campaigns: Flow<List<MarketingCampaign>>): Flow<Instant> =
        combine(campaigns, resumeSignal) { list, _ -> list }
            .transformLatest { list ->
                while (true) {
                    val now = Instant.now(clock)
                    emit(now)
                    val nextTransition = list.nextWindowTransition(now) ?: break
                    val duration = Duration.between(now, nextTransition)
                    val delayMs = if (duration >= Duration.ofMillis(MAX_BOUNDARY_DELAY_MS)) {
                        MAX_BOUNDARY_DELAY_MS
                    } else {
                        duration.toMillis().coerceAtLeast(1L)
                    }
                    delay(delayMs)
                }
            }

    /**
     * The previous screen's campaigns are cleared at once, so they're never filtered with the new amount. A null
     * request only hides the banners: the host leaving its content and coming back to the same screen, e.g. on
     * refresh, reuses the campaigns instead of fetching them again.
     */
    private fun observeCampaigns(requestFlow: Flow<MarketingRequest?>): Flow<List<MarketingCampaign>> = requestFlow
        .mapNotNull { it?.screen }
        .distinctUntilChanged()
        .transformLatest { screen ->
            emit(emptyList())
            delay(REQUEST_DEBOUNCE_MS)
            val campaigns = fetch(screen).filter(::matchesUiType)
            campaigns.forEach { campaign ->
                campaign.validateDates { TangemLogger.e("Campaign ${campaign.id}: invalid date '$it' ignored") }
            }
            ctaLinks = classifyCtaLinks(campaigns)
            emit(campaigns.filter(::hasOpenableLink))
        }

    private suspend fun toUsd(amount: MarketingBannerAmount): BigDecimal? = when (amount) {
        is MarketingBannerAmount.Usd -> amount.value
        is MarketingBannerAmount.Fiat -> getFiatUsdRate(amount.currencyCode)?.let(amount.value::multiply)
    }

    private fun isUsdAvailableNow(amount: MarketingBannerAmount?): Boolean {
        if (amount !is MarketingBannerAmount.Fiat) return true
        return fiatUsdRateLookups[amount.currencyCode.uppercase()]?.isCompleted == true
    }

    /**
     * A failed lookup evicts itself, even when nobody awaits it any more. It starts lazily, so it is already in the
     * map when it completes.
     */
    private suspend fun getFiatUsdRate(currencyCode: String): BigDecimal? {
        val code = currencyCode.uppercase()
        val lookup = fiatUsdRateLookups.getOrPut(code) {
            modelScope.async(start = CoroutineStart.LAZY) {
                getFiatUSDRate(code).also { rate -> if (rate == null) fiatUsdRateLookups.remove(code) }
            }
        }
        return lookup.await()
    }

    private suspend fun fetch(screen: MarketingScreen): List<MarketingCampaign> =
        getMarketingBanner(screen, amountUsd = null).getOrElse { emptyList() }

    private fun matchesUiType(campaign: MarketingCampaign): Boolean = campaign.banner.uiType in when (params) {
        is MarketingBannerComponent.Params.Standalone -> setOf(MarketingBanner.UiType.STANDALONE)
        is MarketingBannerComponent.Params.Linked -> setOf(MarketingBanner.UiType.LINKED_TO_PROVIDER)
        is MarketingBannerComponent.Params.Cta -> CTA_UI_TYPES
    }

    private fun classifyCtaLinks(campaigns: List<MarketingCampaign>): Map<String?, CtaBannerLink> {
        if (params !is MarketingBannerComponent.Params.Cta) return emptyMap()
        val isSupportChatEnabled = useDeskFeatureToggles.isSupportChatDeepLinkEnabled
        return campaigns.map { it.banner.deeplink }.distinct().associateWith { link ->
            resolveCtaBannerLink(link?.trim(), isSupportChatEnabled)
        }
    }

    private fun hasOpenableLink(campaign: MarketingCampaign): Boolean =
        params !is MarketingBannerComponent.Params.Cta ||
            ctaLinks[campaign.banner.deeplink] != CtaBannerLink.UNSUPPORTED

    private fun isConciergeWaitlist(campaign: MarketingCampaign): Boolean =
        ctaLinks[campaign.banner.deeplink] == CtaBannerLink.CONCIERGE_WAITLIST

    private data class MarketingRequest(
        val screen: MarketingScreen,
        val amount: MarketingBannerAmount?,
        val isConciergeWaitlistHidden: Boolean = false,
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
        categoryLabel = banner.categoryLabel,
        subtitle = banner.subtitle,
        etaLabel = banner.etaLabel,
        partnerLabel = banner.partnerLabel,
        buttonText = banner.buttonText,
        hasGlow = banner.hasGlow,
        style = when (banner.uiType) {
            MarketingBanner.UiType.PSEUDO_PROVIDER -> MarketingBannerUM.Style.PSEUDO_PROVIDER
            MarketingBanner.UiType.STANDALONE_CTA -> MarketingBannerUM.Style.STANDALONE_CTA
            MarketingBanner.UiType.STANDALONE,
            MarketingBanner.UiType.LINKED_TO_PROVIDER,
            -> MarketingBannerUM.Style.DEFAULT
        },
    )

    private companion object {
        const val REQUEST_DEBOUNCE_MS = 300L
        const val MAX_BOUNDARY_DELAY_MS = 24 * 60 * 60 * 1000L
        val CTA_UI_TYPES = setOf(MarketingBanner.UiType.PSEUDO_PROVIDER, MarketingBanner.UiType.STANDALONE_CTA)
    }
}