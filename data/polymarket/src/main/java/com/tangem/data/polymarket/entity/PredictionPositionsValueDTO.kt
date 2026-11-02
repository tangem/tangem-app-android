package com.tangem.data.polymarket.entity

import com.tangem.domain.models.serialization.SerializedBigDecimal
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PredictionPositionsValueDTO(@SerialName("amount") val amount: SerializedBigDecimal)