package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.managetokens.model.exceptoin.FindTokenException
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

class FindTokenUseCase(
    private val repository: CustomTokensRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Either<FindTokenException, CryptoCurrency.Token> = either {
        val currency = findCurrency(
            userWalletId = userWalletId,
            contractAddress = contractAddress,
            networkId = networkId,
            derivationPath = derivationPath,
        )

        ensureNotNull(currency) {
            FindTokenException.NotFound
        }
    }

    private suspend fun Raise<FindTokenException>.findCurrency(
        userWalletId: UserWalletId,
        contractAddress: String,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): CryptoCurrency.Token? = catch(
        block = {
            repository.findToken(
                userWalletId = userWalletId,
                contractAddress = contractAddress,
                networkId = networkId,
                derivationPath = derivationPath,
            )
        },
        catch = {
            raise(FindTokenException.DataError(it))
        },
    )
}