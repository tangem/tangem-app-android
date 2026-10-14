package com.tangem.data.wallets.signing

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.blockchain.common.Wallet
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.CardSession
import com.tangem.common.core.CardSessionRunnable
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.bip32.ExtendedPublicKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.MobileWallet
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.derivations.DerivationsRepository
import com.tangem.domain.wallets.hot.HotWalletAccessor
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.sdk.api.TangemSdkManager
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.coroutines.resume

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultDeriveAndSignSessionTest {

    private val derivationsRepository: DerivationsRepository = mockk(relaxUnitFun = true)
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val hotWalletAccessor: HotWalletAccessor = mockk(relaxUnitFun = true)
    private val signer: TransactionSigner = mockk()
    private val session: CardSession = mockk(relaxed = true)
    private var sessionResult: CompletionResult<Unit>? = null

    private val sut = DefaultDeriveAndSignSession(
        derivationsRepository = derivationsRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        tangemSdkManager = tangemSdkManager,
        hotWalletAccessor = hotWalletAccessor,
        deriveInSession = { _, _ -> CompletionResult.Success(DERIVED_KEY) },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(derivationsRepository, cardSdkConfigRepository, tangemSdkManager, hotWalletAccessor, signer)
        sessionResult = null
    }

    @Test
    fun `GIVEN a stored key WHEN run THEN no session opens before sign`() = runTest {
        // Arrange
        val wallet = coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), null) } returns signer
        coEvery { signer.sign(listOf(HASH), any<Wallet.PublicKey>()) } returns CompletionResult.Success(listOf(SIG))

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { key -> key to sign(listOf(HASH)) }

        // Assert
        assertThat(actual.getOrNull()?.first).isEqualTo(DERIVED_KEY)
        assertThat((actual.getOrNull()?.second as CompletionResult.Success).data).containsExactly(SIG)
        coVerify(exactly = 0) {
            tangemSdkManager.runTaskAsync<Unit>(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `GIVEN no stored key WHEN run on cold THEN derives, stores and signs in one session`() = runTest {
        // Arrange
        val wallet = coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
        coEvery { signer.sign(listOf(HASH), any<Wallet.PublicKey>()) } returns CompletionResult.Success(listOf(SIG))
        stubCardSession()

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { sign(listOf(HASH)) }

        // Assert
        assertThat((actual.getOrNull() as CompletionResult.Success).data).containsExactly(SIG)
        assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
        coVerify(exactly = 1) {
            derivationsRepository.storeDerivedKeys(
                wallet.walletId,
                mapOf(SEED_KEY_BAK to ExtendedPublicKeysMap(mapOf(PATH to DERIVED_KEY))),
            )
        }
        coVerify(exactly = 1) {
            tangemSdkManager.runTaskAsync<Unit>(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `GIVEN the block fails before signing WHEN run on cold THEN the key stays stored and the block result returns`() =
        runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            stubCardSession()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { "network failed" }

            // Assert
            assertThat(actual.getOrNull()).isEqualTo("network failed")
            assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
            coVerify(exactly = 1) { derivationsRepository.storeDerivedKeys(any(), any()) }
        }

    @Test
    fun `GIVEN the card rejects the signature WHEN run on cold THEN the session ends with that error`() = runTest {
        // Arrange
        val wallet = coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
        coEvery { signer.sign(any<List<ByteArray>>(), any()) } returns
            CompletionResult.Failure(TangemSdkError.WrongAccessCode())
        stubCardSession()

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { sign(listOf(HASH)) }

        // Assert
        assertThat(actual.getOrNull()).isInstanceOf(CompletionResult.Failure::class.java)
        assertThat((sessionResult as CompletionResult.Failure).error)
            .isInstanceOf(TangemSdkError.WrongAccessCode::class.java)
    }

    @Test
    fun `GIVEN the user closes the sheet while signing WHEN run on cold THEN returns Left and does not hang`() =
        runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            val isSigning = CompletableDeferred<Unit>()
            coEvery { signer.sign(any<List<ByteArray>>(), any()) } coAnswers {
                isSigning.complete(Unit)
                awaitCancellation()
            }
            coEvery {
                tangemSdkManager.runTaskAsync(
                    runnable = any<CardSessionRunnable<Unit>>(),
                    preflightReadFilter = any(),
                    cardId = any(),
                    initialMessage = any(),
                    accessCode = any(),
                    iconScanRes = any(),
                    registerColdWallet = any(),
                )
            } coAnswers {
                firstArg<CardSessionRunnable<Unit>>().run(session) { }
                isSigning.await()
                CompletionResult.Failure(TangemSdkError.UserCancelled())
            }

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { sign(listOf(HASH)) }

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(TangemSdkError.UserCancelled::class.java)
            coVerify(exactly = 1) { derivationsRepository.storeDerivedKeys(any(), any()) }
        }

    @Test
    fun `GIVEN the caller is cancelled during the session WHEN run on cold THEN the sheet closes as cancelled`() =
        runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            val isInBlock = CompletableDeferred<Unit>()
            stubCardSession()

            // Act
            val job = launch {
                sut.run(wallet, SEED_KEY, PATH) {
                    isInBlock.complete(Unit)
                    awaitCancellation()
                }
            }
            isInBlock.await()
            job.cancelAndJoin()

            // Assert
            assertThat((sessionResult as CompletionResult.Failure).error)
                .isInstanceOf(TangemSdkError.UserCancelled::class.java)
        }

    @Test
    fun `GIVEN a successful signature WHEN the block signs again on cold THEN the second sign fails at once`() =
        runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            coEvery { signer.sign(listOf(HASH), any<Wallet.PublicKey>()) } returns
                CompletionResult.Success(listOf(SIG))
            stubCardSession()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) {
                sign(listOf(HASH))
                sign(listOf(HASH))
            }

            // Assert
            assertThat(actual.getOrNull()).isInstanceOf(CompletionResult.Failure::class.java)
            assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
            coVerify(exactly = 1) { signer.sign(any<List<ByteArray>>(), any()) }
        }

    @Test
    fun `GIVEN the block throws WHEN run on cold THEN returns Left instead of throwing`() = runTest {
        // Arrange
        val wallet = coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
        every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
        stubCardSession()

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { error("boom") }

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(TangemSdkError.ExceptionError::class.java)
        assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
    }

    @Test
    fun `GIVEN a timeout inside the block after signing WHEN run on cold THEN returns Left and does not hang`() =
        runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            coEvery { signer.sign(listOf(HASH), any<Wallet.PublicKey>()) } returns
                CompletionResult.Success(listOf(SIG))
            stubCardSession()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) {
                sign(listOf(HASH))
                withTimeout(timeMillis = 1) { awaitCancellation() }
            }

            // Assert
            assertThat(actual.leftOrNull()).isInstanceOf(TangemSdkError.ExceptionError::class.java)
            assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
        }

    @Test
    fun `GIVEN the derivation fails WHEN run on cold THEN returns Left and never runs the block`() = runTest {
        // Arrange
        val wallet = coldWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
        val failing = DefaultDeriveAndSignSession(
            derivationsRepository = derivationsRepository,
            cardSdkConfigRepository = cardSdkConfigRepository,
            tangemSdkManager = tangemSdkManager,
            hotWalletAccessor = hotWalletAccessor,
            deriveInSession = { _, _ -> CompletionResult.Failure(TangemSdkError.UserCancelled()) },
        )
        stubCardSession()
        var isBlockRun = false

        // Act
        val actual = failing.run(wallet, SEED_KEY, PATH) { isBlockRun = true }

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(TangemSdkError.UserCancelled::class.java)
        assertThat(isBlockRun).isFalse()
        coVerify(exactly = 0) { derivationsRepository.storeDerivedKeys(any(), any()) }
    }

    @Test
    fun `GIVEN the hot derivation prompt is cancelled WHEN run THEN Left and the own unlock is cleared`() = runTest {
        // Arrange
        val wallet = hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
        every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returns null
        coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } throws TangemSdkError.UserCancelled()

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { "unreached" }

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(TangemSdkError.UserCancelled::class.java)
        verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
    }

    @Test
    fun `GIVEN a stored key and no signature WHEN run on hot THEN nothing is unlocked`() = runTest {
        // Arrange
        val wallet = hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()
        every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returns null

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { "nothing to sign" }

        // Assert
        assertThat(actual.getOrNull()).isEqualTo("nothing to sign")
        coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any<HotWalletId>()) }
    }

    @Test
    fun `GIVEN no stored key WHEN run on hot THEN unlocks once for derive and sign`() = runTest {
        // Arrange
        val wallet = hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
        every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returnsMany
            listOf(null, null, mockk(), mockk())
        coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } returns mockk()
        coEvery { hotWalletAccessor.derivePublicKeysReusingContextualUnlock(HOT_WALLET_ID, any()) } returns
            derivedResponse()
        coEvery { hotWalletAccessor.signHashesReusingContextualUnlock(HOT_WALLET_ID, any()) } returns signedData()

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { sign(listOf(HASH)) }

        // Assert
        assertThat((actual.getOrNull() as CompletionResult.Success).data).containsExactly(SIG)
        coVerify(exactly = 1) { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) }
        coVerify(exactly = 0) { hotWalletAccessor.signHashes(any(), any()) }
        coVerify(exactly = 0) { hotWalletAccessor.derivePublicKeys(any(), any()) }
        coVerify(exactly = 1) {
            derivationsRepository.storeDerivedKeys(
                wallet.walletId,
                mapOf(SEED_KEY_BAK to ExtendedPublicKeysMap(mapOf(PATH to DERIVED_KEY))),
            )
        }
        verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
    }

    @Test
    fun `GIVEN an unlock established elsewhere WHEN run on hot THEN it is left in place`() = runTest {
        // Arrange
        val wallet = hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()
        every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returns mockk()
        coEvery { hotWalletAccessor.signHashesReusingContextualUnlock(HOT_WALLET_ID, any()) } returns signedData()

        // Act
        sut.run(wallet, SEED_KEY, PATH) { sign(listOf(HASH)) }

        // Assert
        coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any<HotWalletId>()) }
        verify(exactly = 0) { hotWalletAccessor.clearContextualUnlock(any<HotWalletId>()) }
    }

    @Test
    fun `GIVEN the block throws WHEN run on hot THEN returns Left and the own unlock is cleared`() = runTest {
        // Arrange
        val wallet = hotWallet()
        coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()
        every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returns null

        // Act
        val actual = sut.run(wallet, SEED_KEY, PATH) { error("boom") }

        // Assert
        assertThat(actual.leftOrNull()).isInstanceOf(TangemSdkError.ExceptionError::class.java)
        verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
    }

    private fun stubCardSession() {
        coEvery {
            tangemSdkManager.runTaskAsync(
                runnable = any<CardSessionRunnable<Unit>>(),
                preflightReadFilter = any(),
                cardId = any(),
                initialMessage = any(),
                accessCode = any(),
                iconScanRes = any(),
                registerColdWallet = any(),
            )
        } coAnswers {
            val runnable = firstArg<CardSessionRunnable<Unit>>()
            suspendCancellableCoroutine { continuation ->
                runnable.run(session) { result ->
                    sessionResult = result
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        }
    }

    private companion object {
        val PATH = DerivationPath("m/44'/60'/999899'/0/0")
        val SEED_KEY = "02AABB".hexToBytes()
        val SEED_KEY_BAK = ByteArrayKey(SEED_KEY)
        val HASH = "01".repeat(32).hexToBytes()
        val SIG = "02".repeat(64).hexToBytes()
        val HOT_WALLET_ID = HotWalletId(value = "hot", authType = HotWalletId.AuthType.Password)
        val DERIVED_KEY = ExtendedPublicKey(
            publicKey = "0279BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798".hexToBytes(),
            chainCode = "00".repeat(32).hexToBytes(),
        )

        fun coldWallet(): UserWallet.Cold = MockUserWalletFactory.create()

        fun hotWallet(): UserWallet.Hot = UserWallet.Hot(
            name = "Hot",
            walletId = UserWalletId("0A"),
            hotWalletId = HOT_WALLET_ID,
            wallets = listOf(
                MobileWallet(
                    publicKey = SEED_KEY,
                    chainCode = null,
                    curve = EllipticCurve.Secp256k1,
                    derivedKeys = emptyMap(),
                ),
            ),
            backedUp = true,
        )

        fun storedDerivedKey() = ExtendedPublicKeysMap(mapOf(PATH to DERIVED_KEY))

        fun noStoredKeys() = ExtendedPublicKeysMap(emptyMap())

        fun derivedResponse() = DerivedPublicKeyResponse(
            responses = listOf(
                DerivedPublicKeyResponse.ResponseEntry(
                    curve = EllipticCurve.Secp256k1,
                    seedKey = ExtendedPublicKey(publicKey = SEED_KEY, chainCode = ByteArray(32)),
                    publicKeys = mapOf(PATH to DERIVED_KEY),
                ),
            ),
        )

        fun signedData() = listOf(
            SignedData(curve = EllipticCurve.Secp256k1, derivationPath = PATH, signatures = listOf(SIG)),
        )
    }
}