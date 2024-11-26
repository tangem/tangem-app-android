package com.tangem.domain.managetokens.model

sealed class AddCustomTokenForm {

    data class Raw(
        val contractAddress: String = "",
        val name: String = "",
        val symbol: String = "",
        val decimals: String = "",
    ) : AddCustomTokenForm()

    sealed class Validated : AddCustomTokenForm() {

        data object Empty : Validated()

        data class ContractAddressOnly(
            val contractAddress: String,
        ) : Validated()

        data class All(
            val contractAddress: String,
            val name: String,
            val symbol: String,
            val decimals: Int,
        ) : Validated()
    }
}