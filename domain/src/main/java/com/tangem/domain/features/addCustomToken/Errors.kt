package com.tangem.domain.features.addCustomToken

import com.tangem.domain.AnError
import com.tangem.domain.ERROR_CODE_ADD_CUSTOM_TOKEN

/**
[REDACTED_AUTHOR]
 */
sealed class AddCustomTokenError : AnError(ERROR_CODE_ADD_CUSTOM_TOKEN, "Add custom token - error") {
    object FieldIsEmpty : AddCustomTokenError()
    object FieldIsNotEmpty : AddCustomTokenError()
    object InvalidContractAddress : AddCustomTokenError()
    object NetworkIsNotSelected : AddCustomTokenError()
    object InvalidDecimalsCount : AddCustomTokenError()
    object InvalidDerivationPath : AddCustomTokenError()
}

sealed class AddCustomTokenWarning : AnError(ERROR_CODE_ADD_CUSTOM_TOKEN, "Add custom token - warning") {
    object PotentialScamToken : AddCustomTokenWarning()
    object TokenAlreadyAdded : AddCustomTokenWarning()
}