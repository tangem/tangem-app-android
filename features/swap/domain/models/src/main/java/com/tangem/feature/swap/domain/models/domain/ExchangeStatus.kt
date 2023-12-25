package com.tangem.feature.swap.domain.models.domain

data class ExchangeStatusModel(
    val providerId: String,
    val status: ExchangeStatus? = null,
    val txId: String? = null,
    val txExternalUrl: String? = null,
)

enum class ExchangeStatus {
    New,
    Waiting,
    Confirming,
    Verifying,
    Exchanging,
    Failed,
    Sending,
    Finished,
    Refunded,
}