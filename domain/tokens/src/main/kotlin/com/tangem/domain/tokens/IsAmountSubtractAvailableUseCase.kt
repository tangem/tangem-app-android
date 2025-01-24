package com.tangem.domain.tokens

import arrow.core.Either
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.FeePaidCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Use case for checking if currency amount can be subtracted.
 * Amount can be subtracted if only it is paying fee
 */
class IsAmountSubtractAvailableUseCase(
    private val currenciesRepository: CurrenciesRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {
    suspend operator fun invoke(userWalletId: UserWalletId, currency: CryptoCurrency): Either<Throwable, Boolean> =
        Either.catch {
            withContext(dispatchers.io) {
                when (val feeCurrency = currenciesRepository.getFeePaidCurrency(userWalletId, currency.network)) {
                    is FeePaidCurrency.Coin -> currency is CryptoCurrency.Coin
                    is FeePaidCurrency.SameCurrency -> true
                    is FeePaidCurrency.Token -> currency.id == feeCurrency.tokenId
                    is FeePaidCurrency.FeeResource -> false
                }
            }
        }
}