package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.remove.RemoveCurrencyError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class RemoveCurrencyUseCase(
    internal val currenciesRepository: CurrenciesRepository,
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<RemoveCurrencyError, Unit> {
        return either {
            catch(
                block = { currenciesRepository.removeCurrency(userWalletId, currency) },
                catch = { raise(RemoveCurrencyError.DataError(it)) },
            )
        }
    }

    suspend fun hasLinkedTokens(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean {
        val walletCurrencies = currenciesRepository
            .getMultiCurrencyWalletCurrenciesSync(userWalletId = userWalletId, refresh = false)

        return currency is CryptoCurrency.Coin &&
            walletCurrencies.any { it != currency && it.network.id == currency.network.id }
    }
}