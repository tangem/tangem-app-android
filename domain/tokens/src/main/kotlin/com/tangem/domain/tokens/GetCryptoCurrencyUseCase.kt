package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
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
     * @param contractAddress The contract address of the crypto currency
     * @param derivationPath currency derivation path.
     * @return An [Either] representing success (Right) or an error (Left) in fetching the status.
     */
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        id: CryptoCurrency.ID,
        contractAddress: String?,
        derivationPath: Network.DerivationPath,
    ): Either<CurrencyStatusError, CryptoCurrency> {
        return either { getCurrency(userWalletId, id, contractAddress, derivationPath) }
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
        contractAddress: String?,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency {
        return catch(
            block = {
                currenciesRepository.getMultiCurrencyWalletCurrency(
                    userWalletId,
                    id,
                    contractAddress,
                    derivationPath,
                )
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
