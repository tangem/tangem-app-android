package com.tangem.domain.transaction.usecase

import arrow.core.Either
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.WalletAddressServiceRepository

/**
 * Use case for validating wallet memo.
 */
class ValidateWalletMemoUseCase(
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
) {

    operator fun invoke(network: Network, memo: String): Either<Throwable, Boolean> = Either.catch {
        walletAddressServiceRepository.validateMemo(network, memo)
    }
}