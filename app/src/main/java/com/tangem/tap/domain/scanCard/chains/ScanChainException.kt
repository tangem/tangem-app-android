package com.tangem.tap.domain.scanCard.chains

import com.tangem.domain.card.ScanCardException
import com.tangem.tap.common.redux.navigation.AppScreen

sealed class ScanChainException : ScanCardException.ChainException() {

    /**
     * May be returned from [CheckForOnboardingChain] and [CheckForUnfinishedSaltPayBackupChain]
     * */
    object PutSaltPayVisaCard : ScanChainException()

    /**
     * May be returned from [DisclaimerChain]
     * */
    object DisclaimerWasCanceled : ScanChainException()

    /**
     * May be returned from [CheckForOnboardingChain]
     *
     * @param onboardingRoute route where to navigate
     * */
    data class OnboardingNeeded(val onboardingRoute: AppScreen) : ScanChainException()

    /**
     * May be returned from [CheckForOnboardingChain]
     *
     * @param cause cause of exception
     * */
    data class CheckForSaltPayOnboardingCaseException(override val cause: Throwable?) : ScanChainException()
}