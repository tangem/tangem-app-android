package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import com.tangem.domain.tokens.error.GetCurrenciesError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Use case for retrieving a list of cryptocurrencies ([CryptoCurrency]) added to a specific wallet and requiring derivation
 * (for which address is not available until the public key is derived on a card).
 */
class GetMissedAddressesCryptoCurrenciesUseCase(private val currenciesRepository: CurrenciesRepository) {

    /**
     * Retrieves a list of cryptocurrencies without an address (requiring derivation) for a given user wallet.
     * @param userWalletId: Specifies the wallet by its ID
     */
    operator fun invoke(userWalletId: UserWalletId): Flow<Either<GetCurrenciesError, List<CryptoCurrency>>> {
        return currenciesRepository.getMissedAddressesCryptoCurrencies(userWalletId)
            .map<List<CryptoCurrency>, Either<GetCurrenciesError, List<CryptoCurrency>>>(List<CryptoCurrency>::right)
            .catch { emit(GetCurrenciesError.DataError(it).left()) }
    }

    /**
     * Retrieves a map containing lists of cryptocurrencies without an address (requiring derivation) grouped by
     * user wallet IDs.
     * @param userWalletIds: List of IDs specifying the user wallets
     */
    operator fun invoke(
        userWalletIds: List<UserWalletId>,
    ): Flow<Either<GetCurrenciesError, Map<UserWalletId, List<CryptoCurrency>>>> {
        return combine(
            userWalletIds.map { userWalletId -> invoke(userWalletId) },
        ) { missedAddressesCryptoCurrenciesResult ->
            userWalletIds.zip(missedAddressesCryptoCurrenciesResult).toMap().let { map ->
                either { map.bindAll() } // taking out Left if encountered in inner responses
            }
        }.catch { cause -> emit(GetCurrenciesError.DataError(cause).left()) }
    }
}