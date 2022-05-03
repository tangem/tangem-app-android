package com.tangem.domain

import com.tangem.common.module.ModuleError
import com.tangem.common.module.ModuleMessage

/**
[REDACTED_AUTHOR]
 * All DomainError descendants must use their own range of codes, but no more than 999 error codes for each.
 */
sealed interface DomainModuleMessage : ModuleMessage

sealed class DomainError(
    subCode: Int,
    override val message: String,
    override val data: Any?,
) : DomainModuleMessage, ModuleError {
    override val code: Int = ERROR_CODE_DOMAIN + subCode

    companion object {
        // base code used for all error in the module
        const val ERROR_CODE_DOMAIN = 10000
        const val ERROR_CODE_ADD_CUSTOM_TOKEN = 100
//        const val CODE_ANY_OTHER = 200..299
    }
}

sealed class AddCustomTokenError(
    subCode: Int = 0
) : DomainError(ERROR_CODE_ADD_CUSTOM_TOKEN + subCode, this::class.java.simpleName, null) {

    object FieldIsEmpty : AddCustomTokenError()
    object FieldIsNotEmpty : AddCustomTokenError()
    object InvalidContractAddress : AddCustomTokenError()
    object NetworkIsNotSelected : AddCustomTokenError()
    object InvalidDecimalsCount : AddCustomTokenError()
    object InvalidDerivationPath : AddCustomTokenError()

    sealed class Network : AddCustomTokenError() {
        object CheckAddressRequestError : Network()
    }

    sealed class Warning : AddCustomTokenError() {
        object PotentialScamToken : Warning()
        object TokenAlreadyAdded : Warning()
    }
}