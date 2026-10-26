package com.tangem.data.jointaccount.signing

import arrow.core.Either
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils
import com.tangem.blockchain.blockchains.ethereum.EthereumUtils.WORD_BYTES
import com.tangem.blockchain.common.HEX_PREFIX
import com.tangem.blockchain.extensions.isValidHex
import com.tangem.common.extensions.hexToBytes
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.signing.CanonicalJson
import com.tangem.domain.jointaccount.signing.JointAccountSignablePayload
import com.tangem.domain.jointaccount.signing.JointSignatureRecoverer
import org.web3j.crypto.ECDSASignature
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.Sign.CURVE_PARAMS
import java.math.BigInteger
import javax.inject.Inject

/** Recovery is `ecrecover` as the Safe contract runs it, over the digests the app signs with */
internal class DefaultJointSignatureRecoverer @Inject constructor() : JointSignatureRecoverer {

    override fun recoverPayloadSigner(
        payload: JointAccountSignablePayload,
        signature: String,
    ): Either<Throwable, String> = Either.catch {
        val digest = EthereumUtils.hashPersonalMessage(message = CanonicalJson.canonicalize(payload.toCanonicalMap()))

        recover(digest = digest, signature = signature)
    }

    override fun recoverSafeTxSigner(
        safeTx: SafeTx,
        chainId: Long,
        safeAddress: String,
        signature: String,
    ): Either<Throwable, String> = Either.catch {
        val digest = SafeTxTypedDataBuilder.digest(safeTx = safeTx, chainId = chainId, verifyingContract = safeAddress)

        recover(digest = digest, signature = signature)
    }

    private fun recover(digest: ByteArray, signature: String): String {
        val hex = signature.removePrefix(prefix = HEX_PREFIX).removePrefix(prefix = HEX_PREFIX.uppercase())
        require(hex.length == SIGNATURE_BYTES * 2 && hex.isValidHex()) { "The signature is not 65 hex bytes" }
        val bytes = hex.hexToBytes()

        val r = BigInteger(1, bytes.copyOfRange(0, WORD_BYTES))
        val s = BigInteger(1, bytes.copyOfRange(WORD_BYTES, WORD_BYTES * 2))
        val v = bytes[SIGNATURE_BYTES - 1].toInt() and BYTE_MASK
        require(r.isScalar() && s.isScalar()) { "The signature r and s must lie in (0, n)" }
        require(v == V_BASE || v == V_BASE + 1) { "The signature v must be 27 or 28, got $v" }

        val publicKey = Sign.recoverFromSignature(v - V_BASE, ECDSASignature(r, s), digest)
        requireNotNull(publicKey) { "The signature recovers to no point on the curve" }

        return HEX_PREFIX + Keys.getAddress(publicKey).lowercase()
    }

    /** `ecrecover` returns the zero address outside this range, while web3j still recovers a point for `s = 0` */
    private fun BigInteger.isScalar(): Boolean = signum() > 0 && this < CURVE_PARAMS.n

    private companion object {
        const val SIGNATURE_BYTES = 65
        const val V_BASE = 27
        const val BYTE_MASK = 0xff
    }
}