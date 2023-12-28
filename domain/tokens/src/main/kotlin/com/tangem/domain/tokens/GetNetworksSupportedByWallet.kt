package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.tokens.repository.NetworksCompatibilityRepository
import com.tangem.domain.wallets.models.UserWalletId

class GetNetworksSupportedByWallet(
    private val repository: NetworksCompatibilityRepository,
) {
    suspend operator fun invoke(userWalletId: UserWalletId): Either<Throwable, List<Network>> {
        return either {
            catch(
                block = {
                    repository.getSupportedNetworks(userWalletId)
                },
                catch = { throwable -> raise(throwable) },
            )
        }
    }
}