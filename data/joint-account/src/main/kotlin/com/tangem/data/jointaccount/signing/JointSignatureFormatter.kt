package com.tangem.data.jointaccount.signing

import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.extensions.formatHex
import com.tangem.common.extensions.toHexString
import javax.inject.Inject

/**
 * Converts a raw `r‖s` secp256k1 signature into the `0x`-prefixed 65-byte R‖S‖V string of the joint
 * account contract calls, `v ∈ {27, 28}`.
 */
class JointSignatureFormatter @Inject constructor() {

    fun format(signature: ByteArray, hash: ByteArray, publicKey: Wallet.PublicKey): String {
        return UnmarshalHelper
            .unmarshalSignatureExtended(signature = signature, hash = hash, publicKey = publicKey)
            .asRSVLegacyEVM()
            .toHexString()
            .formatHex()
            .lowercase()
    }
}