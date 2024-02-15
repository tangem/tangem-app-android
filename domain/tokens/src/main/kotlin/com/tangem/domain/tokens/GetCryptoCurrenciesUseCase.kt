package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.tokens.error.GetCurrenciesError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.operations.CurrenciesStatusesOperations
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty

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

    fun getAsync(userWalletId: UserWalletId): Flow<Either<GetCurrenciesError, List<CryptoCurrency>>> {
        return currenciesRepository.getMultiCurrencyWalletCurrenciesUpdates(userWalletId)
            .map<List<CryptoCurrency>, Either<GetCurrenciesError, List<CryptoCurrency>>> { it.right() }
            .catch { emit(GetCurrenciesError.DataError(it).left()) }
    }
}
