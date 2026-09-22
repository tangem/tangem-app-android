package com.tangem.data.jointaccount.signing

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Wallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.jointaccount.derivation.jointAccountOwnerDerivationPath
import com.tangem.domain.jointaccount.model.JointOperationSigningError
import com.tangem.domain.jointaccount.model.SafeTx
import com.tangem.data.jointaccount.safe.SafeTxTypedDataBuilder
import com.tangem.domain.jointaccount.signing.JointOperationSigner
import com.tangem.domain.models.account.OwnerKeyIndex
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.transaction.error.SignHashesError
import com.tangem.domain.transaction.usecase.SignUseCase
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.crypto.ECKeyPair
import org.web3j.crypto.Sign
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultJointOperationSignerTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val derivationsRepository: DerivationsRepository = mockk()
    private val signUseCase: SignUseCase = mockk()

    private val signer = DefaultJointOperationSigner(
        ownerKeyReader = JointOwnerKeyReader(
            userWalletsListRepository = userWalletsListRepository,
            derivationsRepository = derivationsRepository,
        ),
        signUseCase = signUseCase,
        formatter = JointSignatureFormatter(),
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val userWalletId = UserWalletId("011")
    private val ownerKeyIndex = OwnerKeyIndex(value = 2).getOrNull()!!
    private val ownerPath = jointAccountOwnerDerivationPath(index = 2)
    private val seedKey = "02AABB".hexToBytes()

    private val ownerKey = ExtendedPublicKey(
        publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
        chainCode = ByteArray(size = 32),
    )

    @BeforeEach
    fun setUp() {
        clearMocks(userWalletsListRepository, derivationsRepository, signUseCase)
        mockkStatic(UserWalletsListRepository::getSyncStrict)
        every { userWalletsListRepository.getSyncStrict(userWalletId) } returns coldWallet()
        stubDerivedKey(present = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UserWalletsListRepository::getSyncStrict)
    }

    @Test
    fun `GIVEN a derived owner key WHEN signOperation THEN the local digest is signed into RSV form`() = runTest {
        // Arrange
        val hashes = slot<List<ByteArray>>()
        coEvery { signUseCase(hashes = capture(hashes), publicKey = any<Wallet.PublicKey>(), userWallet = any()) }
            .answers { hashes.captured.map(::sign).right() }

        // Act
        val actual = signer.signOperation(
            userWalletId = userWalletId,
            ownerKeyIndex = ownerKeyIndex,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
        )

        // Assert
        val signature = actual.getOrNull()!!
        assertThat(signature).matches("0x[0-9a-f]{130}")
        val v = signature.substring(startIndex = 130).toInt(radix = 16)
        assertThat(v).isAnyOf(27, 28)
        val expectedDigest = SafeTxTypedDataBuilder.digest(SAFE_TX, chainId = CHAIN_ID, verifyingContract = SAFE_ADDRESS)
        assertThat(hashes.captured.single()).isEqualTo(expectedDigest)
    }

    @Test
    fun `GIVEN the signing key is derived WHEN signOperation THEN the owner path rides in the public key`() = runTest {
        // Arrange
        val publicKey = slot<Wallet.PublicKey>()
        coEvery { signUseCase(hashes = any<List<ByteArray>>(), publicKey = capture(publicKey), userWallet = any()) }
            .answers { listOf(sign(firstArg<List<ByteArray>>().single())).right() }

        // Act
        signer.signOperation(
            userWalletId = userWalletId,
            ownerKeyIndex = ownerKeyIndex,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
        )

        // Assert
        val derivation = publicKey.captured.derivationType as Wallet.PublicKey.DerivationType.Plain
        assertThat(derivation.hdKey.path).isEqualTo(ownerPath)
        assertThat(publicKey.captured.seedKey).isEqualTo(seedKey)
    }

    @Test
    fun `GIVEN no derived owner key WHEN signOperation THEN nothing is signed`() = runTest {
        // Arrange
        stubDerivedKey(present = false)

        // Act
        val actual = signer.signOperation(
            userWalletId = userWalletId,
            ownerKeyIndex = ownerKeyIndex,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
        )

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(JointOperationSigningError.OwnerKeyNotDerived)
        coVerify(exactly = 0) { signUseCase(hashes = any<List<ByteArray>>(), publicKey = any<Wallet.PublicKey>(), userWallet = any()) }
    }

    @Test
    fun `GIVEN the session is dismissed WHEN signOperation THEN the error is a cancellation`() = runTest {
        // Arrange
        coEvery { signUseCase(hashes = any<List<ByteArray>>(), publicKey = any<Wallet.PublicKey>(), userWallet = any()) }
            .returns(SignHashesError.Cancelled.left())

        // Act
        val actual = signer.signOperation(
            userWalletId = userWalletId,
            ownerKeyIndex = ownerKeyIndex,
            safeTx = SAFE_TX,
            chainId = CHAIN_ID,
            safeAddress = SAFE_ADDRESS,
        )

        // Assert
        assertThat(actual.leftOrNull()).isEqualTo(JointOperationSigningError.UserCancelled)
    }

    @Test
    fun `GIVEN the signer interface WHEN inspected THEN no entry point accepts a ready-made digest`() {
        // Act
        val byteArrayParameters = JointOperationSigner::class.java.methods
            .filterNot { it.name == "equals" }
            .flatMap { method -> method.parameterTypes.toList() }
            .filter { it == ByteArray::class.java }

        // Assert
        assertThat(byteArrayParameters).isEmpty()
    }

    private fun coldWallet(): UserWallet.Cold {
        val cardWallet = mockk<com.tangem.domain.models.scan.CardDTO.Wallet> {
            every { curve } returns EllipticCurve.Secp256k1
            every { publicKey } returns seedKey
        }
        return mockk {
            every { walletId } returns userWalletId
            every { scanResponse } returns mockk(relaxed = true) {
                every { card } returns mockk(relaxed = true) {
                    every { wallets } returns listOf(cardWallet)
                }
            }
        }
    }

    private fun stubDerivedKey(present: Boolean) {
        coEvery { derivationsRepository.getExistingDerivedKeys(userWalletId, ByteArrayKey(seedKey)) } returns
            if (present) ExtendedPublicKeysMap(mapOf(ownerPath to ownerKey)) else ExtendedPublicKeysMap(emptyMap())
    }

    private fun sign(hash: ByteArray): ByteArray {
        val signature = Sign.signMessage(hash, ECKeyPair.create(OWNER_PRIVATE_KEY), false)
        return signature.r + signature.s
    }

    private companion object {
        const val CHAIN_ID = 137L
        const val SAFE_ADDRESS = "0x5afe000000000000000000000000000000000001"

        val OWNER_PRIVATE_KEY: BigInteger = BigInteger.ONE

        val SAFE_TX = SafeTx(
            to = "0x1111111111111111111111111111111111111111",
            value = BigInteger("1000000000000000000"),
            data = "0x",
            operation = SafeTx.OPERATION_CALL,
            safeTxGas = BigInteger("75000"),
            baseGas = BigInteger("110000"),
            gasPrice = BigInteger("30000000000"),
            gasToken = "0x0000000000000000000000000000000000000000",
            refundReceiver = "0x2222222222222222222222222222222222222222",
            nonce = BigInteger.ZERO,
        )
    }
}