package com.tangem.tap.domain.scanCard.chains

import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.operations.backup.BackupService
import com.tangem.tap.common.ChainResult
import com.tangem.tap.common.extensions.dispatchDialogShow
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.extensions.primaryCardIsSaltPayVisa
import com.tangem.tap.common.redux.AppDialog
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.successOr
import com.tangem.tap.domain.scanCard.ScanChainError
import com.tangem.tap.features.onboarding.OnboardingSaltPayHelper
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayExceptionHandler
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayAction
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.store
import com.tangem.wallet.R

/**
 * Created by Anton Zhilenkov on 05.01.2023.
 */
abstract class BaseSaltPayChain : BaseCardScannedChain() {

    protected fun showSaltPayTapVisaLogoCardDialog() {
        store.dispatchDialogShow(
            AppDialog.SimpleOkDialogRes(
                headerId = R.string.saltpay_error_empty_backup_title,
                messageId = R.string.saltpay_error_empty_backup_message,
            ),
        )
    }
}

class SaltPayCheckUnfinishedBackupChain(
    private val backupService: BackupService,
) : BaseSaltPayChain() {

    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        val scanResponse = data.successOr { return data }

        if (!backupService.hasIncompletedBackup || !backupService.primaryCardIsSaltPayVisa()) {
            return data
        }

        val isTheSamePrimaryCard = backupService.primaryCardId
            ?.let { it == scanResponse.card.cardId }
            ?: false

        return if (scanResponse.isSaltPayWallet() || !isTheSamePrimaryCard) {
            showSaltPayTapVisaLogoCardDialog()
            ChainResult.Failure(ScanChainError.InterruptBy.SaltPayError)
        } else {
            data
        }
    }
}

class SaltPayCardScannedChain(
    private val onWalletNotCreated: suspend () -> Unit = { },
) : BaseSaltPayChain() {

    override suspend fun invoke(data: ChainResult<ScanResponse>): ChainResult<ScanResponse> {
        val scanResponse = data.successOr { return data }
        if (!scanResponse.isSaltPay()) return data

        if (scanResponse.isSaltPayVisa()) {
            val (manager, config) = OnboardingSaltPayState.initDependency(scanResponse)
            when (val result = OnboardingSaltPayHelper.isOnboardingCase(scanResponse, manager)) {
                is Result.Success -> {
                    val isOnboardingCase = result.data
                    if (isOnboardingCase) {
                        onWalletNotCreated()
                        store.dispatchOnMain(GlobalAction.Onboarding.Start(scanResponse, canSkipBackup = false))
                        store.dispatchOnMain(OnboardingSaltPayAction.SetDependencies(manager, config))
                        store.dispatchOnMain(OnboardingSaltPayAction.Update)
                        return navigateTo(AppScreen.OnboardingWallet)
                    }
                }
                is Result.Failure -> {
                    SaltPayExceptionHandler.handle(result.error)
                    return ChainResult.Failure(ScanChainError.InterruptBy.SaltPayError)
                }
            }
        } else {
            if (scanResponse.card.backupStatus?.isActive == false) {
                showSaltPayTapVisaLogoCardDialog()
                return ChainResult.Failure(ScanChainError.InterruptBy.SaltPayError)
            }
        }

        return data
    }
}
