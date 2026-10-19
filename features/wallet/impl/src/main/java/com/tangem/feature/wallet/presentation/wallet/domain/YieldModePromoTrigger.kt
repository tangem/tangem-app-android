package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.earn.repository.EarnRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldSupplyRepository
import com.tangem.features.wallet.deeplink.WalletDeepLinkActionListener
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics
import com.tangem.features.yield.supply.api.analytics.YieldSupplyAnalytics.SkipReason
import com.tangem.features.yield.supply.api.promobottomsheet.GetYieldModePromoVariantUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.ResolveYieldModePromoUseCase
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoOffer
import com.tangem.features.yield.supply.api.promobottomsheet.YieldModePromoResolution
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
internal class YieldModePromoTrigger @Inject constructor(
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
    private val yieldSupplyRepository: YieldSupplyRepository,
    private val earnRepository: EarnRepository,
    private val singleAccountStatusListSupplier: SingleAccountStatusListSupplier,
    private val resolveYieldModePromoUseCase: ResolveYieldModePromoUseCase,
    private val getYieldModePromoVariantUseCase: GetYieldModePromoVariantUseCase,
    private val walletDeepLinkActionListener: WalletDeepLinkActionListener,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private val isChecked = AtomicBoolean(false)

    /**
     * The check of the first main screen of a launch. Waits without a timeout until the offer is ready or the check
     * ends, whichever comes first, and returns null when it ends without the promo; once past the silent gates the
     * reason is sent as `Promo Screen Skipped`.
     *
     * @param yieldSupplyApy the catalog refreshed in this launch
     * @param isSurfaceBusy whether a dialog has been shown or an automatic bottom sheet scheduled on the main screen
     * @param isBusyOutsideSlot whether a bottom sheet that skips the dialog slot is going to be shown
     * @param interruptions the user's actions on the main screen and leaving it
     */
    suspend fun findOffer(
        userWalletId: UserWalletId,
        yieldSupplyApy: suspend () -> Either<Throwable, Map<String, BigDecimal>>,
        isSurfaceBusy: StateFlow<Boolean>,
        isBusyOutsideSlot: suspend () -> Boolean,
        interruptions: Flow<Unit>,
    ): YieldModePromoOffer? {
        if (!yieldSupplyFeatureToggles.isYieldModePromoEnabled) return null
        if (!isChecked.compareAndSet(false, true)) return null
        if (walletDeepLinkActionListener.isOpenedByLinkOrPush.value) return null
        if (yieldSupplyRepository.isYieldModePromoBottomSheetShown()) return null
        if (isSurfaceBusy.value || isBusyOutsideSlot()) return skip(SkipReason.SurfaceBusy)

        val endSignals = merge(
            isSurfaceBusy.filter { it }.map { SkipReason.SurfaceBusy },
            interruptions.map { SkipReason.Interrupted },
            walletDeepLinkActionListener.isOpenedByLinkOrPush.filter { it }.map { SkipReason.Interrupted },
        )

        return coroutineScope {
            // Latches the first end signal, so one that lands between the two waits still ends the check
            val end = MutableStateFlow<SkipReason?>(null)
            val endWatcher = launch { end.value = endSignals.first() }

            val result = either {
                val ready = untilEnd(end) { resolve(userWalletId, yieldSupplyApy) }.bind()
                ensureNotEnded(end, isSurfaceBusy)
                // Reading the variant may wait for the experiments to load, so the check can still end meanwhile
                val offer = untilEnd(end) { ready.offerFor(getYieldModePromoVariantUseCase()).right() }.bind()
                ensureNotEnded(end, isSurfaceBusy)
                offer
            }
            endWatcher.cancel()

            result.getOrElse(::skip)
        }
    }

    private suspend fun <T> untilEnd(
        end: StateFlow<SkipReason?>,
        block: suspend () -> Either<SkipReason, T>,
    ): Either<SkipReason, T> = merge<Either<SkipReason, T>>(
        flow { emit(block()) },
        end.filterNotNull().map { it.left() },
    ).first()

    private fun Raise<SkipReason>.ensureNotEnded(end: StateFlow<SkipReason?>, isSurfaceBusy: StateFlow<Boolean>) {
        end.value?.let { raise(it) }
        ensure(!isSurfaceBusy.value) { SkipReason.SurfaceBusy }
    }

    private suspend fun resolve(
        userWalletId: UserWalletId,
        yieldSupplyApy: suspend () -> Either<Throwable, Map<String, BigDecimal>>,
    ): Either<SkipReason, YieldModePromoResolution.Ready> {
        val earnOffers = earnRepository.getYieldOffers().getOrElse { return SkipReason.DataNotReady.left() }
        val apy = yieldSupplyApy().getOrElse { return SkipReason.DataNotReady.left() }

        // TODO: decide how a locked or single-currency wallet ends the check; a locked one waits to be interrupted
        return singleAccountStatusListSupplier(userWalletId)
            .mapNotNull { statusList ->
                val statuses = statusList.flattenPersonalCurrencies()
                when (val resolution = resolveYieldModePromoUseCase(statuses, apy, earnOffers)) {
                    YieldModePromoResolution.Waiting -> null
                    YieldModePromoResolution.NoEligibleToken -> SkipReason.NoEligibleToken.left()
                    YieldModePromoResolution.DataNotReady -> SkipReason.DataNotReady.left()
                    is YieldModePromoResolution.Ready -> resolution.right()
                }
            }
            .firstOrNull() ?: SkipReason.DataNotReady.left()
    }

    private fun skip(reason: SkipReason): YieldModePromoOffer? {
        analyticsEventHandler.send(YieldSupplyAnalytics.PromoScreenSkipped(reason))
        return null
    }
}