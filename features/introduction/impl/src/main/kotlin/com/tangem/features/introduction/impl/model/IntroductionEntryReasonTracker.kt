package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.analytics.repository.AnalyticsRepository
import com.tangem.domain.card.analytics.IntroductionProcess
import com.tangem.domain.card.analytics.IntroductionProcess.ScreenOpened.EntryReason
import com.tangem.utils.coroutines.runSuspendCatching
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The Welcome screen opens once per process as far as analytics is concerned: a viewer coming back from a sheet,
 * a story or an aborted flow gets the screen rebuilt, not reopened. Forgetting the last wallet is a new opening
 * even then.
 */
@Singleton
internal class IntroductionEntryReasonTracker @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val analyticsEventHandler: AnalyticsEventHandler,
) {

    private val hasReportedInThisProcess = AtomicBoolean(false)

    suspend fun reportOpening(routeReason: AppRoute.Home.EntryReason) {
        val entryReason = consume(routeReason) ?: return
        analyticsEventHandler.send(IntroductionProcess.ScreenOpened(entryReason))
    }

    private suspend fun consume(routeReason: AppRoute.Home.EntryReason): EntryReason? {
        val isFirstInThisProcess = hasReportedInThisProcess.compareAndSet(false, true)
        return when (routeReason) {
            AppRoute.Home.EntryReason.LastWalletForgotten -> {
                markShown()
                EntryReason.LastWalletForgotten
            }
            AppRoute.Home.EntryReason.Launch -> when {
                !isFirstInThisProcess -> null
                markShown() -> EntryReason.NoWallets
                else -> EntryReason.FirstLaunch
            }
        }
    }

    /** Records this showing and tells whether the screen had been shown on this installation before. */
    private suspend fun markShown(): Boolean = runSuspendCatching {
        analyticsRepository.checkIsEventSent(WELCOME_OPENED_EVENT_ID).also { wasShown ->
            if (!wasShown) analyticsRepository.setIsEventSent(WELCOME_OPENED_EVENT_ID)
        }
    }.getOrDefault(defaultValue = true)

    private companion object {
        const val WELCOME_OPENED_EVENT_ID = "introduction_welcome_opened"
    }
}