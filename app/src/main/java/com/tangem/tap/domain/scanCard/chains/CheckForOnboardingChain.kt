package com.tangem.tap.domain.scanCard.chains

import arrow.core.left
import arrow.core.right
import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.utils.TrackingContextProxy
import com.tangem.domain.card.ScanCardException
import com.tangem.domain.card.common.util.twinsIsTwinned
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ResultChain
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.WasTwinsOnboardingShownUseCase
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay

/**
 * Handles the verification process to determine if the scanned card requires onboarding.
 *
 * Returns:
 * - [ScanChainException.OnboardingNeeded] if onboarding required.
 *
 * @see Chain for more information about the Chain interface.
 */
class CheckForOnboardingChain(
    private val trackingContextProxy: TrackingContextProxy,
    private val wasTwinsOnboardingShownUseCase: WasTwinsOnboardingShownUseCase,
    private val onboardingHelper: OnboardingHelper,
) : ResultChain<ScanCardException, ScanResponse>() {

    override suspend fun launch(previousChainResult: ScanResponse): ScanChainResult {
        return when {
            onboardingHelper.isOnboardingCase(previousChainResult) -> {
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

                val wasTwinsOnboardingShown = wasTwinsOnboardingShownUseCase.invokeSync()

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