package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.transaction.WalletAddressServiceRepository
import com.tangem.domain.transaction.error.ValidateMemoError

/**
 * Use case for validating wallet memo.
 */
class ValidateWalletMemoUseCase(
    private val walletAddressServiceRepository: WalletAddressServiceRepository,
) {

    operator fun invoke(network: Network, memo: String): Either<ValidateMemoError, Unit> {
        return try {
            val isValidMemo = walletAddressServiceRepository.validateMemo(network, memo)
            if (isValidMemo) {
                Unit.right()
            } else {
                ValidateMemoError.InvalidMemo.left()
            }
        } catch (ex: Throwable) {
            ValidateMemoError.DataError(ex).left()
        }
    }
}