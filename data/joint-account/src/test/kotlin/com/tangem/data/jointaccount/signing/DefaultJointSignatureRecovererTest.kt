package com.tangem.data.jointaccount.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.jointaccount.model.JointAccountConfig
import com.tangem.domain.jointaccount.model.JointAccountConfirmationPayload
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.domain.jointaccount.signing.CanonicalJson
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Keys
import org.web3j.crypto.Sign
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

/**
 * The signatures are produced by an independent implementation (web3j) over independently computed digests:
 * `personal_sign` for the confirmation payload, web3j's own EIP-712 encoding for the `SafeTx`. The recoverer
 * must land on the signer's address, and on nobody else's.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointSignatureRecovererTest {

    private val recoverer = DefaultJointSignatureRecoverer()

    @Test
    fun `GIVEN a confirmation signed by alice WHEN recoverPayloadSigner THEN alice`() {
        // Arrange
        val signature = Sign.signPrefixedMessage(CanonicalJson.canonicalize(PAYLOAD.toCanonicalMap()), ALICE_KEY)

        // Act
        val actual = recoverer.recoverPayloadSigner(payload = PAYLOAD, signature = signature.toRsvHex())

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(ALICE)
    }

    @Test
    fun `GIVEN a confirmation of another payload WHEN recoverPayloadSigner THEN not the signer`() {
        // Arrange
        val other = PAYLOAD.copy(config = PAYLOAD.config.copy(threshold = 1))
        val signature = Sign.signPrefixedMessage(CanonicalJson.canonicalize(other.toCanonicalMap()), ALICE_KEY)

        // Act
        val actual = recoverer.recoverPayloadSigner(payload = PAYLOAD, signature = signature.toRsvHex())

        // Assert
        assertThat(actual.getOrNull()).isNotEqualTo(ALICE)
    }

    @Test
    fun `GIVEN a SafeTx signed by bob WHEN recoverSafeTxSigner THEN bob`() {
        // Arrange
        val json = SafeTxTypedDataBuilder.buildJson(SAFE_TX, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        val signature = Sign.signMessage(StructuredDataEncoder(json).hashStructuredData(), BOB_KEY, false)

        // Act
        val actual = recoverer.recoverSafeTxSigner(
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            signature = signature.toRsvHex(),
        )

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(BOB)
    }

    @Test
    fun `GIVEN a SafeTx signed for another chain WHEN recoverSafeTxSigner THEN not the signer`() {
        // Arrange
        val json = SafeTxTypedDataBuilder.buildJson(SAFE_TX, chainId = CHAIN_ID + 1, verifyingContract = SAFE_ADDRESS)
        val signature = Sign.signMessage(StructuredDataEncoder(json).hashStructuredData(), BOB_KEY, false)

        // Act
        val actual = recoverer.recoverSafeTxSigner(
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
            signature = signature.toRsvHex(),
        )

        // Assert
        assertThat(actual.getOrNull()).isNotEqualTo(BOB)
    }

    @Test
    fun `GIVEN a checksummed Safe address WHEN recoverSafeTxSigner THEN the digest is the same`() {
        // Arrange
        val json = SafeTxTypedDataBuilder.buildJson(SAFE_TX, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        val signature = Sign.signMessage(StructuredDataEncoder(json).hashStructuredData(), BOB_KEY, false)

        // Act
        val actual = recoverer.recoverSafeTxSigner(
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = Keys.toChecksumAddress(SAFE_ADDRESS),
            signature = signature.toRsvHex(),
        )

        // Assert
        assertThat(actual.getOrNull()).isEqualTo(BOB)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `malformed signatures are refused`(model: MalformedModel) {
        // Act
        val actual = recoverer.recoverPayloadSigner(payload = PAYLOAD, signature = model.signature)

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    private fun provideTestModels(): List<MalformedModel> {
        val valid = Sign
            .signPrefixedMessage(CanonicalJson.canonicalize(PAYLOAD.toCanonicalMap()), ALICE_KEY)
            .toRsvHex()

        return listOf(
            MalformedModel(signature = ""),
            MalformedModel(signature = "0x"),
            MalformedModel(signature = valid.dropLast(n = 2)),
            MalformedModel(signature = valid + "00"),
            MalformedModel(signature = valid.dropLast(n = 2) + "00"),
            MalformedModel(signature = valid.dropLast(n = 2) + "1d"),
            MalformedModel(signature = valid.dropLast(n = 4) + "zz1b"),
        )
    }

    data class MalformedModel(val signature: String)

    private fun Sign.SignatureData.toRsvHex(): String {
        return "0x" + (r + s + v).joinToString(separator = "") { "%02x".format(it) }
    }

    private companion object {
        val ALICE_KEY: ECKeyPair = ECKeyPair.create(BigInteger.ONE)
        val BOB_KEY: ECKeyPair = ECKeyPair.create(BigInteger.TWO)

        /** The address of private key 1, a well-known vector: the recoverer is checked against a literal */
        const val ALICE = "0x7e5f4552091a69125d5dfcb7b8c2659029395bdf"
        const val BOB = "0x2b5ad5c4795c026514f8317c7a215e218dccd6cf"
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val CHAIN_ID = 137L

        val PAYLOAD = JointAccountConfirmationPayload(
            config = JointAccountConfig(
                name = "Family",
                icon = "Star",
                iconColor = "Azure",
                membersCount = 2,
                threshold = 2,
            ),
            safeAddress = SAFE_ADDRESS,
        )

        val SAFE_TX = SafeTx(
            to = "0x1111111111111111111111111111111111111111",
            value = BigInteger("1000000000000000000"),
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger("150000"),
            baseGas = BigInteger("110000"),
            gasPrice = BigInteger("60000000000"),
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0xfee0000000000000000000000000000000000001",
            nonce = BigInteger.ZERO,
        )
    }
}