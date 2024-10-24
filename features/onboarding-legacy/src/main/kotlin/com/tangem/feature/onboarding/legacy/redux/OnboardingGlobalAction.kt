package com.tangem.feature.onboarding.legacy.redux

import com.tangem.domain.models.scan.ScanResponse
import com.tangem.feature.onboarding.legacy.redux.products.wallet.redux.BackupStartedSource
import org.rekotlin.Action

sealed class OnboardingGlobalAction : Action {

    /**
     * Initiate an onboarding process.
     * For resuming unfinished backup of standard Wallet see
     * BackupAction.CheckForUnfinishedBackup, GlobalAction.Onboarding.StartForUnfinishedBackup
     */
    data class Start(
        val scanResponse: ScanResponse,
        val source: BackupStartedSource,
        val canSkipBackup: Boolean = true,
    ) : OnboardingGlobalAction()

    /**
     * Initiate resuming of unfinished backup for standard Wallet.
     * See more BackupAction.CheckForUnfinishedBackup
     */
    data class StartForUnfinishedBackup(val addedBackupCardsCount: Int) : OnboardingGlobalAction()

    object Stop : OnboardingGlobalAction()

    data class ShouldResetCardOnCreate(val shouldReset: Boolean) : OnboardingGlobalAction()
}
