package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.tokens.error.GetCurrenciesError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class GetMissedAddressesCryptoCurrenciesUseCase(private val currenciesRepository: CurrenciesRepository) {

    operator fun invoke(userWalletId: UserWalletId): Flow<Either<GetCurrenciesError, List<CryptoCurrency>>> {
        return currenciesRepository.getMissedAddressesCryptoCurrencies(userWalletId)
            .map<List<CryptoCurrency>, Either<GetCurrenciesError, List<CryptoCurrency>>> { it.right() }
            .catch { emit(GetCurrenciesError.DataError(it).left()) }
    }
}