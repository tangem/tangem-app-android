package com.tangem.feature.swap.domain.models.domain

data class ExchangeStatusModel(
    val providerId: Int,
    val status: ExchangeStatus? = null,
    val txId: String? = null,
    val txUrl: String? = null,
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
