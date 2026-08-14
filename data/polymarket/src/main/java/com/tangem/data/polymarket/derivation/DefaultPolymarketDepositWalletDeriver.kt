package com.tangem.data.polymarket.derivation

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.utils.extensions.hexToBytes
import com.tangem.utils.extensions.toHexString
import javax.inject.Inject

/**
 * CREATE2 (Solady ERC-1967 UUPS) derivation of the Polymarket deposit-wallet address on Polygon.
 * Algorithm confirmed byte-for-byte against Polymarket's reference derivation. Output is ERC-55 checksummed.
 */
internal class DefaultPolymarketDepositWalletDeriver @Inject constructor() : PolymarketDepositWalletDeriver {

    override fun deriveDepositWallet(ownerAddress: String): String {
        val factory = PolymarketContracts.DW_FACTORY.hexBytes()
        val walletId = ownerAddress.hexBytes().leftPad(WORD_SIZE)
        val args = factory.leftPad(WORD_SIZE) + walletId
        val salt = args.toKeccak()

        val initCode = UUPS_INIT_PREFIX +
            PolymarketContracts.DW_IMPLEMENTATION.hexBytes() +
            UUPS_PROXY_SUFFIX +
            PolymarketContracts.UUPS_INIT_CONST2.hexBytes() +
            PolymarketContracts.UUPS_INIT_CONST1.hexBytes() +
            args
        val initCodeHash = initCode.toKeccak()

        val create2Hash = (CREATE2_FF + factory + salt + initCodeHash).toKeccak()
        return create2Hash.copyOfRange(WORD_SIZE - ADDRESS_SIZE, WORD_SIZE).toErc55Address()
    }

    private fun String.hexBytes(): ByteArray = removePrefix("0x").hexToBytes()

    private fun ByteArray.leftPad(size: Int): ByteArray =
        if (this.size >= size) this else ByteArray(size - this.size) + this

    private fun ByteArray.toErc55Address(): String {
        val lower = toHexString().lowercase()
        val hash = lower.toByteArray().toKeccak().toHexString().lowercase()
        val out = StringBuilder("0x")
        lower.forEachIndexed { i, c ->
            val shouldUppercase = c in 'a'..'f' && Character.digit(hash[i], HEX_RADIX) >= CHECKSUM_THRESHOLD
            out.append(if (shouldUppercase) c.uppercaseChar() else c)
        }
        return out.toString()
    }

    private companion object {
        const val WORD_SIZE = 32
        const val ADDRESS_SIZE = 20
        const val CHECKSUM_THRESHOLD = 8
        const val HEX_RADIX = 16

        // prefix10 = 0x61003d3d8160233d3973 + (argsLen << 56); argsLen is invariant 64 → this constant.
        val UUPS_INIT_PREFIX = "61007d3d8160233d3973".hexToBytes()
        val UUPS_PROXY_SUFFIX = "6009".hexToBytes()
        val CREATE2_FF = byteArrayOf(0xff.toByte())
    }
}