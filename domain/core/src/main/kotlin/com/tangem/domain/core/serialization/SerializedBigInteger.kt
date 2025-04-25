package com.tangem.domain.core.serialization

import kotlinx.serialization.Serializable
import java.math.BigInteger

typealias SerializedBigInteger = @Serializable(with = BigIntegerSerializer::class) BigInteger