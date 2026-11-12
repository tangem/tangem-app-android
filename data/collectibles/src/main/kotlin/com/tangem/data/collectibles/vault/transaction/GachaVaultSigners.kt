package com.tangem.data.collectibles.vault.transaction

import com.tangem.blockchain.common.TransactionSigner
import com.tangem.common.core.CardSession
import com.tangem.data.collectibles.vault.sessionCardId
import com.tangem.data.wallets.hot.TangemHotWalletSigner
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWallet
import javax.inject.Inject

/** The signer of the vault owner: the card, or the mobile wallet's seed. */
internal class GachaVaultSigners @Inject constructor(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val hotWalletSignerFactory: TangemHotWalletSigner.Factory,
) {

    fun signer(userWallet: UserWallet): TransactionSigner = when (userWallet) {
        is UserWallet.Cold -> cardSdkConfigRepository.getCommonSigner(
            cardId = userWallet.sessionCardId(),
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
            userWalletId = userWallet.walletId,
        )
        is UserWallet.Hot -> hotWalletSignerFactory.create(userWallet)
    }

    fun signer(userWallet: UserWallet.Cold, session: CardSession): TransactionSigner =
        cardSdkConfigRepository.getCommonSigner(
            cardId = userWallet.sessionCardId(),
            twinKey = TwinKey.getOrNull(scanResponse = userWallet.scanResponse),
            userWalletId = userWallet.walletId,
            session = session,
        )
}