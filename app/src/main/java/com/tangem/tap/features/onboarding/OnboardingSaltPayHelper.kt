package com.tangem.tap.features.onboarding

import com.tangem.common.services.Result
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.extensions.successOr
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.message.SaltPayActivationError
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.SaltPayActivationStep
import com.tangem.tap.features.onboarding.products.wallet.saltPay.redux.update
import com.tangem.tap.preferencesStorage

/**
 * Created by Anton Zhilenkov on 16.10.2022.
 */
object OnboardingSaltPayHelper {
    suspend fun isOnboardingCase(scanResponse: ScanResponse, manager: SaltPayActivationManager): Result<Boolean> {
        return try {
            var updatedStep = manager.update(SaltPayActivationStep.None, null).successOr { return it }

            val cardStorage = preferencesStorage.usedCardsPrefStorage
            val activationIsFinished = cardStorage.isActivationFinished(scanResponse.card.cardId)
            if (updatedStep == SaltPayActivationStep.Success && activationIsFinished) {
                updatedStep = SaltPayActivationStep.Finished
            }
            val isActivationCase = updatedStep != SaltPayActivationStep.Finished
            val isBackupCase = scanResponse.card.backupStatus?.isActive == false
            Result.Success(isActivationCase || isBackupCase)
        } catch (ex: Exception) {
            Result.Failure(ex)
        } catch (error: SaltPayActivationError) {
            Result.Failure(error)
        }
    }
}
