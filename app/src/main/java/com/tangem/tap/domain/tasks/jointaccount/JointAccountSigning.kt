package com.tangem.tap.domain.tasks.jointaccount

import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.toKeccak
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.UnmarshalHelper
import com.tangem.common.extensions.toDecompressedPublicKey
import com.tangem.common.extensions.toHexString
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey

/**
 * The parts of joint account signing that must be identical on every wallet type: a cold card and a hot wallet
 * derive the same owner key and have to produce the same address, the same digest and the same signature
 * encoding — otherwise the backend would accept one kind of wallet and reject the other.
 */
internal object JointAccountSigning {

    private const val EIP191_PREFIX = "\u0019Ethereum Signed Message:\n"

    /** The owner address in a Safe: the EVM address of the derived owner key, EIP-55 checksummed. */
    fun evmAddress(ownerKey: ExtendedPublicKey): String {
        return Blockchain.Ethereum.makeAddressesFromExtendedPublicKey(
            extendedPublicKey = ownerKey,
            rawPath = null,
            cachedIndex = null,
        ).address
    }

    /** EIP-191 (`0x45`) digest of the canonical payload — what the owner key actually signs. */
    fun eip191Digest(canonicalPayload: ByteArray): ByteArray {
        val prefix = (EIP191_PREFIX + canonicalPayload.size).toByteArray()

        return (prefix + canonicalPayload).toKeccak()
    }

    /** `0x` + 130 hex chars (65 bytes R‖S‖V, v ∈ {27, 28}) — the form the backend recovers the signer from. */
    fun toRsvHex(signature: ByteArray, digest: ByteArray, ownerKey: ExtendedPublicKey): String {
        val rsv = UnmarshalHelper.unmarshalSignatureExtended(
            signature = signature,
            hash = digest,
            publicKey = ownerKey.publicKey.toDecompressedPublicKey(),
        ).asRSVLegacyEVM()

        return "0x" + rsv.toHexString().lowercase()
    }
}