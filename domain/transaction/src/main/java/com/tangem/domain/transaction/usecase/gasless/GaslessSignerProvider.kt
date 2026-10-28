package com.tangem.domain.transaction.usecase.gasless

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.card.common.TapWorkarounds.isTangemTwins
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWallet

class GaslessSignerProvider(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val getHotWalletSigner: (UserWallet.Hot) -> TransactionSigner,
) {

    operator fun invoke(userWallet: UserWallet): TransactionSigner {
        return when (userWallet) {
            is UserWallet.Cold -> {
                val card = userWallet.scanResponse.card
                val isCardNotBackedUp = card.backupStatus?.isActive != true && !card.isTangemTwins

                cardSdkConfigRepository.getCommonSigner(
                    cardId = card.cardId.takeIf { isCardNotBackedUp },
                    twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
                    userWalletId = userWallet.walletId,
                )
            }
            is UserWallet.Hot -> getHotWalletSigner(userWallet)
        }
    }
}