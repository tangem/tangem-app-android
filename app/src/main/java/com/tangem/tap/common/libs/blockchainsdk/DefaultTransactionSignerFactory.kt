package com.tangem.tap.common.libs.blockchainsdk

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.core.analytics.models.Basic.TransactionSent.WalletForm
import com.tangem.core.analytics.store.LastSignedWalletFormStore
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.domain.card.models.TwinKey
import com.tangem.tap.domain.TangemSigner

internal class DefaultTransactionSignerFactory(
    private val lastSignedWalletFormStore: LastSignedWalletFormStore,
) : TransactionSignerFactory {

    override fun createTransactionSigner(cardId: String?, sdk: TangemSdk, twinKey: TwinKey?): TransactionSigner {
        return TangemSigner(
            cardId = cardId,
            tangemSdk = sdk,
            initialMessage = Message(),
            twinKey = twinKey,
        ) { signResponse ->
            lastSignedWalletFormStore.update(
                if (signResponse.isRing) WalletForm.Ring else WalletForm.Card,
            )
        }
    }
}