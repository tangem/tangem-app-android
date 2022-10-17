package com.tangem.tap.features.onboarding.products.wallet.saltPay.message

import com.tangem.common.module.ModuleError
import com.tangem.common.module.ModuleErrorCode
import com.tangem.common.module.ModuleMessage

sealed interface SaltPayModuleMessage : ModuleMessage

sealed class SaltPayModuleError(
    subCode: Int,
    override val message: String,
    override val data: Any? = null,
) : SaltPayModuleMessage, ModuleError() {
    override val code: Int = ModuleErrorCode.SALT_PAY + subCode

    companion object {
        // base code used for all errors in the module
        internal const val ERROR_CODE_REGISTRATION = 100
//        const val CODE_ANY_OTHER = 200..299, 300..399 etc
    }
}

sealed class SaltPayRegistrationError(
    subCode: Int,
    message: String? = null,
    override val data: Any? = null,
) : SaltPayModuleError(
    subCode = ERROR_CODE_REGISTRATION + subCode,
    message = message ?: this::class.java.simpleName,
) {
    object Unknown : SaltPayRegistrationError(4)
    object Empty : SaltPayRegistrationError(4)
    object FailedToMakeTxData : SaltPayRegistrationError(1)
    object FailedToSendTx : SaltPayRegistrationError(2)
    object NeedPin : SaltPayRegistrationError(3)
    object NoGas : SaltPayRegistrationError(5)
    object EmptyResponse : SaltPayRegistrationError(6)
    object Card : SaltPayRegistrationError(7)
    object CardNotFound : SaltPayRegistrationError(8)
    object CardDisabled : SaltPayRegistrationError(9)
    object CardNotPassed : SaltPayRegistrationError(10)
    object EmptyDynamicAttestResponse : SaltPayRegistrationError(11)
    object EmptyBackupCardScanned : SaltPayRegistrationError(12)
    object WeakPin : SaltPayRegistrationError(13)
}
