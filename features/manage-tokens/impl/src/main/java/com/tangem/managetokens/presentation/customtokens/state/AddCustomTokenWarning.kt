package com.tangem.managetokens.presentation.customtokens.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.managetokens.impl.R

/**
 * Warning model of add custom token screen
 *
 * @property title warning description
 */
internal sealed class AddCustomTokenWarning(val title: TextReference, val subtitle: TextReference? = null) {

    object PotentialScamToken : AddCustomTokenWarning(
        title = resourceReference(R.string.custom_token_validation_error_not_found_title),
        subtitle = resourceReference(R.string.custom_token_validation_error_not_found_description),
    )

    object InvalidContractAddress : AddCustomTokenWarning(
        title = resourceReference(R.string.custom_token_creation_error_invalid_contract_address),
    )

    object WrongDecimals : AddCustomTokenWarning(
        title =
        resourceReference(R.string.custom_token_creation_error_wrong_decimals, wrappedList(MAXIMUM_DECIMAL_NUMBER)),
    )

    private companion object {
        const val MAXIMUM_DECIMAL_NUMBER = 30
    }
}