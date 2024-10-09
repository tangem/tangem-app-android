package com.tangem.blockchainsdk.signer

import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner

/**
[REDACTED_AUTHOR]
 */
interface TransactionSignerFactory {

    fun createTransactionSigner(cardId: String?, sdk: TangemSdk): TransactionSigner
}