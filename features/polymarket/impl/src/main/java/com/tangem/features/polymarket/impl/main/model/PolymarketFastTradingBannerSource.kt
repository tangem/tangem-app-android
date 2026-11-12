package com.tangem.features.polymarket.impl.main.model

import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.interactor.PolymarketSessionKeyOverview
import com.tangem.domain.polymarket.model.PolymarketFastTradingState
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.newestPerAddress
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerKind
import com.tangem.domain.polymarket.sessionkey.PolymarketFastTradingBannerStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyAssessor
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyReconciliation
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationRequest
import com.tangem.domain.polymarket.sessionkey.operation.PolymarketSessionKeyOperationStart
import com.tangem.domain.polymarket.usecase.ObservePolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.RefreshPolymarketFastTradingUseCase
import com.tangem.domain.polymarket.usecase.StartPolymarketSessionKeyOperationUseCase
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

private val PENDING_SETTLING_WINDOW: Duration = Duration.ofMinutes(10)
private val DAY: Duration = Duration.ofDays(1)

private typealias Dismissals = Map<PolymarketFastTradingBannerKind, Instant?>

@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class PolymarketFastTradingBannerSource(
    private val observeFastTrading: ObservePolymarketFastTradingUseCase,
    private val refreshFastTrading: RefreshPolymarketFastTradingUseCase,
    private val startOperation: StartPolymarketSessionKeyOperationUseCase,
    private val bannerStore: PolymarketFastTradingBannerStore,
    private val featureToggles: PolymarketFeatureToggles,
    private val clock: Clock,
) {

    @Inject
    constructor(
        observeFastTrading: ObservePolymarketFastTradingUseCase,
        refreshFastTrading: RefreshPolymarketFastTradingUseCase,
        startOperation: StartPolymarketSessionKeyOperationUseCase,
        bannerStore: PolymarketFastTradingBannerStore,
        featureToggles: PolymarketFeatureToggles,
    ) : this(
        observeFastTrading = observeFastTrading,
        refreshFastTrading = refreshFastTrading,
        startOperation = startOperation,
        bannerStore = bannerStore,
        featureToggles = featureToggles,
        clock = Clock.systemUTC(),
    )

    fun observe(userWalletId: UserWalletId, rechecks: Flow<Unit>): Flow<PolymarketFastTradingBanner?> {
        if (!featureToggles.isPolymarketFastOrdersEnabled) return flowOf(null)

        return combine(
            flow = observeFastTrading(userWalletId),
            flow2 = observeDismissals(userWalletId),
            flow3 = rechecks,
        ) { state, dismissals, _ -> state to dismissals }
            .transformLatest { (state, dismissals) ->
                var now = clock.instant()
                while (true) {
                    emit(select(state = state, dismissals = dismissals, now = now))
                    val boundary = nextBoundary(state, dismissals, now) ?: break
                    delay(Duration.between(now, boundary).toMillis())
                    now = boundary
                }
            }
            .distinctUntilChanged()
    }

    suspend fun refresh(userWalletId: UserWalletId) {
        if (featureToggles.isPolymarketFastOrdersEnabled) refreshFastTrading(userWalletId)
    }

    fun start(userWalletId: UserWalletId, banner: PolymarketFastTradingBanner): PolymarketSessionKeyOperationStart? {
        val request = when (banner) {
            PolymarketFastTradingBanner.UnknownSigners -> return null
            PolymarketFastTradingBanner.Activate -> PolymarketSessionKeyOperationRequest.Activate
            is PolymarketFastTradingBanner.Reactivate -> banner.request
            is PolymarketFastTradingBanner.Extend -> PolymarketSessionKeyOperationRequest.Extend
        }
        return startOperation(userWalletId, request)
    }

    suspend fun dismiss(userWalletId: UserWalletId, banner: PolymarketFastTradingBanner) {
        val kind = banner.dismissibleKind() ?: return
        bannerStore.dismiss(userWalletId = userWalletId, kind = kind, at = clock.instant())
    }

    private fun observeDismissals(userWalletId: UserWalletId): Flow<Dismissals> = combine(
        PolymarketFastTradingBannerKind.entries.map { kind ->
            bannerStore.dismissedAt(userWalletId, kind).map { dismissedAt -> kind to dismissedAt }
        },
    ) { dismissals -> dismissals.toMap() }

    private fun select(
        state: PolymarketFastTradingState,
        dismissals: Dismissals,
        now: Instant,
    ): PolymarketFastTradingBanner? {
        val overview = state.overview.getOrNull() ?: return null
        if (overview.hasForeignSigners()) return PolymarketFastTradingBanner.UnknownSigners
        if (state.operation != null) return null
        if (overview.wallet.sessionKeys.newestPerAddress().any { it.isSettlingAt(now) }) return null

        val banner = offerFor(overview.assessment.deviceState, now) ?: return null
        val kind = banner.dismissibleKind() ?: return banner
        return banner.takeUnless { kind.isDismissed(dismissals[kind], now) }
    }

    private fun offerFor(deviceState: PolymarketSessionKeyDeviceState, now: Instant): PolymarketFastTradingBanner? =
        when (deviceState) {
            PolymarketSessionKeyDeviceState.OfferEnable,
            is PolymarketSessionKeyDeviceState.Replace,
            -> PolymarketFastTradingBanner.Activate
            is PolymarketSessionKeyDeviceState.Reenable -> reactivationOffer(
                sessionKey = deviceState.sessionKey,
                request = PolymarketSessionKeyOperationRequest.Reactivate,
            )
            is PolymarketSessionKeyDeviceState.RepairByRenewal -> reactivationOffer(
                sessionKey = deviceState.sessionKey,
                request = PolymarketSessionKeyOperationRequest.Activate,
            )
            is PolymarketSessionKeyDeviceState.Active,
            is PolymarketSessionKeyDeviceState.Disabled,
            is PolymarketSessionKeyDeviceState.RenewalRequired,
            -> deviceState.extendableKey()?.let { extensionOffer(it, now) }
            is PolymarketSessionKeyDeviceState.Enabling,
            is PolymarketSessionKeyDeviceState.Revoking,
            is PolymarketSessionKeyDeviceState.WaitingForPrevious,
            -> null
        }

    private fun reactivationOffer(
        sessionKey: PolymarketSessionKey,
        request: PolymarketSessionKeyOperationRequest,
    ): PolymarketFastTradingBanner = if (sessionKey.status == PolymarketSessionKeyStatus.FAILED) {
        PolymarketFastTradingBanner.Activate
    } else {
        PolymarketFastTradingBanner.Reactivate(request)
    }

    private fun extensionOffer(sessionKey: PolymarketSessionKey, now: Instant): PolymarketFastTradingBanner? {
        val validUntil = sessionKey.validUntil ?: return null
        if (Duration.between(now, validUntil) > PolymarketSessionKeyAssessor.EXTENSION_OFFER_WINDOW) return null
        return daysLeft(sessionKey, now)?.let(PolymarketFastTradingBanner::Extend)
    }

    private fun daysLeft(sessionKey: PolymarketSessionKey, now: Instant): Int? {
        val remaining = sessionKey.validUntil?.let { Duration.between(now, it) } ?: return null
        if (remaining <= Duration.ZERO) return null
        val wholeDays = remaining.toDays()
        val hasPartialDay = remaining > DAY.multipliedBy(wholeDays)
        return (if (hasPartialDay) wholeDays + 1 else wholeDays).toInt()
    }

    private fun nextBoundary(state: PolymarketFastTradingState, dismissals: Dismissals, now: Instant): Instant? {
        val overview = state.overview.getOrNull()
        val pendingEnds = overview?.wallet?.sessionKeys.orEmpty().newestPerAddress()
            .filter { it.status == PolymarketSessionKeyStatus.PENDING }
            .map { it.createdAt.plus(PENDING_SETTLING_WINDOW) }
        val dismissalEnds = dismissals.mapNotNull { (kind, dismissedAt) -> dismissedAt?.plus(kind.dismissalPeriod) }
        val extensionBoundary = overview?.assessment?.deviceState?.extendableKey()?.let { extensionBoundary(it, now) }
        return (pendingEnds + dismissalEnds + listOfNotNull(extensionBoundary)).filter { it > now }.minOrNull()
    }

    private fun extensionBoundary(sessionKey: PolymarketSessionKey, now: Instant): Instant? {
        val validUntil = sessionKey.validUntil ?: return null
        val windowStart = validUntil.minus(PolymarketSessionKeyAssessor.EXTENSION_OFFER_WINDOW)
        if (now < windowStart) return windowStart
        val daysLeft = daysLeft(sessionKey, now) ?: return null
        return validUntil.minus(DAY.multipliedBy(daysLeft - 1L))
    }

    private fun PolymarketSessionKeyDeviceState.extendableKey(): PolymarketSessionKey? = when (this) {
        is PolymarketSessionKeyDeviceState.Active -> sessionKey
        is PolymarketSessionKeyDeviceState.Disabled -> sessionKey
        is PolymarketSessionKeyDeviceState.RenewalRequired -> sessionKey
        else -> null
    }

    private fun PolymarketSessionKeyOverview.hasForeignSigners(): Boolean {
        val reconciliation = assessment.reconciliation as? PolymarketSessionKeyReconciliation.Checked ?: return false
        return reconciliation.foreignSigners.any { !it.address.equals(localKeyAddress, ignoreCase = true) }
    }

    private fun PolymarketSessionKey.isSettlingAt(now: Instant): Boolean = when (status) {
        PolymarketSessionKeyStatus.REVOKING -> true
        PolymarketSessionKeyStatus.PENDING -> now < createdAt.plus(PENDING_SETTLING_WINDOW)
        else -> false
    }
}

private fun PolymarketFastTradingBanner.dismissibleKind(): PolymarketFastTradingBannerKind? = when (this) {
    PolymarketFastTradingBanner.UnknownSigners -> null
    PolymarketFastTradingBanner.Activate -> PolymarketFastTradingBannerKind.ACTIVATE
    is PolymarketFastTradingBanner.Reactivate -> PolymarketFastTradingBannerKind.REACTIVATE
    is PolymarketFastTradingBanner.Extend -> PolymarketFastTradingBannerKind.EXTEND
}