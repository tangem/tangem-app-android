package com.tangem.domain

import com.tangem.common.module.FbConsumeException
import com.tangem.common.module.ModuleError
import com.tangem.common.module.ModuleErrorCode
import com.tangem.common.module.ModuleMessage

/**
 * Created by Anton Zhilenkov on 14/04/2022.
 * All DomainError descendants must use their own range of codes, but no more than 999 error codes for each.
 */
sealed interface DomainModuleMessage : ModuleMessage

sealed class DomainModuleError(
    subCode: Int,
    override val message: String,
    override val data: Any?,
) : DomainModuleMessage, ModuleError() {
    override val code: Int = ModuleErrorCode.DOMAIN + subCode

    companion object {
        // base code used for all errors in the module
        internal const val ERROR_CODE_ADD_CUSTOM_TOKEN = 100
//        const val CODE_ANY_OTHER = 200..299, 300..399 etc
    }
}

sealed class AddCustomTokenError(
    subCode: Int = 0,
    message: String? = null,
    data: Any? = null,
) : DomainModuleError(
    subCode = ERROR_CODE_ADD_CUSTOM_TOKEN + subCode,
    message = message ?: this::class.java.simpleName,
    data = data,
) {

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
        object UnsupportedSolanaToken : Warning()
    }

    data class SelectTokeNetworkError(val networkId: String) :
        AddCustomTokenError(
            message = "Unknown network [$networkId] should not be included in the network selection dialog.",
        ),
        FbConsumeException

    data class UnAppropriateInitialization(
        val of: String,
        val info: String? = null,
    ) : AddCustomTokenError(
        message = "The [$of], must be properly initialized. Info [$info]",
    )
}
