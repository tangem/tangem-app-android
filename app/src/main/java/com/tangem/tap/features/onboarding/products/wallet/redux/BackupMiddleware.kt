package com.tangem.tap.features.onboarding.products.wallet.redux

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.tap.*
import com.tangem.tap.common.analytics.events.Onboarding.*
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.demo.DemoHelper
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

class BackupMiddleware {
    val backupMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                if (action is BackupAction) handleBackupAction(state, action)
                next(action)
            }
        }
    }
}

@Suppress("LongMethod", "ComplexMethod", "MagicNumber")
private fun handleBackupAction(appState: () -> AppState?, action: BackupAction) {
    if (DemoHelper.tryHandle(appState, action)) return

    when (action) {
        is BackupAction.DiscardBackup -> {
            backupService.discardSavedBackup()
        }
        is BackupAction.DiscardSavedBackup -> {
            mainScope.launch {
                backupService.discardSavedBackup()

                val onboardingRepository = store.inject(DaggerGraphState::onboardingRepository)
                val cardRepository = store.inject(DaggerGraphState::cardRepository)
                val unfinishedBackup = onboardingRepository.getUnfinishedFinalizeOnboarding() ?: return@launch

                cardRepository.finishCardActivation(unfinishedBackup.card.cardId)
                onboardingRepository.clearUnfinishedFinalizeOnboarding()
                Analytics.send(Finished())
            }
        }
        is BackupAction.ResumeFoundUnfinishedBackup -> {
            if (action.unfinishedBackupScanResponse != null) {
                store.dispatchNavigationAction {
                    push(
                        AppRoute.Onboarding(
                            scanResponse = action.unfinishedBackupScanResponse,
                            mode = AppRoute.Onboarding.Mode.ContinueFinalize,
                        ),
                    )
                }
            }
        }
    }
}