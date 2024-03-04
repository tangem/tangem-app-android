package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.repository.NetworksCompatibilityRepository
import com.tangem.domain.wallets.models.UserWalletId

class RequiresHardenedDerivationOnlyUseCase(
    private val repository: NetworksCompatibilityRepository
) {

    suspend operator fun invoke(
        networkId: String,
        userWalletId: UserWalletId,
    ): Either<Throwable, Boolean> {
        return either {
            catch(
                block = {
                    repository.requiresHardenedDerivationOnly(networkId, userWalletId)
                },
                catch = { throwable -> raise(throwable) },
            )
        }
    }

}
