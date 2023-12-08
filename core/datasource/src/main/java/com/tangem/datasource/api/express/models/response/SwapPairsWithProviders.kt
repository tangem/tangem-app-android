package com.tangem.datasource.api.express.models.response

class SwapPairsWithProviders(
    val swapPair: List<SwapPair>,
    val providers: List<ExchangeProvider>,
)