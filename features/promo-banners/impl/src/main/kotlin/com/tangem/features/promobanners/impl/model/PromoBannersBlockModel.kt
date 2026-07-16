package com.tangem.features.promobanners.impl.model

import androidx.core.net.toUri
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.promobanners.impl.analytics.PromoBannerAnalyticsEvent
import com.tangem.features.promobanners.impl.converters.PromoBannerDisplayToNotificationConverter
import com.tangem.features.promobanners.impl.repository.PromoBannersRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

private typealias ShownBannerKey = Pair<String, Int>

@ModelScoped
internal class PromoBannersBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val repository: PromoBannersRepository,
    private val deeplinkLauncher: DeeplinkLauncher,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val params = paramsContainer.require<PromoBannersBlockComponent.Params>()
    private val converter = PromoBannerDisplayToNotificationConverter()

    private val placeholderName: String = params.placeholder.value
    private val shownBannerIds: MutableSet<ShownBannerKey> = ConcurrentHashMap.newKeySet()
    private var wasCarouselScrolled = false
    private val savedDisplayIdByWalletId: MutableMap<String, Int> = mutableMapOf()
    private val prefetchedWalletIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val prefetchSemaphore = Semaphore(permits = PREFETCH_PARALLELISM)

    /** Raw banners per wallet, filled from cache/prefetch/network. Source of truth for the UI. */
    private val rawBannersByWalletId = MutableStateFlow<Map<String, List<PromoBannerDisplay>>>(emptyMap())

    private val selectedWalletId = MutableStateFlow<String?>(null)

    private val screenVisible = MutableStateFlow(params.isInitiallyVisibleOnScreen)

    private val baseBannerStates: StateFlow<Map<String, PromoBannersBlockUM>> =
        rawBannersByWalletId
            .map { rawByWallet ->
                rawByWallet.mapValues { (walletId, banners) -> buildState(walletId, banners, isVisible = false) }
            }
            .stateIn(modelScope, SharingStarted.Eagerly, emptyMap())

    /**
     * Per-wallet UI states, so a pager page can render its own wallet's banners synchronously from
     * cache as it slides in — instead of the whole block waiting for the wallet selection to settle.
     * Only cheaply toggles [PromoBannersBlockUM.isVisibleOnScreen] on selection/visibility change.
     */
    val bannerStates: StateFlow<Map<String, PromoBannersBlockUM>> =
        combine(baseBannerStates, selectedWalletId, screenVisible) { base, selected, visible ->
            base.mapValues { (walletId, state) ->
                // Only the active wallet is "visible on screen" for analytics purposes; pre-composed
                // off-screen pager pages must not emit "banner shown" events.
                val shouldBeVisible = visible && walletId == selected
                if (state.isVisibleOnScreen == shouldBeVisible) {
                    state
                } else {
                    state.copy(
                        isVisibleOnScreen = shouldBeVisible,
                    )
                }
            }
        }.stateIn(modelScope, SharingStarted.Eagerly, emptyMap())

    val uiState: StateFlow<PromoBannersBlockUM> =
        combine(bannerStates, selectedWalletId) { states, selected ->
            states[selected] ?: getInitialState()
        }.stateIn(modelScope, SharingStarted.Eagerly, getInitialState())

    init {
        subscribeOnSelectedWallet()
        prefetchAllWallets()
    }

    fun setVisibleOnScreen(visible: Boolean) {
        screenVisible.value = visible
    }

    private fun subscribeOnSelectedWallet() {
        modelScope.launch {
            userWalletsListRepository.selectedUserWallet
                .filterNotNull()
                .map { it.walletId.stringValue }
                .distinctUntilChanged()
                .collectLatest { walletId ->
                    wasCarouselScrolled = false
                    selectedWalletId.value = walletId
                    loadWallet(walletId)
                }
        }
    }

    /**
     * Warms the banners for every wallet in the background so switching to another wallet renders its
     * banners instantly from cache instead of after a network gap. Each wallet is fetched at most once
     * ([prefetchedWalletIds]); [PromoBannersRepository.getBanners] is a no-op once the wallet is cached.
     */
    private fun prefetchAllWallets() {
        modelScope.launch {
            val languageISOCode = Locale.getDefault().language
            userWalletsListRepository.userWallets
                .filterNotNull()
                .collect { wallets ->
                    wallets.forEach { wallet ->
                        val walletId = wallet.walletId.stringValue
                        if (prefetchedWalletIds.add(walletId)) {
                            modelScope.launch {
                                prefetchSemaphore.withPermit {
                                    runSuspendCatching {
                                        repository.getBanners(walletId, params.placeholder, languageISOCode)
                                    }.onSuccess { putBanners(walletId, it) }
                                        .onFailure { prefetchedWalletIds.remove(walletId) }
                                }
                            }
                        }
                    }
                }
        }
    }

    private suspend fun loadWallet(walletId: String) {
        val cached = runSuspendCatching { repository.getCachedBanners(walletId, params.placeholder) }.getOrNull()
        if (cached != null) {
            putBanners(walletId, cached)
            return
        }

        putBanners(walletId, banners = emptyList())
        runSuspendCatching {
            repository.getBanners(walletId, params.placeholder, Locale.getDefault().language)
        }.onSuccess { banners ->
            putBanners(walletId, banners)
        }.onFailure { error ->
            TangemLogger.w("Failed to load promo banners", error)
        }
    }

    private fun putBanners(walletId: String, banners: List<PromoBannerDisplay>) {
        rawBannersByWalletId.update { it + (walletId to banners) }
    }

    private fun buildState(
        walletId: String,
        banners: List<PromoBannerDisplay>,
        isVisible: Boolean,
    ): PromoBannersBlockUM {
        val bannerUMs = banners.map { banner ->
            converter.convert(
                banner = banner,
                onDeeplinkClick = { deeplink -> onButtonClick(banner.id, deeplink) },
                onDismiss = { displayId -> onBannerDismiss(walletId, displayId) },
            )
        }.toImmutableList()

        val savedDisplayId = savedDisplayIdByWalletId[walletId]
        val initialPage = if (savedDisplayId != null) {
            bannerUMs.indexOfFirst { it.displayId == savedDisplayId }.coerceAtLeast(0)
        } else {
            0
        }

        return PromoBannersBlockUM(
            userWalletId = walletId,
            initialPage = initialPage,
            banners = bannerUMs,
            isVisibleOnScreen = isVisible,
            placeholder = params.placeholder,
            onBannerShown = { displayId -> onBannerShown(walletId, displayId) },
            onCarouselScrolled = ::onCarouselScrolled,
            onPageChanged = { displayId -> savedDisplayIdByWalletId[walletId] = displayId },
        )
    }

    private fun onBannerShown(walletId: String, displayId: Int) {
        if (walletId != selectedWalletId.value) return
        if (shownBannerIds.add(walletId to displayId)) {
            analyticsEventHandler.send(PromoBannerAnalyticsEvent.Shown(displayId, placeholderName))
        }
    }

    private fun onCarouselScrolled(displayId: Int) {
        if (!wasCarouselScrolled) {
            wasCarouselScrolled = true
            analyticsEventHandler.send(PromoBannerAnalyticsEvent.CarouselScrolled(displayId, placeholderName))
        }
    }

    private fun onButtonClick(displayId: Int, deeplink: String?) {
        analyticsEventHandler.send(PromoBannerAnalyticsEvent.Clicked(displayId, placeholderName))
        deeplink?.let { deeplinkLauncher.launch(appendSurveyDisplayId(it, displayId)) }
    }

    private fun appendSurveyDisplayId(deeplink: String, displayId: Int): String {
        val uri = deeplink.toUri()
        val isSurveyDeeplink = uri.scheme == DEEPLINK_SCHEME_TANGEM && uri.host == DEEPLINK_HOST_SURVEY
        if (!isSurveyDeeplink || uri.getQueryParameter(QUERY_DISPLAY_ID) != null) return deeplink

        return uri.buildUpon()
            .appendQueryParameter(QUERY_DISPLAY_ID, displayId.toString())
            .build()
            .toString()
    }

    private fun getInitialState() = PromoBannersBlockUM(
        userWalletId = "",
        initialPage = 0,
        banners = persistentListOf(),
        isVisibleOnScreen = screenVisible.value,
        placeholder = params.placeholder,
        onBannerShown = {},
        onCarouselScrolled = {},
        onPageChanged = {},
    )

    private fun onBannerDismiss(walletId: String, displayId: Int) {
        analyticsEventHandler.send(PromoBannerAnalyticsEvent.Dismissed(displayId, placeholderName))
        rawBannersByWalletId.update { byWallet ->
            val banners = byWallet[walletId] ?: return@update byWallet
            byWallet + (walletId to banners.filterNot { it.id == displayId })
        }
        modelScope.launch {
            runSuspendCatching {
                repository.dismissBanner(walletId, displayId)
            }.onFailure { error ->
                TangemLogger.w(
                    "Failed to dismiss promo banner $displayId for wallet $walletId",
                    error,
                )
            }
        }
    }

    private companion object {
        const val DEEPLINK_SCHEME_TANGEM = "tangem"
        const val DEEPLINK_HOST_SURVEY = "survey"
        const val QUERY_DISPLAY_ID = "display_id"
        const val PREFETCH_PARALLELISM = 3
    }
}