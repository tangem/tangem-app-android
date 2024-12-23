package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetCryptoCurrencyUseCase(
    private val currenciesRepository: CurrenciesRepository,
) {

    /**
     * Returns specific cryptocurrency for a given user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @param id The ID of the cryptocurrency.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): Either<CurrencyStatusError, CryptoCurrency> {
        return either { getCurrency(userWalletId, id) }
    }

    /**
     * Returns specific cryptocurrency for a given user wallet.
     *
     * !!! Important Use only [CryptoCurrency.ID.value] as cryptoCurrencyId
     *
     * @param userWalletId The ID of the user's wallet.
     * @param cryptoCurrencyId String representation of the [CryptoCurrency.ID.value] of the cryptocurrency.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyId: String,
    ): Either<CurrencyStatusError, CryptoCurrency> {
        return either { getCurrency(userWalletId, cryptoCurrencyId) }
    }

    /**
     * Returns the primary cryptocurrency for a given user wallet.
     *
     * @param userWalletId The ID of the user's wallet.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(userWalletId: UserWalletId): Either<CurrencyStatusError, CryptoCurrency> {
        return either { getPrimaryCurrency(userWalletId) }
    }

    private suspend fun Raise<CurrencyStatusError>.getCurrency(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
    ): CryptoCurrency {
        return catch(
            block = {
                currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, id)
            },
            catch = { raise(CurrencyStatusError.DataError(it)) },
        )
    }

    private suspend fun Raise<CurrencyStatusError>.getCurrency(
        userWalletId: UserWalletId,
        id: String,
    ): CryptoCurrency {
        return catch(
            block = {
                currenciesRepository.getMultiCurrencyWalletCurrency(userWalletId, id)
            },
            catch = { raise(CurrencyStatusError.DataError(it)) },
        )
    }

    private suspend fun Raise<CurrencyStatusError>.getPrimaryCurrency(userWalletId: UserWalletId): CryptoCurrency {
        return catch(
            block = { currenciesRepository.getSingleCurrencyWalletPrimaryCurrency(userWalletId) },
            catch = { raise(CurrencyStatusError.DataError(it)) },
        )
    }
}
