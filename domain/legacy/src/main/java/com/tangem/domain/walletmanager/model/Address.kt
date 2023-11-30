package com.tangem.domain.walletmanager.model

data class Address(
    val value: String,
    val type: Type,
) {

    enum class Type {
        Primary, Secondary,
    }
}