package com.tangem.domain.jointaccount.safe

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.toHexString
import com.tangem.domain.jointaccount.model.SafeTx
import org.junit.jupiter.api.Test
import org.web3j.crypto.StructuredDataEncoder
import java.math.BigInteger

/**
 * The type strings feed `keccak256`, so a single changed character silently changes every digest: the
 * contract computes its own and no operation ever executes. Both hashes are pinned against the
 * precomputed constants of Safe v1.5.0 (Safe.sol), and the hand-encoded digest is cross-checked against
 * an independent EIP-712 implementation (web3j) over the same JSON the Blockaid gate inspects.
 */
internal class SafeTxTypedDataBuilderTest {

    @Test
    fun `GIVEN the SafeTx type string WHEN hashed THEN it matches the contract SAFE_TX_TYPEHASH`() {
        // Act
        val actual = SafeTxTypedDataBuilder.safeTxTypeHash().toHexString().lowercase()

        // Assert
        assertThat(actual).isEqualTo("bb8310d486368db6bd6f849402fdd73ad53d316b5a4b2644ad6efe0f941286d8")
    }

    @Test
    fun `GIVEN the domain type string WHEN hashed THEN it matches the contract DOMAIN_SEPARATOR_TYPEHASH`() {
        // Act
        val actual = SafeTxTypedDataBuilder.domainTypeHash().toHexString().lowercase()

        // Assert
        assertThat(actual).isEqualTo("47e79534a245952e8b16893a336b85a3d9ea9fa8c573f3d803afb92a79469218")
    }

    @Test
    fun `GIVEN a native transfer WHEN hashed THEN the digest matches the independent web3j encoding`() {
        // Arrange
        val safeTx = nativeTransfer()

        // Act
        val handEncoded = SafeTxTypedDataBuilder.digest(
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
        assertThat(handEncoded.toHexString()).isEqualTo(StructuredDataEncoder(json).hashStructuredData().toHexString())
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
        val handEncoded = SafeTxTypedDataBuilder.digest(
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
        assertThat(handEncoded.toHexString()).isEqualTo(StructuredDataEncoder(json).hashStructuredData().toHexString())
    }

    @Test
    fun `GIVEN two chain ids WHEN hashed THEN the domain separator differs`() {
        // Act
        val mainnet = SafeTxTypedDataBuilder.domainSeparator(chainId = 1, verifyingContract = SAFE_ADDRESS)
        val polygon = SafeTxTypedDataBuilder.domainSeparator(chainId = 137, verifyingContract = SAFE_ADDRESS)

        // Assert
        assertThat(mainnet.toHexString()).isNotEqualTo(polygon.toHexString())
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
    fun `GIVEN calldata with an odd hex length WHEN hashed THEN it fails instead of dropping a nibble`() {
        // Arrange
        val oddCalldata = nativeTransfer().copy(data = "0xabc")

        // Act
        val actual = runCatching {
            SafeTxTypedDataBuilder.digest(oddCalldata, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN malformed addresses or calldata WHEN building the JSON THEN it fails like the digest does`() {
        // Arrange
        val oddCalldata = nativeTransfer().copy(data = "0xabc")
        val negativeValue = nativeTransfer().copy(value = BigInteger.ONE.negate())
        val shortSafe = SAFE_ADDRESS.dropLast(1)

        // Act
        val calldataFailure = runCatching {
            SafeTxTypedDataBuilder.buildJson(oddCalldata, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        }.exceptionOrNull()
        val valueFailure = runCatching {
            SafeTxTypedDataBuilder.buildJson(negativeValue, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        }.exceptionOrNull()
        val addressFailure = runCatching {
            SafeTxTypedDataBuilder.buildJson(nativeTransfer(), chainId = CHAIN_ID, verifyingContract = shortSafe)
        }.exceptionOrNull()

        // Assert
        assertThat(calldataFailure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(valueFailure).isInstanceOf(IllegalArgumentException::class.java)
        assertThat(addressFailure).isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `GIVEN an operation outside uint8 WHEN hashed THEN it fails instead of encoding a value the contract rejects`() {
        // Arrange
        val overflow = nativeTransfer().copy(operation = 256)

        // Act
        val actual = runCatching {
            SafeTxTypedDataBuilder.digest(overflow, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        }.exceptionOrNull()

        // Assert
        assertThat(actual).isInstanceOf(IllegalArgumentException::class.java)
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