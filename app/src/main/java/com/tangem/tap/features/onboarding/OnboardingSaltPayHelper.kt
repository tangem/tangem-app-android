package com.tangem.tap.features.onboarding

import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.successOr
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayRegistrationStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.updateActivationStatus

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
                val status = manager.updateActivationStatus(
                    amountToClaim = null,
                    step = SaltPayRegistrationStep.None,
                ).successOr { return it }

                val isRegistrationCase = status.step != SaltPayRegistrationStep.Finished
                val isBackupCase = scanResponse.card.backupStatus?.isActive == false
                Result.Success(isRegistrationCase || isBackupCase)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayRegistrationError) {
                Result.Failure(error)
            }
        }

        suspend fun testProceedToOnboarding(
            scanResponse: ScanResponse,
            manager: SaltPayActivationManager,
        ): Result<Boolean> = Result.Success(true)

        suspend fun testProceedToMainScreen(
            scanResponse: ScanResponse,
            manager: SaltPayActivationManager,
        ): Result<Boolean> = Result.Success(false)
    }
}
