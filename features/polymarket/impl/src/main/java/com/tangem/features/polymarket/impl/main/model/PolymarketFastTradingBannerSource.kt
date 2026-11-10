package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.newestPerAddress
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerKind
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

private val PENDING_SETTLING_WINDOW: Duration = Duration.ofMinutes(10)

internal class PolymarketFastTradingBannerSource(
    private val observeFastTrading: ObservePolymarketFastTradingUseCase,
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase,
    private val bannerStore: PolymarketFastTradingBannerStore,
    private val featureToggles: PolymarketFeatureToggles,
    private val clock: Clock,
) {

    @Inject
    constructor(
        observeFastTrading: ObservePolymarketFastTradingUseCase,
        refreshFastTrading: RefreshPolymarketFastTradingUseCase,
        bannerStore: PolymarketFastTradingBannerStore,
        featureToggles: PolymarketFeatureToggles,
    ) : this(
        observeFastTrading = observeFastTrading,
        refreshFastTrading = refreshFastTrading,
        bannerStore = bannerStore,
        featureToggles = featureToggles,
        clock = Clock.systemUTC(),
    )

    fun observe(userWalletId: UserWalletId, rechecks: Flow<Unit>): Flow<PolymarketFastTradingBanner?> {
        if (!featureToggles.isPolymarketFastOrdersEnabled) return flowOf(null)

        return combine(
            flow = observeFastTrading(userWalletId),
            flow2 = bannerStore.dismissedAt(userWalletId, PolymarketFastTradingBannerKind.ACTIVATE),
            flow3 = rechecks,
        ) { state, activateDismissedAt, _ ->
            select(state = state, activateDismissedAt = activateDismissedAt, now = clock.instant())
        }.distinctUntilChanged()
    }

    suspend fun refresh(userWalletId: UserWalletId) {
        if (featureToggles.isPolymarketFastOrdersEnabled) refreshFastTrading(userWalletId)
    }

    suspend fun dismissActivation(userWalletId: UserWalletId) {
        bannerStore.dismiss(
            userWalletId = userWalletId,
            kind = PolymarketFastTradingBannerKind.ACTIVATE,
            at = clock.instant(),
        )
    }

    private fun select(
        state: PolymarketFastTradingState,
        activateDismissedAt: Instant?,
        now: Instant,
    ): PolymarketFastTradingBanner? {
        val overview = state.overview.getOrNull() ?: return null

        val reconciliation = overview.assessment.reconciliation as? PolymarketSessionKeyReconciliation.Checked
        if (!reconciliation?.foreignSigners.isNullOrEmpty()) return PolymarketFastTradingBanner.UnknownSigners

        val isSettling = overview.wallet.sessionKeys.newestPerAddress().any { it.isSettlingAt(now) }
        val isDismissed = PolymarketFastTradingBannerKind.ACTIVATE.isDismissed(activateDismissedAt, now)
        return PolymarketFastTradingBanner.Activate.takeIf {
            state.isActivationOffered && !isSettling && !isDismissed
        }
    }

    private fun PolymarketSessionKey.isSettlingAt(now: Instant): Boolean = when (status) {
        PolymarketSessionKeyStatus.REVOKING -> true
        PolymarketSessionKeyStatus.PENDING -> now < createdAt.plus(PENDING_SETTLING_WINDOW)
        else -> false
    }
}