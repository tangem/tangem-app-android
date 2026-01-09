package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository

/**
 * Use case for checking if currency amount can be subtracted.
 * Amount can be subtracted if only it is paying fee
 */
class IsAmountSubtractAvailableUseCase(
    private val currenciesRepository: CurrenciesRepository,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
        isGaslessEthTx: Boolean = false,
    ): Either<Throwable, Boolean> = Either.catch {
        if (isGaslessEthTx) return@catch true
        when (val feeCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, currency.network)) {
            is FeePaidCurrency.Coin -> currency is CryptoCurrency.Coin
            is FeePaidCurrency.SameCurrency -> true
            is FeePaidCurrency.Token -> currency.id == feeCurrency.tokenId
            is FeePaidCurrency.FeeResource -> false
        }
    }
}