package com.domain.blockaid.models.transaction

import java.math.BigInteger

data class GasEstimationResult(
    val estimatedGasList: List<BigInteger>,
)