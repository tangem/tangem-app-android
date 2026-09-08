package com.tangem.domain.jointaccount.safe

import com.tangem.domain.jointaccount.model.SafeTx
import org.spongycastle.crypto.digests.KeccakDigest
import java.math.BigInteger

object SafeTxTypedDataBuilder {

    /**
     * The exact string behind Safe v1.5.0 `SAFE_TX_TYPEHASH` — one changed character
     * changes every digest, so the hash is pinned by test.
     */
    private const val SAFE_TX_TYPE = "SafeTx(address to,uint256 value,bytes data,uint8 operation," +
        "uint256 safeTxGas,uint256 baseGas,uint256 gasPrice,address gasToken,address refundReceiver," +
        "uint256 nonce)"

    /** The Safe domain has no name and no version — only the chain and the account's own address */
    private const val DOMAIN_TYPE = "EIP712Domain(uint256 chainId,address verifyingContract)"

    private const val KECCAK_BITS = 256
    private val UINT8_RANGE = 0..255

    private val EIP712_PREFIX = byteArrayOf(0x19, 0x01)

    fun safeTxTypeHash(): ByteArray = SAFE_TX_TYPE.toByteArray(Charsets.UTF_8).keccak()

    fun domainTypeHash(): ByteArray = DOMAIN_TYPE.toByteArray(Charsets.UTF_8).keccak()

    fun domainSeparator(chainId: Long, verifyingContract: String): ByteArray = concat(
        domainTypeHash(),
        BigInteger.valueOf(chainId).toUint256Word(),
        verifyingContract.toAddressWord(),
    ).keccak()

    fun digest(safeTx: SafeTx, chainId: Long, verifyingContract: String): ByteArray = concat(
        EIP712_PREFIX,
        domainSeparator(chainId = chainId, verifyingContract = verifyingContract),
        hashStruct(safeTx),
    ).keccak()

    /** The same payload as an `eth_signTypedData_v4` JSON — for the Blockaid gate and the web3j cross-check in tests */
    fun buildJson(safeTx: SafeTx, chainId: Long, verifyingContract: String): String {
        requireHashable(safeTx = safeTx, verifyingContract = verifyingContract)

        return buildString {
            append("""{"types":{"EIP712Domain":[""")
            append("""{"name":"chainId","type":"uint256"},{"name":"verifyingContract","type":"address"}""")
            append("""],"SafeTx":[""")
            append("""{"name":"to","type":"address"},{"name":"value","type":"uint256"},""")
            append("""{"name":"data","type":"bytes"},{"name":"operation","type":"uint8"},""")
            append("""{"name":"safeTxGas","type":"uint256"},{"name":"baseGas","type":"uint256"},""")
            append("""{"name":"gasPrice","type":"uint256"},{"name":"gasToken","type":"address"},""")
            append("""{"name":"refundReceiver","type":"address"},{"name":"nonce","type":"uint256"}""")
            append("""]},"primaryType":"SafeTx","domain":{""")
            append(""""chainId":$chainId,"verifyingContract":"${verifyingContract.lowercase()}"""")
            append("""},"message":{""")
            append(""""to":"${safeTx.to.lowercase()}",""")
            append(""""value":"${safeTx.value}",""")
            append(""""data":"${normalizedData(safeTx.data)}",""")
            append(""""operation":${safeTx.operation},""")
            append(""""safeTxGas":"${safeTx.safeTxGas}",""")
            append(""""baseGas":"${safeTx.baseGas}",""")
            append(""""gasPrice":"${safeTx.gasPrice}",""")
            append(""""gasToken":"${safeTx.gasToken.lowercase()}",""")
            append(""""refundReceiver":"${safeTx.refundReceiver.lowercase()}",""")
            append(""""nonce":"${safeTx.nonce}"""")
            append("}}")
        }
    }

    /** The JSON must describe exactly what [digest] hashes — running the same encoding rejects the same inputs */
    private fun requireHashable(safeTx: SafeTx, verifyingContract: String) {
        verifyingContract.toAddressWord()
        hashStruct(safeTx)
    }

    private fun hashStruct(safeTx: SafeTx): ByteArray {
        require(safeTx.operation in UINT8_RANGE) { "operation is a uint8 in the Safe schema, got ${safeTx.operation}" }

        return concat(
            safeTxTypeHash(),
            safeTx.to.toAddressWord(),
            safeTx.value.toUint256Word(),
            dataBytes(safeTx.data).keccak(),
            BigInteger.valueOf(safeTx.operation.toLong()).toUint256Word(),
            safeTx.safeTxGas.toUint256Word(),
            safeTx.baseGas.toUint256Word(),
            safeTx.gasPrice.toUint256Word(),
            safeTx.gasToken.toAddressWord(),
            safeTx.refundReceiver.toAddressWord(),
            safeTx.nonce.toUint256Word(),
        ).keccak()
    }

    private fun normalizedData(data: String): String = "0x" + data.removePrefix(prefix = "0x").lowercase()

    private fun dataBytes(data: String): ByteArray = data.hexPayloadToBytes()

    private fun concat(vararg parts: ByteArray): ByteArray = parts.reduce(ByteArray::plus)

    private fun ByteArray.keccak(): ByteArray {
        val digest = KeccakDigest(KECCAK_BITS)
        digest.update(this, 0, size)

        return ByteArray(digest.digestSize).also { digest.doFinal(it, 0) }
    }
}