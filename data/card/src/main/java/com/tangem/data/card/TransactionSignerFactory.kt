package com.tangem.data.card

import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.models.wallet.UserWalletId

/**
[REDACTED_AUTHOR]
 */
interface TransactionSignerFactory {

    fun createTransactionSigner(
        cardId: String?,
        sdk: TangemSdk,
        twinKey: TwinKey?,
        userWalletId: UserWalletId,
    ): TransactionSigner
}