package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.visa.error.VisaApiError

/**
 * Maps a failed bank off-ramp call onto the wizard's error model. The backend names exactly one bank off-ramp
 * failure, so everything else stays unknown rather than being guessed into a screen of its own; [operation] keeps
 * the failing call in the cause, because by the time the failure is logged the call it came from is gone.
 */
internal object BankOfframpErrorConverter {

    fun convert(error: VisaApiError, operation: String): BankOfframpError = when (error) {
        VisaApiError.BankOfframpUnsupportedConfiguration -> BankOfframpError.UnsupportedConfiguration
        else -> BankOfframpError.Unknown(cause = IllegalStateException("Bank off-ramp $operation failed: $error"))
    }
}