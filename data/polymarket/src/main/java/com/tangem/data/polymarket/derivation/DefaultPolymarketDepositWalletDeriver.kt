package com.tangem.data.polymarket.derivation

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.derivation.PolymarketDepositWalletDeriver
import com.tangem.utils.extensions.hexToBytes
import com.tangem.utils.extensions.toHexString
import javax.inject.Inject

/**
 * CREATE2 (Solady ERC-1967 beacon) derivation of the Polymarket deposit-wallet address on Polygon.
 * Pinned by known-answer vectors read off real factory deployments. Output is ERC-55 checksummed.
 */
internal class DefaultPolymarketDepositWalletDeriver @Inject constructor() : PolymarketDepositWalletDeriver {

    override fun deriveDepositWallet(ownerAddress: String): String {
        val factory = PolymarketContracts.DW_FACTORY.hexBytes()
        val args = factory.leftPad(WORD_SIZE) + paddedOwner(ownerAddress)
        val salt = args.toKeccak()

        val initCode = PolymarketContracts.DW_BEACON_INIT_PREFIX.hexBytes() +
            PolymarketContracts.DW_BEACON.hexBytes() +
            PolymarketContracts.DW_BEACON_INIT_SUFFIX.hexBytes() +
            args
        val initCodeHash = initCode.toKeccak()

        val create2Hash = (CREATE2_FF + factory + salt + initCodeHash).toKeccak()
        return create2Hash.copyOfRange(WORD_SIZE - ADDRESS_SIZE, WORD_SIZE).toErc55Address()
    }

    /**
     * The proxy's second constructor argument: the owner left-padded to a 32-byte word. Despite the shape,
     * this is not the `walletId` the BFF asks for — that one is the Tangem wallet id and has nothing to do
     * with this derivation.
     */
    private fun paddedOwner(ownerAddress: String): ByteArray = ownerAddress.hexBytes().leftPad(WORD_SIZE)

    private fun String.hexBytes(): ByteArray = removePrefix(HEX_PREFIX).hexToBytes()

    private fun ByteArray.leftPad(size: Int): ByteArray =
        if (this.size >= size) this else ByteArray(size - this.size) + this

    /**
     * Written here rather than reused: the Blockchain SDK's checksum comes from kethereum, which it declares
     * as `implementation`, so `withERC55Checksum` never reaches this classpath. The one public entry point,
     * `EthereumAddressService.makeAddress`, starts from a public key, while a CREATE2 result is already the
     * twenty address bytes. Pinned by the known-answer vectors, whose expected values are checksummed.
     */
    private fun ByteArray.toErc55Address(): String {
        val lower = toHexString().lowercase()
        val hash = lower.toByteArray().toKeccak().toHexString().lowercase()
        val out = StringBuilder(HEX_PREFIX)
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
        const val HEX_PREFIX = "0x"

        val CREATE2_FF = byteArrayOf(0xff.toByte())
    }
}