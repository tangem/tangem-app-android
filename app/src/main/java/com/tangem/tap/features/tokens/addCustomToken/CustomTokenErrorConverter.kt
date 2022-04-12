package com.tangem.tap.features.tokens.addCustomToken

import android.content.Context
import com.tangem.domain.DomainError
import com.tangem.domain.ErrorConverter
import com.tangem.domain.features.addCustomToken.AddCustomTokenError
import com.tangem.domain.features.addCustomToken.AddCustomTokenWarning
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class CustomTokenErrorConverter(
    private val context: Context
) : ErrorConverter<String> {

    override fun convertError(error: DomainError): String {
        val customTokenError = (error as? AddCustomTokenError) ?: throw UnsupportedOperationException()

        val resId = when (customTokenError) {
            AddCustomTokenError.InvalidContractAddress -> R.string.custom_token_creation_error_invalid_contract_address
            AddCustomTokenError.NetworkIsNotSelected -> R.string.custom_token_creation_error_network_not_selected
            AddCustomTokenError.InvalidDerivationPath -> R.string.custom_token_creation_error_invalid_derivation_path
            AddCustomTokenError.FieldIsEmpty -> R.string.custom_token_creation_error_empty_fields
            else -> null
        }
        return resId?.let { context.getString(it) } ?: "Unknown error: ${customTokenError::class.java.simpleName}"
    }
}

class CustomTokenWarningConverter(
    private val context: Context
) : ErrorConverter<String> {

    override fun convertError(error: DomainError): String {
        val customTokenWarning = (error as? AddCustomTokenWarning) ?: throw UnsupportedOperationException()

        val resId = when (customTokenWarning) {
            AddCustomTokenWarning.PotentialScamToken -> R.string.custom_token_validation_error_not_found
            AddCustomTokenWarning.TokenAlreadyAdded -> R.string.custom_token_validation_error_already_added
        }
        return context.getString(resId)
    }
}