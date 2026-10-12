package com.tangem.blockchainsdk.yield

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.smartcontract.SmartContractCallData
import com.tangem.common.extensions.hexToBytes

/** Backend-built payload of a Merkl rewards claim; the bytes are opaque and are never parsed or rebuilt. */
data class MerklClaimCallData(override val data: ByteArray) : SmartContractCallData {

    override val methodId: String = data.take(METHOD_ID_LENGTH)
        .joinToString(separator = "", prefix = "0x") { byte -> "%02x".format(byte) }

    override fun validate(blockchain: Blockchain): Boolean = blockchain.isEvm()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MerklClaimCallData) return false

        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int = data.contentHashCode()

    companion object {

        private const val METHOD_ID_LENGTH = 4
        private const val HEX_PREFIX = "0x"

        /** Decodes the hex payload the claim endpoint reports, with or without the `0x` prefix. */
        fun fromHex(data: String): MerklClaimCallData = MerklClaimCallData(data.removePrefix(HEX_PREFIX).hexToBytes())
    }
}