package com.tangem.tap.domain.scanCard.chains

import com.tangem.domain.common.ScanResponse
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.common.successOr
import com.tangem.tap.domain.scanCard.ScanChainError
import com.tangem.tap.features.onboarding.OnboardingHelper
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsAction
import com.tangem.tap.features.onboarding.products.twins.redux.TwinCardsStep
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store
import kotlinx.coroutines.delay

abstract class BaseCardScannedChain : ScanCardChain {

    protected suspend inline fun navigateTo(screen: AppScreen): ChainResult.Failure {
        delay(DELAY_SDK_DIALOG_CLOSE)
        store.dispatchOnMain(NavigationAction.NavigateTo(screen))
        return ChainResult.Failure(ScanChainError.InterruptBy.Navigation(screen))
    }
}

class CardScannedChain(
    private val onWalletNotCreated: suspend () -> Unit = {},
) : BaseCardScannedChain() {

    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        val scanResponse = data.successOr { return data }

        if (OnboardingHelper.isOnboardingCase(scanResponse)) {
            onWalletNotCreated()
            store.dispatchOnMain(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = true))
            val appScreen = OnboardingHelper.whereToNavigate(scanResponse)
            return navigateTo(appScreen)
        } else {
            if (scanResponse.twinsIsTwinned() && !preferencesStorage.wasTwinsOnboardingShown()) {
                onWalletNotCreated()
                store.dispatchOnMain(TwinCardsAction.SetStepOfScreen(TwinCardsStep.WelcomeOnly(scanResponse)))
                return navigateTo(AppScreen.OnboardingTwins)
            }
        }

        return data
    }
}
