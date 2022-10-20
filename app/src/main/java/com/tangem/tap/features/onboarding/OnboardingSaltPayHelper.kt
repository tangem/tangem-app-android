package com.tangem.tap.features.onboarding

import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.successOr
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayRegistrationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.OnboardingSaltPayState
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayRegistrationStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.toSaltPayStep

/**
[REDACTED_AUTHOR]
 */
class OnboardingSaltPayHelper {
    companion object {

        suspend fun isOnboardingCase(scanResponse: ScanResponse): Result<Boolean> {
            return try {
                val (manager, config) = OnboardingSaltPayState.initDependency(scanResponse)
                val registrationResponseItem = manager.checkRegistration().successOr { return it }
                val registrationStep = registrationResponseItem.toSaltPayStep()
                manager.checkHasGas().successOr { return it }

                val isRegistrationOnboardingCase = registrationStep != SaltPayRegistrationStep.Finished
                val isBackupOnboardingCase = scanResponse.card.backupStatus?.isActive == false
                Result.Success(isRegistrationOnboardingCase || isBackupOnboardingCase)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayRegistrationError){
                Result.Failure(error)
            }
        }

        suspend fun testProceedToOnboarding(scanResponse: ScanResponse): Result<Boolean> {
            return try {
                val isBackupOnboardingCase = scanResponse.card.backupStatus?.isActive == false
                Result.Success(isBackupOnboardingCase)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayRegistrationError){
                Result.Failure(error)
            }
        }

        suspend fun testProceedToMainScreen(scanResponse: ScanResponse): Result<Boolean> {
            return try {
                Result.Success(false)
            } catch (ex: Exception) {
                Result.Failure(ex)
            } catch (error: SaltPayRegistrationError){
                Result.Failure(error)
            }
        }
    }
}