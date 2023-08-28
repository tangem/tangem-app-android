package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.models.remove.RemoveCurrencyError
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider

class RemoveCurrencyUseCase(
    internal val currenciesRepository: CurrenciesRepository,
    internal val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(userWallet: UserWallet, currency: CryptoCurrency): Either<RemoveCurrencyError, Unit> {
        return either {
            catch(
                block = { currenciesRepository.removeCurrency(userWallet, currency) },
                catch = { throwable ->
                    raise(
                        when (throwable) {
                            is RemoveCurrencyError.HasLinkedTokens -> throwable
                            else -> RemoveCurrencyError.DataError(throwable)
                        },
                    )
                },
            )
        }
    }
}
