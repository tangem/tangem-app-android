package com.tangem.tap.features.onboarding

import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.successOr
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayRegistrationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayRegistrationStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.updateSaltPayStatus

/**
[REDACTED_AUTHOR]
 */
class OnboardingSaltPayHelper {
    companion object {

        suspend fun isOnboardingCase(
            scanResponse: ScanResponse,
            manager: SaltPayRegistrationManager,
        ): Result<Boolean> {
            return try {
                val status = manager.updateSaltPayStatus(
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

        suspend fun testProceedToOnboarding(scanResponse: ScanResponse): Result<Boolean> {
            return try {
                val isBackupOnboardingCase = scanResponse.card.backupStatus?.isActive == false
                Result.Success(isBackupOnboardingCase)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayRegistrationError) {
                Result.Failure(error)
            }
        }

        suspend fun testProceedToMainScreen(scanResponse: ScanResponse): Result<Boolean> {
            return try {
                Result.Success(false)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayRegistrationError) {
                Result.Failure(error)
            }
        }
    }
}