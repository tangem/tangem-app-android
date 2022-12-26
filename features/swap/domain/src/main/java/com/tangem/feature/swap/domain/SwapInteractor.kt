package com.tangem.feature.swap.domain

import com.tangem.feature.swap.domain.models.data.Currency
import com.tangem.feature.swap.domain.models.data.SwapState
import java.math.BigDecimal

interface SwapInteractor {

    suspend fun getTokensToSwap(networkId: String): List<Currency>

    suspend fun getTokenBalance(tokenId: String): String

    suspend fun getFee(): BigDecimal

    suspend fun givePermissionToSwap(tokenToApprove: Currency)

    suspend fun findBestQuote(
        fromToken: Currency,
        toToken: Currency,
        amount: String,
    ): SwapState

    suspend fun onSwap(): SwapState
}
