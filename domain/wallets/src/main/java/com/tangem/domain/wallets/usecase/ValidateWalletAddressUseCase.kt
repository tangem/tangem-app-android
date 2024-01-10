package com.tangem.domain.wallets.usecase

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.repository.WalletAddressServiceRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.withContext

/**
 * Use case for validating wallet address.
 */
class ValidateWalletAddressUseCase(
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        network: Network,
        address: String,
    ): Either<Throwable, Boolean> = withContext(dispatchers.io) {
        Either.catch {
            walletAddressServiceRepository.validateAddress(userWalletId, network, address)
        }
    }
}