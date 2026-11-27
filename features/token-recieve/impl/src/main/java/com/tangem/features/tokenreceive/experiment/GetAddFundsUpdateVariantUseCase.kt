package com.tangem.features.tokenreceive.experiment

import com.tangem.core.abtests.manager.ABTestsManager
import javax.inject.Inject

internal class GetAddFundsUpdateVariantUseCase @Inject constructor(
    private val abTestsManager: ABTestsManager,
) {

    operator fun invoke(): AddFundsUpdateVariant {
        return if (abTestsManager.getCachedValue(key = AMPLITUDE_ID) == TREATMENT_VALUE) {
            AddFundsUpdateVariant.TREATMENT
        } else {
            AddFundsUpdateVariant.CONTROL
        }
    }

    private companion object {
        const val AMPLITUDE_ID = "twi_1958_add_funds_update"
        const val TREATMENT_VALUE = "treatment"
    }
}

internal enum class AddFundsUpdateVariant { CONTROL, TREATMENT }