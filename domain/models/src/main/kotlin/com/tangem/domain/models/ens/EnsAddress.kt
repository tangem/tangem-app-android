package com.tangem.domain.models.ens

sealed interface EnsAddress {
    data class Address(val name: String) : EnsAddress
    data class Error(val error: Exception) : EnsAddress
    data object NotSupported : EnsAddress
}