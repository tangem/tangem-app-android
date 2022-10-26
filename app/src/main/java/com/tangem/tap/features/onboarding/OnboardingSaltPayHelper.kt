package com.tangem.tap.features.onboarding

import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.successOr
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayActivationStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.update

/**
* [REDACTED_AUTHOR]
 */
class OnboardingSaltPayHelper {
    companion object {

        suspend fun isOnboardingCase(
            scanResponse: ScanResponse,
            manager: SaltPayActivationManager,
        ): Result<Boolean> {
            return try {
                val updatedStep = manager.update(SaltPayActivationStep.None, null).successOr { return it }
                val isRegistrationCase = updatedStep != SaltPayActivationStep.Finished
                val isBackupCase = scanResponse.card.backupStatus?.isActive == false
                Result.Success(isRegistrationCase || isBackupCase)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayActivationError) {
                Result.Failure(error)
            }
        }

        fun testProceedToOnboarding(
            scanResponse: ScanResponse,
            manager: SaltPayActivationManager,
        ): Result<Boolean> = Result.Success(true)

        fun testProceedToMainScreen(
            scanResponse: ScanResponse,
            manager: SaltPayActivationManager,
        ): Result<Boolean> = Result.Success(false)
    }
}
