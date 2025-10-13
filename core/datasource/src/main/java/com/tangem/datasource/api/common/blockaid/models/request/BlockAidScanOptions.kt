package com.tangem.datasource.api.common.blockaid.models.request

enum class BlockAidScanOptions(val value: String) {
    Simulation("simulation"),
    Validation("validation"),
    GasEstimation("gas_estimation"),
}