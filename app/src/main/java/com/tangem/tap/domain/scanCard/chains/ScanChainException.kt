package com.tangem.tap.domain.scanCard.chains

import com.tangem.common.routing.AppRoute
import com.tangem.domain.card.ScanCardException

sealed class ScanChainException : ScanCardException.ChainException() {

    /**
     * May be returned from [DisclaimerChain]
     * */
    data object DisclaimerWasCanceled : ScanChainException() {

        @Suppress("UnusedPrivateMember")
        private fun readResolve(): Any = DisclaimerWasCanceled
    }

    /**
     * May be returned from [CheckForOnboardingChain]
     *
     * @param onboardingRoute route where to navigate
     * */
    data class OnboardingNeeded(val onboardingRoute: AppRoute) : ScanChainException()
}