package com.tangem.domain.models.serialization

import kotlinx.serialization.Serializable
import java.math.BigInteger

typealias SerializedBigInteger = @Serializable(with = BigIntegerSerializer::class) BigInteger