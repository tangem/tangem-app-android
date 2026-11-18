package com.tangem.grow.datasource.earnvault.models

import com.tangem.domain.models.serialization.BigDecimalSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/** One point per day, oldest first; [isStale] when the history is older than 48 hours */
@Serializable
data class EarnVaultApyHistoryResponse(
    @SerialName("points")
    val points: List<EarnVaultApyPointDto>,
    @SerialName("averageApy")
    @Serializable(with = BigDecimalSerializer::class)
    val averageApy: BigDecimal,
    @SerialName("asOf")
    val asOf: String,
    @SerialName("stale")
    val isStale: Boolean,
)

@Serializable
data class EarnVaultApyPointDto(
    @SerialName("date")
    val date: String,
    @SerialName("apy")
    @Serializable(with = BigDecimalSerializer::class)
    val apy: BigDecimal,
)

enum class EarnVaultApyPeriodDto(private val value: String) {
    ONE_MONTH("1M"),
    THREE_MONTHS("3M"),
    SIX_MONTHS("6M"),
    ;

    override fun toString(): String = value
}