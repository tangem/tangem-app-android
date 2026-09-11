package com.tangem.domain.jointaccount.safe

import com.tangem.domain.jointaccount.model.JointOperationTransfer
import com.tangem.domain.jointaccount.model.JointTransferIntent
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network

/**
 * Reads the transfer out of a pending operation's [SafeTx] and matches it to a currency the account holds.
 * Anything the account cannot name — a delegate call, an unreadable payload, a token that is not in the account, a
 * call the Safe makes to itself — is `null`: the operation cannot be shown, so it cannot be signed from this build.
 *

 * with, so nothing reaches the screen that [com.tangem.domain.jointaccount.verifier.JointOperationVerifier] would
 * refuse to sign.
 */
class JointOperationTransferResolver(private val payloadCodec: JointTransferPayloadCodec) {

    fun resolve(
        safeTx: SafeTx,
        currencies: List<CryptoCurrency>,
        network: Network,
        safeAddress: String,
    ): JointOperationTransfer? {
        if (safeTx.operation != SafeTx.OPERATION_CALL) return null
        if (safeTx.to.equals(safeAddress, ignoreCase = true)) return null

        return when (val intent = payloadCodec.decode(safeTx)) {
            is JointTransferIntent.Native -> currencies
                .firstOrNull { it is CryptoCurrency.Coin && it.network == network }
                ?.let { coin -> JointOperationTransfer(currency = coin, intent = intent) }
            is JointTransferIntent.Erc20 -> currencies
                .filterIsInstance<CryptoCurrency.Token>()
                .firstOrNull { token ->
                    token.network == network && token.contractAddress.equals(intent.tokenContract, ignoreCase = true)
                }
                ?.let { token -> JointOperationTransfer(currency = token, intent = intent) }
            null -> null
        }
    }
}