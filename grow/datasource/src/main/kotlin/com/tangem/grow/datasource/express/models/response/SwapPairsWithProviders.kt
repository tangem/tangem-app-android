package com.tangem.grow.datasource.express.models.response

class SwapPairsWithProviders(
    val swapPair: List<SwapPair>,
    val providers: List<ExchangeProvider>,
)