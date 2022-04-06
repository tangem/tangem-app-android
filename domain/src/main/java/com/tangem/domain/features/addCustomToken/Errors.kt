package com.tangem.domain.features.addCustomToken

import com.tangem.domain.common.AnyError

/**
[REDACTED_AUTHOR]
 */
sealed class AddCustomTokenWarning : AnyError(0, "Add custom token - warning") {
    object PotentialScamToken : AddCustomTokenWarning()
    object TokenAlreadyAdded : AddCustomTokenWarning()
}

sealed class AddCustomTokenError : AnyError(1, "Add custom token - error") {
    object NetworkIsNotSelected : AddCustomTokenError()
    object InvalidContractAddress : AddCustomTokenError()
    object FieldIsEmpty : AddCustomTokenError()
    object FieldIsNotEmpty : AddCustomTokenError()
    object InvalidDerivationPath : AddCustomTokenError()
}