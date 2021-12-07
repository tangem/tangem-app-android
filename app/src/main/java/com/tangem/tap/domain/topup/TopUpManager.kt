package com.tangem.tap.domain.topup

import com.tangem.Message
import com.tangem.blockchain.blockchains.ethereum.EthereumWalletManager
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.AmountType
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.extensions.Result
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TangemSigner
import com.tangem.tap.store
import com.tangem.tap.tangemSdk
import java.math.BigDecimal

class TopUpManager {
    suspend fun topUpTestErc20Tokens(walletManager: EthereumWalletManager, token: Token) {
        walletManager.update()

        val amountToSend = Amount(walletManager.wallet.blockchain)
        val destinationAddress = token.contractAddress

        val feeResult = walletManager.getFee(amountToSend,
            destinationAddress) as? Result.Success ?: return
        val fee = feeResult.data[0]

        if ((walletManager.wallet.amounts[AmountType.Coin]?.value ?: BigDecimal.ZERO) < fee.value) {
            return
        }

        val transaction = walletManager.createTransaction(amountToSend, fee, destinationAddress)

        val signer = TangemSigner(
            tangemSdk = tangemSdk, Message()
        ) { signResponse ->
            store.dispatch(
                GlobalAction.UpdateWalletSignedHashes(
                    walletSignedHashes = signResponse.totalSignedHashes,
                    walletPublicKey = walletManager.wallet.publicKey.seedKey,
                    remainingSignatures = signResponse.remainingSignatures
                )
            )
        }
        walletManager.send(transaction, signer)
    }
}