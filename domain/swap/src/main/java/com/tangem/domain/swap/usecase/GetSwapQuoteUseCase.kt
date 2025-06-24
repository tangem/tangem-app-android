package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.domain.wallets.models.UserWallet
import java.math.BigDecimal

/**
 * Get swap quote for selected pair
 */
class GetSwapQuoteUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapErrorResolver: SwapErrorResolver,
) {

    /**
     * @param userWallet selected user wallet
     * @param fromCryptoCurrency currency swap from
     * @param toCryptoCurrency currency swap to
     * @param fromAmount swap amount
     * @param provider swap provider
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        fromAmount: BigDecimal,
        provider: ExpressProvider,
    ): Either<ExpressError, SwapQuoteModel> = Either.catch {
        swapRepositoryV2.getSwapQuote(
            userWallet = userWallet,
            fromCryptoCurrency = fromCryptoCurrency,
            toCryptoCurrency = toCryptoCurrency,
            fromAmount = fromAmount,
            provider = provider,
            rateType = ExpressRateType.Float, // todo rate type
        )
    }.mapLeft(swapErrorResolver::resolve)
}