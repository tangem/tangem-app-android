package com.tangem.tap.common.entities

data class FiatCurrency(
    val code: String,
    val name: String,
    val symbol: String,
) {
    val displayName: String
        get() = "${this.name} (${this.code}) - ${this.symbol}"

    companion object {
        val Default = FiatCurrency(
            symbol = "$",
            code = "USD",
            name = "US Dollar",
        )
    }
}