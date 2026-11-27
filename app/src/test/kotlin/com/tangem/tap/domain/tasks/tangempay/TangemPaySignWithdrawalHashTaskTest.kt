package com.tangem.tap.domain.tasks.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.common.CompletionResult
import com.tangem.common.card.Card
import com.tangem.common.card.CardWallet
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CompletionCallback
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.crypto.CryptoUtils
import com.tangem.crypto.CryptoUtils.generatePublicKey
import com.tangem.crypto.Secp256k1
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.common.tangempay.TangemPayUtilities
import com.tangem.domain.tangempay.error.TangemPayActivationError
import com.tangem.operations.derivation.DeriveWalletPublicKeyTask
import com.tangem.operations.sign.SignHashCommand
import com.tangem.operations.sign.SignHashResponse
import io.mockk.EqMatcher
import io.mockk.MockKMatcherScope
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class TangemPaySignWithdrawalHashTaskTest {

    init {
        CryptoUtils.initCrypto()
    }

    private val hash = WITHDRAWAL_HASH.hexToBytes()
    private val walletPublicKey = compressedPublicKey(privateKey = ByteArray(KEY_SIZE) { 1 })
    private val derivedPrivateKey = ByteArray(KEY_SIZE) { 2 }
    private val derivedExtendedPublicKey = ExtendedPublicKey(
        publicKey = compressedPublicKey(privateKey = derivedPrivateKey),
        chainCode = ByteArray(KEY_SIZE) { 3 },
    )

    private val session: CardSession = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(session)
        mockkConstructor(DeriveWalletPublicKeyTask::class, SignHashCommand::class)
        every { session.environment.card } returns cardWith(wallet(EllipticCurve.Secp256k1, walletPublicKey))
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(DeriveWalletPublicKeyTask::class, SignHashCommand::class)
    }

    @Test
    fun `GIVEN card signs the hash WHEN run THEN returns the signature in RSV form for the derived key`() {
        // Arrange
        val cardSignature = Secp256k1.ecdsaSignDigest(hash, derivedPrivateKey)
        stubDerivation(CompletionResult.Success(derivedExtendedPublicKey))
        stubSigning(CompletionResult.Success(signHashResponse(cardSignature)))

        // Act
        val actual = TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        val expected = TangemPayUtilities.unmarshallSignature(
            signature = cardSignature,
            hash = hash,
            extendedPublicKey = derivedExtendedPublicKey,
        )
        assertThat((actual as CompletionResult.Success).data).isEqualTo(expected)
    }

    @Test
    fun `GIVEN derived key WHEN run THEN card signs exactly the given hash at the Tangem Pay path`() {
        // Arrange
        stubDerivation(CompletionResult.Success(derivedExtendedPublicKey))
        stubSigning(CompletionResult.Success(signHashResponse(Secp256k1.ecdsaSignDigest(hash, derivedPrivateKey))))

        // Act
        TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        verify(exactly = 1) { tangemPaySignCommand().run(session, any()) }
    }

    @Test
    fun `GIVEN session without card WHEN run THEN fails with MissingPreflightRead`() {
        // Arrange
        every { session.environment.card } returns null

        // Act
        val actual = TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        assertThat((actual as CompletionResult.Failure).error)
            .isInstanceOf(TangemSdkError.MissingPreflightRead::class.java)
        verify(exactly = 0) { anyConstructed<SignHashCommand>().run(any(), any()) }
    }

    @Test
    fun `GIVEN card without secp256k1 wallet WHEN run THEN fails with MissingWallet and never signs`() {
        // Arrange
        every { session.environment.card } returns cardWith(wallet(EllipticCurve.Ed25519, walletPublicKey))

        // Act
        val actual = TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        assertThat((actual as CompletionResult.Failure).error.code)
            .isEqualTo(TangemPayActivationError.MissingWallet.errorCode)
        verify(exactly = 0) { anyConstructed<SignHashCommand>().run(any(), any()) }
    }

    @Test
    fun `GIVEN secp256k1 wallet without public key WHEN run THEN fails with PublicKeyIsEmpty and never signs`() {
        // Arrange
        every { session.environment.card } returns cardWith(wallet(EllipticCurve.Secp256k1, publicKey = null))

        // Act
        val actual = TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        assertThat((actual as CompletionResult.Failure).error.code)
            .isEqualTo(TangemPayActivationError.PublicKeyIsEmpty.errorCode)
        verify(exactly = 0) { anyConstructed<SignHashCommand>().run(any(), any()) }
    }

    @Test
    fun `GIVEN derivation fails WHEN run THEN forwards the derivation error and never signs`() {
        // Arrange
        val error = TangemSdkError.WalletNotFound()
        stubDerivation(CompletionResult.Failure(error))

        // Act
        val actual = TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        assertThat((actual as CompletionResult.Failure).error).isSameInstanceAs(error)
        verify(exactly = 0) { anyConstructed<SignHashCommand>().run(any(), any()) }
    }

    @Test
    fun `GIVEN signing fails WHEN run THEN forwards the signing error`() {
        // Arrange
        val error = TangemSdkError.UserCancelled()
        stubDerivation(CompletionResult.Success(derivedExtendedPublicKey))
        stubSigning(CompletionResult.Failure(error))

        // Act
        val actual = TangemPaySignWithdrawalHashTask(hash = hash).runAndCapture()

        // Assert
        assertThat((actual as CompletionResult.Failure).error).isSameInstanceAs(error)
    }

    private fun stubDerivation(result: CompletionResult<ExtendedPublicKey>) {
        every { tangemPayDerivationTask().run(session, any()) } answers {
            secondArg<CompletionCallback<ExtendedPublicKey>>().invoke(result)
        }
    }

    private fun stubSigning(result: CompletionResult<SignHashResponse>) {
        every { tangemPaySignCommand().run(session, any()) } answers {
            secondArg<CompletionCallback<SignHashResponse>>().invoke(result)
        }
    }

    private fun MockKMatcherScope.tangemPayDerivationTask(): DeriveWalletPublicKeyTask = constructedWith(
        EqMatcher(walletPublicKey),
        EqMatcher(TangemPayUtilities.customDerivationPath),
    )

    private fun MockKMatcherScope.tangemPaySignCommand(): SignHashCommand = constructedWith(
        EqMatcher(hash),
        EqMatcher(walletPublicKey),
        EqMatcher(TangemPayUtilities.customDerivationPath),
    )

    private fun TangemPaySignWithdrawalHashTask.runAndCapture(): CompletionResult<String> {
        var result: CompletionResult<String>? = null
        run(session) { result = it }
        return requireNotNull(result)
    }

    private fun signHashResponse(signature: ByteArray) = SignHashResponse(
        cardId = CARD_ID,
        walletPublicKey = walletPublicKey,
        signature = signature,
        totalSignedHashes = null,
    )

    private fun compressedPublicKey(privateKey: ByteArray): ByteArray =
        generatePublicKey(privateKey = privateKey, curve = EllipticCurve.Secp256k1, compressed = true)

    private fun cardWith(vararg wallets: CardWallet): Card = mockk {
        every { this@mockk.wallets } returns wallets.toList()
    }

    private fun wallet(curve: EllipticCurve, publicKey: ByteArray?): CardWallet = mockk {
        every { this@mockk.curve } returns curve
        every { this@mockk.publicKey } returns publicKey
    }

    private companion object {
        const val KEY_SIZE = 32
        const val CARD_ID = "AC01000000041100"
        const val WITHDRAWAL_HASH = "0x1c8aff950685c2ed4bc3174f3472287b56d9517b9c948127319a09a7a36deac8"
    }
}