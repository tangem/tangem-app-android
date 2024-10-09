package com.tangem.blockchainsdk.signer

import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner

/**
 * @author Andrew Khokhlov on 09/10/2024
 */
interface TransactionSignerFactory {

    fun createTransactionSigner(cardId: String?, sdk: TangemSdk): TransactionSigner
}
