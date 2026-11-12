package com.tangem.features.polymarket.impl.fasttrading.banner

import com.tangem.core.decompose.navigation.Router
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.polymarket.impl.fasttrading.PolymarketFastTradingActivationComponent.Action
import com.tangem.features.polymarket.impl.fasttrading.banner.ui.PolymarketFastTradingBannerUM
import com.tangem.features.polymarket.impl.navigation.PolymarketRoute
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Suppress("LongParameterList")
internal class PolymarketFastTradingBannerController(
    private val scope: CoroutineScope,
    private val dispatchers: CoroutineDispatcherProvider,
    private val userWalletId: UserWalletId,
    private val source: PolymarketFastTradingBannerSource,
    private val router: Router,
    private val onUnknownSignersReview: (() -> Unit)?,
) {

    private val rechecks = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    private val subscriptionJob = JobHolder()

    val banner: StateFlow<PolymarketFastTradingBannerUM?>
        field = MutableStateFlow<PolymarketFastTradingBannerUM?>(null)

    fun subscribe() {
        source.observe(
            userWalletId = userWalletId,
            rechecks = rechecks,
            includesUnknownSigners = onUnknownSignersReview != null,
        )
            .onEach { value -> banner.value = value?.toUM() }
            .flowOn(dispatchers.default)
            .launchIn(scope)
            .saveIn(subscriptionJob)
    }

    fun resume() {
        rechecks.tryEmit(Unit)
        subscribe()
        scope.launch { source.refresh(userWalletId) }
    }

    fun pause() {
        subscriptionJob.cancel()
    }

    suspend fun refresh() {
        rechecks.tryEmit(Unit)
        source.refresh(userWalletId)
    }

    private fun PolymarketFastTradingBanner.toUM(): PolymarketFastTradingBannerUM? = when (this) {
        PolymarketFastTradingBanner.UnknownSigners -> onUnknownSignersReview?.let { onReview ->
            PolymarketFastTradingBannerUM.UnknownSigners(onActionClick = onReview)
        }
        PolymarketFastTradingBanner.Activate -> PolymarketFastTradingBannerUM.Activate(
            onActionClick = { openActivation(Action.Activate) },
            onCloseClick = { dismiss(this) },
        )
        is PolymarketFastTradingBanner.Reactivate -> PolymarketFastTradingBannerUM.Reactivate(
            onActionClick = { openActivation(Action.Reactivate(request)) },
            onCloseClick = { dismiss(this) },
        )
        is PolymarketFastTradingBanner.Extend -> PolymarketFastTradingBannerUM.Extend(
            daysLeft = daysLeft,
            onActionClick = { openActivation(Action.Extend) },
            onCloseClick = { dismiss(this) },
        )
    }

    private fun openActivation(action: Action) {
        router.push(PolymarketRoute.FastTradingActivation(userWalletId = userWalletId, action = action))
    }

    private fun dismiss(banner: PolymarketFastTradingBanner) {
        scope.launch { source.dismiss(userWalletId = userWalletId, banner = banner) }
    }
}