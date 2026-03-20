package com.tangem.features.promobanners.impl.model

import com.tangem.common.routing.LinkHandler
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
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@ModelScoped
internal class PromoBannersBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val repository: PromoBannersRepository,
    private val linkHandler: LinkHandler,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val userWalletsListRepository: UserWalletsListRepository,
) : Model() {

    private val params = paramsContainer.require<PromoBannersBlockComponent.Params>()
    private val converter = PromoBannerDisplayToNotificationConverter()

    private val placeholder: String = params.placeholder.name.lowercase()
    private val shownBannerIds: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var wasCarouselScrolled = false

    val uiState: StateFlow<PromoBannersBlockUM>
        field = MutableStateFlow(PromoBannersBlockUM())

    init {
        subscribeOnSelectedWallet()
    }

    private fun subscribeOnSelectedWallet() {
        modelScope.launch {
            userWalletsListRepository.selectedUserWallet
                .filterNotNull()
                .map { it.walletId.stringValue }
                .distinctUntilChanged()
                .onEach {
                    shownBannerIds.clear()
                    wasCarouselScrolled = false
                }
                .collectLatest { walletId -> loadBanners(walletId) }
        }
    }

    private suspend fun loadBanners(walletId: String) {
        val locale = Locale.getDefault().language

        runSuspendCatching {
            repository.getBanners(walletId, params.placeholder, locale)
        }.onSuccess { banners ->
            uiState.value = PromoBannersBlockUM(
                banners = banners.map { banner ->
                    converter.convert(
                        banner = banner,
                        onDeeplinkClick = { deeplink -> onButtonClick(banner.id, deeplink) },
                        onDismiss = { displayId -> onBannerDismiss(walletId, displayId) },
                    )
                }.toImmutableList(),
                onBannerShown = ::onBannerShown,
                onCarouselScrolled = ::onCarouselScrolled,
            )
        }.onFailure { error ->
            Timber.w(error, "Failed to load promo banners")
        }
    }

    private fun onBannerShown(displayId: String) {
        if (shownBannerIds.add(displayId)) {
            analyticsEventHandler.send(PromoBannerAnalyticsEvent.Shown(displayId, placeholder))
        }
    }

    private fun onCarouselScrolled(displayId: String) {
        if (!wasCarouselScrolled) {
            wasCarouselScrolled = true
            analyticsEventHandler.send(PromoBannerAnalyticsEvent.CarouselScrolled(displayId, placeholder))
        }
    }

    private fun onButtonClick(displayId: String, deeplink: String?) {
        analyticsEventHandler.send(PromoBannerAnalyticsEvent.Clicked(displayId, placeholder))
        deeplink?.let { linkHandler.navigate(it) }
    }

    private fun onBannerDismiss(walletId: String, displayId: String) {
        analyticsEventHandler.send(PromoBannerAnalyticsEvent.Dismissed(displayId, placeholder))
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
                Timber.w(
                    error,
                    "Failed to dismiss promo banner %s for wallet %s",
                    displayId,
                    walletId,
                )
            }
        }
    }
}