package com.tangem.data.polymarket.signing

import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.formatHex
import com.tangem.common.extensions.toHexString
import javax.inject.Inject

/**
 * Converts a raw `r‖s` secp256k1 signature into the `0x`-prefixed 65-byte R‖S‖V string Polymarket expects.
 */
class PolymarketSignatureFormatter @Inject constructor() {

    fun format(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): String {
        return UnmarshalHelper
            .unmarshalSignatureExtended(signature = signature, hash = hash, publicKey = publicKey)
            .asRSVLegacyEVM()
            .toHexString()
            .formatHex()
            .lowercase()
    }
}