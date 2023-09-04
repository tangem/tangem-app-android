package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.error.GetCurrenciesError
import com.tangem.domain.tokens.models.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetCryptoCurrenciesUseCase(private val currenciesRepository: CurrenciesRepository) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        refresh: Boolean = false,
    ): Either<GetCurrenciesError, List<CryptoCurrency>> {
        return either {
            catch(
                block = { currenciesRepository.getMultiCurrencyWalletCurrenciesSync(userWalletId, refresh) },
                catch = { raise(GetCurrenciesError.DataError(it)) },
            )
        }
    }
}