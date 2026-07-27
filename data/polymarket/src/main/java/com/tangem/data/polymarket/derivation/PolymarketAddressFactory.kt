package com.tangem.data.polymarket.derivation

import com.tangem.blockchain.common.Blockchain
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import javax.inject.Inject

/**
 * Converts a secp256k1 [ExtendedPublicKey] into a Polygon ERC-55 checksummed address.
 */
class PolymarketAddressFactory @Inject constructor() {

    fun createAddress(extendedPublicKey: ExtendedPublicKey): String {
        return Blockchain.Polygon.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = extendedPublicKey,
            rawPath = null,
            cachedIndex = null,
        ).address
    }
}