package com.tangem.domain.core.serialization

import kotlinx.serialization.Serializable
import java.math.BigDecimal

typealias SerializedBigDecimal = @Serializable(with = BigDecimalSerializer::class) BigDecimal
