package com.tangem.store.datasource.blockaid.models.request

enum class BlockAidScanOptions(val value: String) {
    Simulation("simulation"),
    Validation("validation"),
    GasEstimation("gas_estimation"),
}