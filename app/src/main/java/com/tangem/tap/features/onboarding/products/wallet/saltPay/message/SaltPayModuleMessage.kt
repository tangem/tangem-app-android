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
    object Unknown : SaltPayActivationError(4)
    object Empty : SaltPayActivationError(4)

    // gnosis
    object FailedToMakeTxData : SaltPayActivationError(1)
    object FailedToSendTx : SaltPayActivationError(2)

    object NeedPin : SaltPayActivationError(3)
    object NoGas : SaltPayActivationError(5) //
    object EmptyResponse : SaltPayActivationError(6)
    object CardNotFound : SaltPayActivationError(8)
    class CardDisabled(message: String?) : SaltPayActivationError(9, message)
    class CardNotPassed(message: String?) : SaltPayActivationError(10, message)
    object EmptyDynamicAttestResponse : SaltPayActivationError(11)
    object EmptyBackupCardScanned : SaltPayActivationError(12)
    object WeakPin : SaltPayActivationError(13)

    object FailedToGetFundsToClaim : SaltPayActivationError(14)
    object NoFundsToClaim : SaltPayActivationError(15)
    object ClaimTransactionFailed : SaltPayActivationError(16)
    object NotClaimed : SaltPayActivationError(17)

    companion object {
        const val EMPTY_MESSAGE = ""
    }
}