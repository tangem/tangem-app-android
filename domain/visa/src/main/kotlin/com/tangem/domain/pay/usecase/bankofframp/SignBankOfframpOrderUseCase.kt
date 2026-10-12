package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensureNotNull
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.repository.BankOfframpOrderRepository

/**
 * Signs the order payload with the card (or hot wallet) and submits the signature to the backend.
 *

 */
class SignBankOfframpOrderUseCase(
    private val repository: BankOfframpOrderRepository,
    private val authDataSource: TangemPayAuthDataSource,
    private val userWalletsListRepository: UserWalletsListRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        orderId: String,
        payloadHash: String,
        idempotencyKey: String,
    ): Either<BankOfframpError, BankOfframpSignResult> = either {
        val userWallet = userWalletsListRepository.userWallets.value
            ?.firstOrNull { wallet -> wallet.walletId == userWalletId }
        ensureNotNull(userWallet) {
            BankOfframpError.Unknown(
                cause = IllegalStateException("No wallet $userWalletId to sign order $orderId with"),
            )
        }
        val signature = authDataSource.getWithdrawalSignature(userWallet = userWallet, hash = payloadHash)
            .mapLeft { BankOfframpError.Unknown(cause = it) }
            .bind()
        when (signature) {
            is WithdrawalSignatureResult.Cancelled -> BankOfframpSignResult.Cancelled
            is WithdrawalSignatureResult.Success -> {
                repository.submitSignature(
                    userWalletId = userWalletId,
                    orderId = orderId,
                    signature = signature.signature,
                    idempotencyKey = idempotencyKey,
                ).bind()
                BankOfframpSignResult.Signed
            }
        }
    }
}