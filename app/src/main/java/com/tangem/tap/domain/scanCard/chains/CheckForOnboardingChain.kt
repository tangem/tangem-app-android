package com.tangem.tap.domain.scanCard.chains

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ResultChain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.features.onboarding.OnboardingHelper
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
        val trackingContextProxy = store.inject(DaggerGraphState::trackingContextProxy)

        return when {
            OnboardingHelper.isOnboardingCase(previousChainResult) -> {
                trackingContextProxy.addContext(previousChainResult)
                ScanChainException.OnboardingNeeded(
                    AppRoute.Onboarding(
                        scanResponse = previousChainResult,
                        mode = AppRoute.Onboarding.Mode.Onboarding,
                    ),
                ).left()
            }
            else -> {
                trackingContextProxy.setContext(previousChainResult)

                val wasTwinsOnboardingShown = store.inject(DaggerGraphState::wasTwinsOnboardingShownUseCase)
                    .invokeSync()

                // If twins was twinned previously but twins welcome not shown
                if (previousChainResult.twinsIsTwinned() && !wasTwinsOnboardingShown) {
                    ScanChainException.OnboardingNeeded(
                        AppRoute.Onboarding(
                            scanResponse = previousChainResult,
                            mode = AppRoute.Onboarding.Mode.WelcomeOnlyTwin,
                        ),
                    ).left()
                } else {
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    previousChainResult.right()
                }
            }
        }
    }
}