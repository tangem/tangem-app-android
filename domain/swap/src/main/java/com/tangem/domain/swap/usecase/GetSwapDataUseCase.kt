package com.tangem.domain.swap.usecase

import arrow.core.Either
import com.tangem.domain.express.models.ExpressError
import com.tangem.domain.express.models.ExpressProvider
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.swap.SwapErrorResolver
import com.tangem.domain.swap.SwapRepositoryV2
import com.tangem.domain.swap.models.SwapDataModel

@Suppress("LongParameterList")
class GetSwapDataUseCase(
    private val swapRepositoryV2: SwapRepositoryV2,
    private val swapErrorResolver: SwapErrorResolver,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        fromCryptoCurrencyStatus: CryptoCurrencyStatus,
        fromAmount: String,
        toCryptoCurrency: CryptoCurrency,
        toAddress: String,
        expressProvider: ExpressProvider,
        rateType: ExpressRateType,
    ): Either<ExpressError, SwapDataModel> = Either.catch {
        swapRepositoryV2.getSwapData(
            userWallet = userWallet,
            fromCryptoCurrencyStatus = fromCryptoCurrencyStatus,
            fromAmount = fromAmount,
            toCryptoCurrency = toCryptoCurrency,
            toAddress = toAddress,
            expressProvider = expressProvider,
            rateType = rateType,
        )
    }.mapLeft { throwable ->
        swapErrorResolver.resolve(throwable)
    }
}