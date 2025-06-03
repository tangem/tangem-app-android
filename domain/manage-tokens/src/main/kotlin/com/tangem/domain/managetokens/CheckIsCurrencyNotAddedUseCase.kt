package com.tangem.domain.managetokens

import arrow.core.Either
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

class CheckIsCurrencyNotAddedUseCase(
    private val repository: CustomTokensRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
        contractAddress: String?,
    ): Either<Throwable, Boolean> = Either.catch {
        repository.isCurrencyNotAdded(userWalletId, networkId, derivationPath, contractAddress)
    }
}