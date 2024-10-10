package com.tangem.domain.redux

import com.tangem.domain.models.scan.ScanResponse
import org.rekotlin.Action

sealed interface LegacyAction : Action {

    /**
     * Initiate an onboarding process.
     * For resuming unfinished backup of standard Wallet see
     * BackupAction.CheckForUnfinishedBackup, GlobalAction.Onboarding.StartForUnfinishedBackup
     */
    data class StartOnboardingProcess(val scanResponse: ScanResponse, val canSkipBackup: Boolean = true) : LegacyAction

    data object PrepareDetailsScreen : LegacyAction
}