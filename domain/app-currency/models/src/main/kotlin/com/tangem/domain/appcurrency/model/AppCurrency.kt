package com.tangem.domain.appcurrency.model

data class AppCurrency(
    val code: String,
    val name: String,
    val symbol: String,
    val iconSmallUrl: String? = null,
    val iconMediumUrl: String? = null,
) {

    companion object {
        val Default = AppCurrency(
            code = "USD",
            name = "US Dollar",
            symbol = "$",
        )
    }
}