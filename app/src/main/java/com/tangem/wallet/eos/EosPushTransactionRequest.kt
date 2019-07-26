package com.tangem.wallet.eos

data class EosPushTransactionRequest(
        var compression: String = "none",
        var transaction: EosPackedTransaction? = null,
        var signatures: List<String>? = null
)