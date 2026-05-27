package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressProviderType
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapCurrencyStatus
import com.tangem.domain.swap.models.SwapTxType

/**
 * Use case for retrieving swap pairs between a specific primary and secondary currency.
 *
 * Returns either a list of available swap pairs or a resolved swap error.
 *
 * @property swapRepositoryV2   repository providing swap pair data
 * @property swapErrorResolver  resolver that maps exceptions to domain swap errors
 */
class GetSwapPairUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapErrorResolver: SwapErrorResolver,
) {
    suspend operator fun invoke(
        primarySwapCurrencyStatus: SwapCurrencyStatus,
        secondarySwapCurrencyStatus: SwapCurrencyStatus,
        filterProviderTypes: List<ExpressProviderType>,
        swapTxType: SwapTxType,
    ) = Either.catch {
        swapRepositoryV2.getPairs(
            primarySwapCurrencyStatus = primarySwapCurrencyStatus,
            secondarySwapCurrencyStatus = secondarySwapCurrencyStatus,
            filterProviderTypes = filterProviderTypes,
            swapTxType = swapTxType,
        )
    }.mapLeft(swapErrorResolver::resolve)
}