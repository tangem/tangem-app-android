package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapAmountType
import com.tangem.domain.swap.models.SwapQuoteModel
import com.tangem.domain.models.wallet.UserWallet
import java.math.BigDecimal

/**
 * Get swap quote for selected pair
 */
@Suppress("LongParameterList")
class GetSwapQuoteUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapErrorResolver: SwapErrorResolver,
) {

    /**
     * @param userWallet selected user wallet
     * @param fromCryptoCurrency currency swap from
     * @param toCryptoCurrency currency swap to
     * @param amount swap amount

     * @param rateType rate type for the quote. Float API does not support [SwapAmountType.To].
     * @param provider swap provider
     */
    suspend operator fun invoke(
        userWallet: UserWallet,
        fromCryptoCurrency: CryptoCurrency,
        toCryptoCurrency: CryptoCurrency,
        amount: BigDecimal,
        amountType: SwapAmountType,
        rateType: ExpressRateType,
        provider: ExpressProvider,
    ): Either<ExpressError, SwapQuoteModel> = Either.catch {
        swapRepositoryV2.getSwapQuote(
            userWallet = userWallet,
            fromCryptoCurrency = fromCryptoCurrency,
            toCryptoCurrency = toCryptoCurrency,
            amount = amount,
            amountType = amountType,
            provider = provider,
            rateType = rateType,
        )
    }.mapLeft(swapErrorResolver::resolve)
}