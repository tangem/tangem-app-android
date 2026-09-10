package com.tangem.domain.pay.usecase.bankofframp

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.pay.WithdrawalSignatureResult
import com.tangem.domain.pay.datasource.TangemPayAuthDataSource
import com.tangem.domain.pay.model.bankofframp.BankOfframpError
import com.tangem.domain.pay.model.bankofframp.BankOfframpSignResult
import com.tangem.domain.pay.repository.BankOfframpRepository

/** Signs the order payload with the card (or hot wallet) and submits the signature to the backend. */
class SignBankOfframpOrderUseCase(
    private val repository: BankOfframpRepository,
    private val authDataSource: TangemPayAuthDataSource,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        orderId: String,
        payloadHash: String,
    ): Either<BankOfframpError, BankOfframpSignResult> = either {
        val signature = authDataSource.getWithdrawalSignature(userWallet = userWallet, hash = payloadHash)
            .mapLeft { BankOfframpError.Unknown(cause = it) }
            .bind()
        when (signature) {
            is WithdrawalSignatureResult.Cancelled -> BankOfframpSignResult.Cancelled
            is WithdrawalSignatureResult.Success -> {
                repository.submitSignature(
                    userWalletId = userWallet.walletId,
                    orderId = orderId,
                    signature = signature.signature,
                ).bind()
                BankOfframpSignResult.Signed
            }
        }
    }
}