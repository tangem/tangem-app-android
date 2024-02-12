package com.tangem.tap.features.customtoken.impl.presentation.validators

import com.tangem.domain.tokens.error.AddCustomTokenError

/**
 * Result of validation contract address
 *
[REDACTED_AUTHOR]
 */
sealed interface ContractAddressValidatorResult {

    /** Success */
    object Success : ContractAddressValidatorResult

    /**
     * Error
     *
     * @property type type of error
     */
    data class Error(val type: AddCustomTokenError) : ContractAddressValidatorResult
}