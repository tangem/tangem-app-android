package com.tangem.tap.common.libs.blockchainsdk

import com.tangem.Message
import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.data.card.TransactionSignerFactory
import com.tangem.domain.card.models.TwinKey
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.store

internal class DefaultTransactionSignerFactory : TransactionSignerFactory {

    override fun createTransactionSigner(cardId: String?, sdk: TangemSdk, twinKey: TwinKey?): TransactionSigner {
        return TangemSigner(
            cardId = cardId,
            tangemSdk = sdk,
            initialMessage = Message(),
            twinKey = twinKey,
        ) { signResponse ->
            store.dispatch(action = GlobalAction.IsSignWithRing(signResponse.isRing))
        }
    }
}