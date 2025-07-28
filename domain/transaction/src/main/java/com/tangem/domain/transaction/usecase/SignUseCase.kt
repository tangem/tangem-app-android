package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemError
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.network.Network
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.requireColdWallet

class SignUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {
    suspend operator fun invoke(
        hash: ByteArray,
        userWallet: UserWallet,
        network: Network,
    ): Either<TangemError, ByteArray> {
        userWallet.requireColdWallet() // TODO [REDACTED_TASK_KEY]
        val card = userWallet.scanResponse.card
        val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

        val signer = cardSdkConfigRepository.getCommonSigner(
            cardId = card.cardId.takeIf { isCardNotBackedUp },
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
        )

        val walletManager = walletManagersFacade.getOrCreateWalletManager(userWallet.walletId, network)
            ?: error("WalletManager not found")

        return when (val signResult = signer.sign(hash, walletManager.wallet.publicKey)) {
            is CompletionResult.Failure -> signResult.error.left()
            is CompletionResult.Success -> signResult.data.right()
        }
    }
}