package com.tangem.domain.managetokens

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.managetokens.repository.CustomTokensRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.coroutines.runSuspendCatching

/**
 * Checks whether the elliptic curve used by [networkId] for the [userWalletId] wallet supports the selected
 * [derivationPath]. Used before adding a custom token to prevent adding a token with a derivation path that the
 * network's curve cannot derive (e.g. an Algorand token with an EVM derivation path).
 *
 * Returns [Either.Right] with `true` when the derivation is supported, `false` otherwise. [Either.Left] is returned
 * when the check itself fails unexpectedly.
 */
class CheckDerivationPathSupportedUseCase(
    private val customTokensRepository: CustomTokensRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        networkId: Network.ID,
        derivationPath: Network.DerivationPath,
    ): Either<Throwable, Boolean> = runSuspendCatching {
        customTokensRepository.isDerivationPathSupported(
            userWalletId = userWalletId,
            networkId = networkId,
            derivationPath = derivationPath,
        )
    }.fold(
        onSuccess = { it.right() },
        onFailure = { it.left() },
    )
}