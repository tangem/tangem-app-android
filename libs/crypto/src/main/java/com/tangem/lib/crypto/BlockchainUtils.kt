package com.tangem.lib.crypto

import com.tangem.blockchain.blockchains.xrp.XrpAddressService
import com.tangem.blockchain.common.Blockchain
import com.tangem.lib.crypto.converter.XrpTaggedAddressConverter
import com.tangem.lib.crypto.models.XrpTaggedAddress

/**
 * !!!IMPORTANT!!!
 * Methods for working with different blockchains
 * All methods are depend on specific blockchain or check for specific blockchain
 *
 * Temporary solution for domain specific logic for Blockchain.
 * Instead of creating repositories and unnecessary and overkill use cases
 */
object BlockchainUtils {

    private const val XRP_X_ADDRESS = 'X'

    /** Decodes XRP Blockchain address */
    fun decodeRippleXAddress(xAddress: String, networkId: String): XrpTaggedAddress? {
        return if (networkId == Blockchain.XRP.id && xAddress.firstOrNull() == XRP_X_ADDRESS) {
            val decodedAddress = XrpAddressService.decodeXAddress(xAddress)
            return decodedAddress?.let(XrpTaggedAddressConverter()::convert)
        } else {
            null
        }
    }

    /** If current [networkId] is Bitcoin */
    fun isBitcoin(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Bitcoin || blockchain == Blockchain.BitcoinTestnet
    }

    /** If current [networkId] is Dogecoin */
    fun isDogecoin(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Dogecoin
    }

    /** If current [networkId] is Tezos */
    fun isTezos(networkId: String): Boolean {
        val blockchain = Blockchain.fromId(networkId)
        return blockchain == Blockchain.Tezos
    }
}
