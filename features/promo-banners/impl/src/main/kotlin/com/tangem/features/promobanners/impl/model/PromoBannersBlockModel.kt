package com.tangem.features.promobanners.impl.model

import com.tangem.core.navigation.deeplink.DeeplinkLauncher
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import com.tangem.features.promobanners.impl.analytics.PromoBannerAnalyticsEvent
import com.tangem.features.promobanners.impl.converters.PromoBannerDisplayToNotificationConverter
import com.tangem.features.promobanners.impl.repository.PromoBannersRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import kotlinx.collections.immutable.persistentListOf
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    private var isVisibleOnScreen: Boolean = params.isInitiallyVisibleOnScreen
    private var wasCarouselScrolled = false
    private val savedDisplayIdByWalletId: MutableMap<String, Int> = mutableMapOf()

    val uiState: StateFlow<PromoBannersBlockUM>
        field = MutableStateFlow(getInitialState())

    init {
        subscribeOnSelectedWallet()
    }

    fun setVisibleOnScreen(visible: Boolean) {
        isVisibleOnScreen = visible
        uiState.update { it.copy(isVisibleOnScreen = visible) }
    }

    private fun subscribeOnSelectedWallet() {
        modelScope.launch {
            userWalletsListRepository.selectedUserWallet
                .filterNotNull()
                .map { it.walletId.stringValue }
                .distinctUntilChanged()
                .onEach {
                    wasCarouselScrolled = false
                }
                .collectLatest { walletId -> loadBanners(walletId) }
        }
    }

    private suspend fun loadBanners(walletId: String) {
        val languageISOCode = Locale.getDefault().language

        runSuspendCatching {
            repository.getBanners(walletId, params.placeholder, languageISOCode)
        }.onSuccess { banners ->
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

            uiState.value = PromoBannersBlockUM(
                userWalletId = walletId,
                initialPage = initialPage,
                banners = bannerUMs,
                isVisibleOnScreen = isVisibleOnScreen,
                placeholder = params.placeholder,
                onBannerShown = { displayId -> onBannerShown(walletId, displayId) },
                onCarouselScrolled = ::onCarouselScrolled,
                onPageChanged = { displayId -> savedDisplayIdByWalletId[walletId] = displayId },
            )
        }.onFailure { error ->
            TangemLogger.w("Failed to load promo banners", error)
        }
    }

    private fun onBannerShown(walletId: String, displayId: Int) {
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
        deeplink?.let { deeplinkLauncher.launch(it) }
    }

    private fun getInitialState() = PromoBannersBlockUM(
        userWalletId = "",
        initialPage = 0,
        banners = persistentListOf(),
        isVisibleOnScreen = isVisibleOnScreen,
        placeholder = params.placeholder,
        onBannerShown = {},
        onCarouselScrolled = {},
        onPageChanged = {},
    )

    private fun onBannerDismiss(walletId: String, displayId: Int) {
        analyticsEventHandler.send(PromoBannerAnalyticsEvent.Dismissed(displayId, placeholderName))
        uiState.update { state ->
            state.copy(
                banners = state.banners
                    .filterNot { it.displayId == displayId }
                    .toImmutableList(),
            )
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
}