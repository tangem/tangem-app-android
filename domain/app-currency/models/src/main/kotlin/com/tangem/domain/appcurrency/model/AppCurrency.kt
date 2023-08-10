package com.tangem.domain.appcurrency.model

data class AppCurrency(
    val code: String,
    val name: String,
    val symbol: String,
) {

    companion object {
        val Default = AppCurrency(
            code = "USD",
            name = "US Dollar",
            symbol = "$",
        )
    }
}
