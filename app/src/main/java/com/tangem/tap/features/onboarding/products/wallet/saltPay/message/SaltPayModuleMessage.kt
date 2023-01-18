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

sealed class SaltPayActivationError(
    subCode: Int,
    message: String? = null,
    override val data: Any? = null,
) : SaltPayModuleError(
    subCode = ERROR_CODE_REGISTRATION + subCode,
    message = message ?: EMPTY_MESSAGE,
) {
    object Unknown : SaltPayActivationError(subCode = 4)
    object Empty : SaltPayActivationError(subCode = 4)

    // gnosis
    object FailedToMakeTxData : SaltPayActivationError(subCode = 1)
    object FailedToSendTx : SaltPayActivationError(subCode = 2)

    object NeedPin : SaltPayActivationError(subCode = 3)
    object NoGas : SaltPayActivationError(subCode = 5)
    object EmptyResponse : SaltPayActivationError(subCode = 6)
    object CardNotFound : SaltPayActivationError(subCode = 8)
    class CardDisabled(message: String?) : SaltPayActivationError(subCode = 9, message = message)
    class CardNotPassed(message: String?) : SaltPayActivationError(subCode = 10, message = message)
    object EmptyDynamicAttestResponse : SaltPayActivationError(subCode = 11)
    object EmptyBackupCardScanned : SaltPayActivationError(subCode = 12)
    object WeakPin : SaltPayActivationError(subCode = 13)

    object FailedToGetFundsToClaim : SaltPayActivationError(subCode = 14)
    object NoFundsToClaim : SaltPayActivationError(subCode = 15)
    object ClaimTransactionFailed : SaltPayActivationError(subCode = 16)

    companion object {
        const val EMPTY_MESSAGE = ""
    }
}
