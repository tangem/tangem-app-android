package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.blockchain.common.MessageSigner
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.common.CompletionResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.transaction.error.SignCloreMessageError
import com.tangem.domain.walletmanager.WalletManagersFacade

class SignCloreMessageUseCase(
    private val walletManagersFacade: WalletManagersFacade,
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val getHotWalletSigner: (UserWallet.Hot) -> TransactionSigner,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        currency: CryptoCurrency,
        message: String,
    ): Either<SignCloreMessageError, String> {
        val walletManager = walletManagersFacade.getOrCreateWalletManager(
            userWalletId = userWallet.walletId,
            network = currency.network,
        )

        if (walletManager == null) {
            return SignCloreMessageError.WalletManagerNotFound.left()
        }

        if (walletManager !is MessageSigner) {
            return SignCloreMessageError.MessageSigningNotSupported.left()
        }

        val signer = when (userWallet) {
            is UserWallet.Cold -> {
                val card = userWallet.scanResponse.card
                val isCardNotBackedUp = card.backupStatus?.isActive != true
                cardSdkConfigRepository.getCommonSigner(
                    cardId = card.cardId.takeIf { isCardNotBackedUp },
                    twinKey = null,
                )
            }
            is UserWallet.Hot -> getHotWalletSigner(userWallet)
        }

        return when (val result = walletManager.signMessage(message, signer)) {
            is CompletionResult.Success -> result.data.right()
            is CompletionResult.Failure -> SignCloreMessageError.SigningFailed(
                message = result.error.message ?: "Unknown error",
            ).left()
        }
    }
}