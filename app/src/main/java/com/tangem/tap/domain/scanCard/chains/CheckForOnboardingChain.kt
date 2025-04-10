package com.tangem.tap.domain.scanCard.chains

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.Analytics
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.common.TapWorkarounds.canSkipBackup
import com.tangem.domain.common.util.twinsIsTwinned
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ResultChain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.addContext
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.extensions.setContext
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.features.onboarding.products.wallet.redux.BackupStartedSource
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import org.rekotlin.Store

/**
 * Handles the verification process to determine if the scanned card requires onboarding.
 *
 * Returns:
 * - [ScanChainException.OnboardingNeeded] if onboarding required.
 *
 * @param store the [Store] that holds the state of the app.
 *
 * @see Chain for more information about the Chain interface.
 */
class CheckForOnboardingChain(
    private val store: Store<AppState>,
) : ResultChain<ScanCardException, ScanResponse>() {

    override suspend fun launch(previousChainResult: ScanResponse): ScanChainResult {
        store.dispatchOnMain(TwinCardsAction.IfTwinsPrepareState(previousChainResult))

        return when {
            OnboardingHelper.isOnboardingCase(previousChainResult) -> {
                Analytics.addContext(previousChainResult)
                // must check skip backup using card canSkipBackup
                store.dispatchOnMain(
                    GlobalAction.Onboarding.Start(
                        scanResponse = previousChainResult,
                        source = BackupStartedSource.Onboarding,
                        canSkipBackup = previousChainResult.card.canSkipBackup,
                    ),
                )
                val route = OnboardingHelper.whereToNavigate(previousChainResult)
                ScanChainException.OnboardingNeeded(route).left()
            }
            else -> {
                Analytics.setContext(previousChainResult)

                val isTwinRefactoringEnabled =
                    store.inject(DaggerGraphState::onboardingV2FeatureToggles).isTwinRefactoringEnabled

                val wasTwinsOnboardingShown = store.inject(DaggerGraphState::wasTwinsOnboardingShownUseCase)
                    .invokeSync()

                // If twins was twinned previously but twins welcome not shown
                if (previousChainResult.twinsIsTwinned() && !wasTwinsOnboardingShown) {
                    if (isTwinRefactoringEnabled) {
                        ScanChainException.OnboardingNeeded(
                            AppRoute.Onboarding(
                                scanResponse = previousChainResult,
                                mode = AppRoute.Onboarding.Mode.WelcomeOnlyTwin,
                            ),
                        ).left()
                    } else {
                        store.dispatchOnMain(
                            TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly(previousChainResult)),
                        )
                        ScanChainException.OnboardingNeeded(AppRoute.OnboardingTwins).left()
                    }
                } else {
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    previousChainResult.right()
                }
            }
        }
    }
}