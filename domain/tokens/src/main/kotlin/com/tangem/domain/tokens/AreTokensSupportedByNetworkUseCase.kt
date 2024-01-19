package com.tangem.domain.tokens

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.domain.tokens.repository.NetworksCompatibilityRepository
import com.tangem.domain.wallets.models.UserWalletId

class AreTokensSupportedByNetworkUseCase(private val repository: NetworksCompatibilityRepository) {

    suspend operator fun invoke(networkId: String, userWalletId: UserWalletId?): Either<Throwable, Boolean> {
        return either {
            catch(
                block = {
                    if (userWalletId == null) {
                        repository.areTokensSupportedByNetwork(networkId)
                    } else {
                        repository.areTokensSupportedByNetwork(networkId, userWalletId)
                    }
                },
                catch = { throwable -> raise(throwable) },
            )
        }
    }

    operator fun invoke(networkId: String): Either<Throwable, Boolean> {
        return either {
            catch(
                block = { repository.areTokensSupportedByNetwork(networkId) },
                catch = { throwable -> raise(throwable) },
            )
        }
    }
}