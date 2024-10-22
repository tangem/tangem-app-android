package com.tangem.domain.managetokens.model.exceptoin

sealed class CustomTokenFormValidationException {

    sealed class ContractAddress : CustomTokenFormValidationException() {

        data object Invalid : ContractAddress()
    }

    sealed class Decimals : CustomTokenFormValidationException() {

        data object Empty : Decimals()

        data object Invalid : Decimals()
    }

    data object EmptyName : CustomTokenFormValidationException()

    data object EmptySymbol : CustomTokenFormValidationException()

    data class DataError(val cause: Throwable) : CustomTokenFormValidationException()
}