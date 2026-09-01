package com.tangem.data.polymarket.builder

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PredictionOrder
import com.tangem.utils.extensions.addHexPrefix
import com.tangem.utils.extensions.toHexString

internal object PolymarketErc7739SignatureBuilder {

    private const val WALLET_DOMAIN_NAME = "DepositWallet"
    private const val WALLET_DOMAIN_VERSION = "1"

    private const val WORD_SIZE = 32
    private const val ADDRESS_SIZE = 20
    private const val BYTE_MASK = 0xFF
    private const val HEX_CHARS_PER_BYTE = 2
    private const val HEX_RADIX = 16

    const val TAIL_SIZE_AFTER_V = 252

    private val EIP191_PREFIX = byteArrayOf(0x19, 0x01)

    fun digest(order: PredictionOrder, isNegRisk: Boolean, depositWalletAddress: String): ByteArray {
        val parent = concat(
            typedDataSignTypeHash(),
            PolymarketOrderTypedDataBuilder.hashStruct(order),
            WALLET_DOMAIN_NAME.toByteArray().toKeccak(),
            WALLET_DOMAIN_VERSION.toByteArray().toKeccak(),
            chainIdWord(),
            addressWord(depositWalletAddress),
            ByteArray(WORD_SIZE),
        ).toKeccak()

        return concat(
            EIP191_PREFIX,
            PolymarketOrderTypedDataBuilder.domainSeparator(isNegRisk),
            parent,
        ).toKeccak()
    }

    fun wrap(innerSignature: String, order: PredictionOrder, isNegRisk: Boolean): String {
        val description = PolymarketOrderTypedDataBuilder.orderTypeString().toByteArray()

        return (
            innerSignature.removePrefix(PolymarketContracts.HEX_PREFIX).lowercase() +
                concat(
                    PolymarketOrderTypedDataBuilder.domainSeparator(isNegRisk),
                    PolymarketOrderTypedDataBuilder.hashStruct(order),
                    description,
                    lengthSuffix(description.size),
                ).toHexString().lowercase()
            ).addHexPrefix()
    }

    private fun typedDataSignTypeHash(): ByteArray = (
        "TypedDataSign(Order contents,string name,string version,uint256 chainId," +
            "address verifyingContract,bytes32 salt)" + PolymarketOrderTypedDataBuilder.orderTypeString()
        ).toByteArray().toKeccak()

    private fun lengthSuffix(length: Int): ByteArray = byteArrayOf(
        (length shr java.lang.Byte.SIZE).toByte(),
        (length and BYTE_MASK).toByte(),
    )

    private fun chainIdWord(): ByteArray {
        val bytes = PolymarketContracts.CHAIN_ID.toBigInteger().toByteArray()

        return ByteArray(WORD_SIZE - bytes.size) + bytes
    }

    private fun addressWord(address: String): ByteArray {
        val hex = address.removePrefix(PolymarketContracts.HEX_PREFIX)
        require(hex.length == ADDRESS_SIZE * HEX_CHARS_PER_BYTE) { "Address must be $ADDRESS_SIZE bytes: $address" }

        val bytes = hex.chunked(size = HEX_CHARS_PER_BYTE).map { it.toInt(radix = HEX_RADIX).toByte() }.toByteArray()

        return ByteArray(WORD_SIZE - ADDRESS_SIZE) + bytes
    }

    private fun concat(vararg parts: ByteArray): ByteArray = parts.reduce { acc, part -> acc + part }
}