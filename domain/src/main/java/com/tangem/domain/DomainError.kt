package com.tangem.domain

import com.tangem.common.module.ModuleError
import com.tangem.common.module.ModuleMessage

/**
[REDACTED_AUTHOR]
 * All DomainError descendants must use their own range of codes, but no more than 999 error codes for each.
 */
sealed interface DomainModuleMessage : ModuleMessage

sealed class DomainError(
    override val code: Int,
    override val message: String,
    override val data: Any?,
) : DomainModuleMessage, ModuleError {

    companion object {
        const val CODE_ADD_CUSTOM_TOKEN = 1000
    }
}

sealed class AddCustomTokenError(
    subCode: Int = 0
) : DomainError(CODE_ADD_CUSTOM_TOKEN + subCode, this::class.java.simpleName, null) {

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