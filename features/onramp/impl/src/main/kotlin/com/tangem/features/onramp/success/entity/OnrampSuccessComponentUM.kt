package com.tangem.features.onramp.success.entity

sealed class OnrampSuccessComponentUM {

    data object Loading : OnrampSuccessComponentUM()

    data class Content(
        val txId: String,
        val createdAt: Long,
        val providerId: String,
        val currencyImageUrl: String,
        val fromAmount: String,
        val toAmount: String,
        val onBackClick: () -> Unit,
    ) : OnrampSuccessComponentUM()
}
