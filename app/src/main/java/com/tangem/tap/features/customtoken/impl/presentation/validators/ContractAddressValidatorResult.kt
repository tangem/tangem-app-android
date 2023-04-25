package com.tangem.tap.features.customtoken.impl.presentation.validators

import com.tangem.domain.AddCustomTokenError

/**
 * Result of validation contract address
 *
 * @author Andrew Khokhlov on 22/04/2023
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
