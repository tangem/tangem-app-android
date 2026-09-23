package com.tangem.data.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import com.tangem.domain.jointaccount.model.SafeTx
import org.junit.jupiter.api.Test
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

/**
 * The digest is produced by the blockchain SDK out of the JSON built here, so what this suite has to prove is that
 * the JSON describes the Safe v1.5.0 schema correctly: it is cross-checked against an independent EIP-712
 * implementation (web3j) over the same payload the Blockaid gate inspects. A single changed character in a type
 * string would silently change every digest — the contract would compute its own and no operation would execute.
 */
internal class SafeTxTypedDataBuilderTest {

    @Test
    fun `GIVEN a native transfer WHEN hashed THEN the digest matches the independent web3j encoding`() {
        // Arrange
        val safeTx = nativeTransfer()

        // Act
        val actual = SafeTxTypedDataBuilder.digest(
            safeTx = safeTx,
            chainId = CHAIN_ID,
            verifyingContract = SAFE_ADDRESS,
        )
        val json = SafeTxTypedDataBuilder.buildJson(
            safeTx = safeTx,
            chainId = CHAIN_ID,
            verifyingContract = SAFE_ADDRESS,
        )

        // Assert
        assertThat(actual.toHexString()).isEqualTo(StructuredDataEncoder(json).hashStructuredData().toHexString())
    }

    @Test
    fun `GIVEN an ERC-20 transfer WHEN hashed THEN the digest matches the independent web3j encoding`() {
        // Arrange
        val safeTx = nativeTransfer().copy(
            to = TOKEN_ADDRESS,
            value = BigInteger.ZERO,
            data = "0xa9059cbb" + "0".repeat(n = 24) + RECIPIENT.removePrefix(prefix = "0x") + "0".repeat(n = 64),
        )

        // Act
        val actual = SafeTxTypedDataBuilder.digest(
            safeTx = safeTx,
            chainId = CHAIN_ID,
            verifyingContract = SAFE_ADDRESS,
        )
        val json = SafeTxTypedDataBuilder.buildJson(
            safeTx = safeTx,
            chainId = CHAIN_ID,
            verifyingContract = SAFE_ADDRESS,
        )

        // Assert
        assertThat(actual.toHexString()).isEqualTo(StructuredDataEncoder(json).hashStructuredData().toHexString())
    }

    @Test
    fun `GIVEN two chain ids WHEN hashed THEN the digest differs`() {
        // Act
        val mainnet = SafeTxTypedDataBuilder.digest(nativeTransfer(), chainId = 1, verifyingContract = SAFE_ADDRESS)
        val polygon = SafeTxTypedDataBuilder.digest(nativeTransfer(), chainId = 137, verifyingContract = SAFE_ADDRESS)

        // Assert
        assertThat(mainnet.toHexString()).isNotEqualTo(polygon.toHexString())
    }

    @Test
    fun `GIVEN a chain id outside the domain range WHEN hashed THEN it fails instead of signing for no chain`() {
        // Act
        val actual = listOf(0L, -1L).map { chainId ->
            runCatching {
                SafeTxTypedDataBuilder.digest(nativeTransfer(), chainId = chainId, verifyingContract = SAFE_ADDRESS)
            }.exceptionOrNull()
        }

        // Assert
        assertThat(actual).hasSize(2)
        actual.forEach { assertThat(it).isInstanceOf(IllegalArgumentException::class.java) }
    }

    @Test
    fun `GIVEN a gas field changed WHEN hashed THEN the digest changes`() {
        // Arrange
        val safeTx = nativeTransfer()
        val bumpedGas = safeTx.copy(gasPrice = safeTx.gasPrice + BigInteger.ONE)

        // Act
        val original = SafeTxTypedDataBuilder.digest(safeTx, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        val bumped = SafeTxTypedDataBuilder.digest(bumpedGas, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)

        // Assert
        assertThat(original.toHexString()).isNotEqualTo(bumped.toHexString())
    }

    @Test
    fun `GIVEN a field beyond uint256 WHEN hashed THEN it fails instead of truncating`() {
        // Arrange
        val beyondUint256 = BigInteger.ONE.shiftLeft(256)
        val maxValue = nativeTransfer().copy(value = beyondUint256 - BigInteger.ONE)
        val overflow = nativeTransfer().copy(value = beyondUint256)

        // Act
        val maxFits = runCatching {
            SafeTxTypedDataBuilder.digest(maxValue, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        }
        val actual = runCatching {
            SafeTxTypedDataBuilder.digest(overflow, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        }.exceptionOrNull()

        // Assert
        assertThat(maxFits.isSuccess).isTrue()
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN malformed addresses or calldata WHEN building the JSON THEN it fails instead of encoding`() {
        // Arrange
        val oddCalldata = nativeTransfer().copy(data = "0xabc")
        val negativeValue = nativeTransfer().copy(value = BigInteger.ONE.negate())
        val shortSafe = SAFE_ADDRESS.dropLast(n = 1)
        val overflowOperation = nativeTransfer().copy(operation = 256)

        // Act
        val failures = listOf(
            runCatching { SafeTxTypedDataBuilder.buildJson(oddCalldata, CHAIN_ID, SAFE_ADDRESS) },
            runCatching { SafeTxTypedDataBuilder.buildJson(negativeValue, CHAIN_ID, SAFE_ADDRESS) },
            runCatching { SafeTxTypedDataBuilder.buildJson(nativeTransfer(), CHAIN_ID, shortSafe) },
            runCatching { SafeTxTypedDataBuilder.buildJson(overflowOperation, CHAIN_ID, SAFE_ADDRESS) },
        ).map { it.exceptionOrNull() }

        // Assert
        assertThat(failures).hasSize(4)
        failures.forEach { assertThat(it).isInstanceOf(IllegalArgumentException::class.java) }
    }

    /**
     * Validation strips the prefix before checking, so an address arriving without `0x` is accepted — the JSON
     * has to put it back, or the encoder would hash a value the Safe never would.
     */
    @Test
    fun `GIVEN addresses without the hex prefix WHEN hashed THEN the digest is the prefixed one`() {
        // Arrange
        val unprefixed = nativeTransfer().copy(
            to = RECIPIENT.removePrefix(prefix = "0x"),
            refundReceiver = TREASURY.uppercase().removePrefix(prefix = "0X"),
        )

        // Act
        val actual = SafeTxTypedDataBuilder.digest(
            safeTx = unprefixed,
            chainId = CHAIN_ID,
            verifyingContract = SAFE_ADDRESS.removePrefix(prefix = "0x"),
        )
        val json = SafeTxTypedDataBuilder.buildJson(unprefixed, CHAIN_ID, SAFE_ADDRESS)

        // Assert
        val expected = SafeTxTypedDataBuilder.digest(nativeTransfer(), CHAIN_ID, SAFE_ADDRESS)
        assertThat(actual).isEqualTo(expected)
        assertThat(json).contains(""""to":"$RECIPIENT"""")
        assertThat(json).contains(""""refundReceiver":"$TREASURY"""")
    }

    private fun nativeTransfer(): SafeTx = SafeTx(
        to = RECIPIENT,
        value = BigInteger("1000000000000000000"),
        data = "0x",
        operation = SafeTx.OPERATION_CALL,
        safeTxGas = BigInteger("75000"),
        baseGas = BigInteger("110000"),
        gasPrice = BigInteger("30000000000"),
        gasToken = ZERO_ADDRESS,
        refundReceiver = TREASURY,
        nonce = BigInteger.ZERO,
    )

    private companion object {
        const val CHAIN_ID = 1L
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"
        const val RECIPIENT = "0x1111111111111111111111111111111111111111"
        const val TOKEN_ADDRESS = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
        const val TREASURY = "0x2222222222222222222222222222222222222222"
        const val ZERO_ADDRESS = "0x0000000000000000000000000000000000000000"
    }
}