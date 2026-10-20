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
import com.tangem.domain.wallets.signing.DeriveAndSignError
import com.tangem.domain.wallets.signing.SignRequest
import com.tangem.domain.wallets.signing.Signed
import com.tangem.hot.sdk.model.DerivedPublicKeyResponse
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.hot.sdk.model.SignedData
import com.tangem.hot.sdk.model.UnlockHotWallet
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.coroutines.resume

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultDeriveAndSignSessionTest {

    private val derivationsRepository: DerivationsRepository = mockk(relaxUnitFun = true)
    private val cardSdkConfigRepository: CardSdkConfigRepository = mockk()
    private val tangemSdkManager: TangemSdkManager = mockk()
    private val hotWalletAccessor: HotWalletAccessor = mockk(relaxUnitFun = true)
    private val inSessionKeyDeriver: InSessionKeyDeriver = mockk()
    private val signer: TransactionSigner = mockk()
    private val session: CardSession = mockk(relaxed = true)
    private var sessionResult: CompletionResult<Signed<String>>? = null

    private val sut = DefaultDeriveAndSignSession(
        derivationsRepository = derivationsRepository,
        cardSdkConfigRepository = cardSdkConfigRepository,
        tangemSdkManager = tangemSdkManager,
        hotWalletAccessor = hotWalletAccessor,
        inSessionKeyDeriver = inSessionKeyDeriver,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            derivationsRepository,
            cardSdkConfigRepository,
            tangemSdkManager,
            hotWalletAccessor,
            inSessionKeyDeriver,
            signer,
        )
        sessionResult = null
        coEvery { inSessionKeyDeriver.derive(session, SEED_KEY, PATH) } returns CompletionResult.Success(DERIVED_KEY)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Cold {

        @Test
        fun `GIVEN a stored key WHEN run THEN prepares before the card session and signs once`() = runTest {
            // Arrange
            val wallet = coldWallet()
            val events = mutableListOf<String>()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), null) } returns signer
            coEvery { signer.sign(listOf(HASH), any<Wallet.PublicKey>()) } coAnswers {
                events += "sign"
                CompletionResult.Success(listOf(SIG))
            }

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { key ->
                events += "prepare"
                SignRequest(value = key.publicKey.size.toString(), hashes = listOf(HASH))
            }

            // Assert
            assertThat(actual.getOrNull()?.signatures).containsExactly(SIG)
            assertThat(events).containsExactly("prepare", "sign").inOrder()
            coVerify(exactly = 0) {
                tangemSdkManager.runTaskAsync<Any>(any(), any(), any(), any(), any(), any(), any())
            }
        }

        @Test
        fun `GIVEN a stored key and nothing to sign WHEN run THEN no card is touched`() = runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = emptyList()) }

            // Assert
            assertThat(actual.getOrNull()?.value).isEqualTo(VALUE)
            assertThat(actual.getOrNull()?.signatures).isEmpty()
            verify(exactly = 0) { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), any()) }
        }

        @Test
        fun `GIVEN no stored key WHEN run THEN derives, stores, prepares and signs in one session`() = runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            coEvery { signer.sign(listOf(HASH), any<Wallet.PublicKey>()) } returns CompletionResult.Success(listOf(SIG))
            stubCardSession()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

            // Assert
            assertThat(actual.getOrNull()?.value).isEqualTo(VALUE)
            assertThat(actual.getOrNull()?.signatures).containsExactly(SIG)
            assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
            coVerify(exactly = 1) {
                derivationsRepository.storeDerivedKeys(
                    wallet.walletId,
                    mapOf(SEED_KEY_BAK to ExtendedPublicKeysMap(mapOf(PATH to DERIVED_KEY))),
                )
            }
            coVerify(exactly = 1) { signer.sign(any<List<ByteArray>>(), any()) }
            coVerify(exactly = 1) {
                tangemSdkManager.runTaskAsync<Any>(any(), any(), any(), any(), any(), any(), registerColdWallet = true)
            }
        }

        @Test
        fun `GIVEN no stored key and nothing to sign WHEN run THEN the session closes without a signature`() =
            runTest {
                // Arrange
                val wallet = coldWallet()
                coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
                stubCardSession()

                // Act
                val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = emptyList()) }

                // Assert
                assertThat(actual.getOrNull()?.signatures).isEmpty()
                assertThat(sessionResult).isInstanceOf(CompletionResult.Success::class.java)
                verify(exactly = 0) { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN the card rejects the signature WHEN run THEN returns CardError and the session fails`() = runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), session) } returns signer
            coEvery { signer.sign(any<List<ByteArray>>(), any()) } returns
                CompletionResult.Failure(TangemSdkError.WrongAccessCode())
            stubCardSession()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.CardError)
            assertThat(sessionResult).isInstanceOf(CompletionResult.Failure::class.java)
        }

        @Test
        fun `GIVEN the derivation fails WHEN run THEN returns the error and never prepares`() = runTest {
            // Arrange
            val wallet = coldWallet()
            var isPrepared = false
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            coEvery { inSessionKeyDeriver.derive(session, SEED_KEY, PATH) } returns
                CompletionResult.Failure(TangemSdkError.UserCancelled())
            stubCardSession()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) {
                isPrepared = true
                SignRequest(value = VALUE, hashes = listOf(HASH))
            }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.UserCancelled)
            assertThat(isPrepared).isFalse()
            coVerify(exactly = 0) { derivationsRepository.storeDerivedKeys(any(), any()) }
        }

        @Test
        fun `GIVEN prepare throws WHEN run THEN returns Unknown and the session fails`() = runTest {
            // Arrange
            val wallet = coldWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            stubCardSession()

            // Act
            val actual = sut.run<String>(wallet, SEED_KEY, PATH) { error("boom") }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.Unknown)
            assertThat(sessionResult).isInstanceOf(CompletionResult.Failure::class.java)
        }

        @Test
        fun `GIVEN the user closes the sheet during prepare WHEN run THEN returns UserCancelled and nothing signs`() =
            runTest {
                // Arrange
                val wallet = coldWallet()
                val isPreparing = CompletableDeferred<Unit>()
                coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
                coEvery {
                    tangemSdkManager.runTaskAsync(
                        runnable = any<CardSessionRunnable<Signed<String>>>(),
                        preflightReadFilter = any(),
                        cardId = any(),
                        initialMessage = any(),
                        accessCode = any(),
                        iconScanRes = any(),
                        registerColdWallet = any(),
                    )
                } coAnswers {
                    firstArg<CardSessionRunnable<Signed<String>>>().run(session) { sessionResult = it }
                    isPreparing.await()
                    CompletionResult.Failure(TangemSdkError.UserCancelled())
                }

                // Act
                val actual = sut.run<String>(wallet, SEED_KEY, PATH) {
                    isPreparing.complete(Unit)
                    awaitCancellation()
                }

                // Assert
                assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.UserCancelled)
                assertThat(sessionResult).isNull()
                verify(exactly = 0) { cardSdkConfigRepository.getCommonSigner(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN the caller is cancelled during prepare WHEN run THEN the sheet closes as cancelled`() = runTest {
            // Arrange
            val wallet = coldWallet()
            val isPreparing = CompletableDeferred<Unit>()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            stubCardSession()

            // Act
            val job = launch {
                sut.run<String>(wallet, SEED_KEY, PATH) {
                    isPreparing.complete(Unit)
                    awaitCancellation()
                }
            }
            isPreparing.await()
            job.cancelAndJoin()

            // Assert
            assertThat((sessionResult as CompletionResult.Failure).error)
                .isInstanceOf(TangemSdkError.UserCancelled::class.java)
        }

        @Test
        fun `GIVEN starting the card session throws WHEN run THEN returns Unknown and never prepares`() = runTest {
            // Arrange
            val wallet = coldWallet()
            var isPrepared = false
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            coEvery {
                tangemSdkManager.runTaskAsync(
                    runnable = any<CardSessionRunnable<Signed<String>>>(),
                    preflightReadFilter = any(),
                    cardId = any(),
                    initialMessage = any(),
                    accessCode = any(),
                    iconScanRes = any(),
                    registerColdWallet = any(),
                )
            } throws IllegalStateException("no activity")

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) {
                isPrepared = true
                SignRequest(value = VALUE, hashes = listOf(HASH))
            }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.Unknown)
            assertThat(isPrepared).isFalse()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Hot {

        @Test
        fun `GIVEN no stored key WHEN run THEN unlocks once and derives and signs with that unlock`() = runTest {
            // Arrange
            val wallet = hotWallet()
            val unlock: UnlockHotWallet = mockk()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returnsMany listOf(null, unlock, unlock)
            coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } returns unlock
            coEvery { hotWalletAccessor.derivePublicKeysWithUnlock(unlock, any()) } returns derivedResponse()
            coEvery { hotWalletAccessor.signHashesWithUnlock(unlock, any()) } returns signedData()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

            // Assert
            assertThat(actual.getOrNull()?.signatures).containsExactly(SIG)
            coVerify(exactly = 1) { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) }
            coVerify(exactly = 0) { hotWalletAccessor.signHashesReusingContextualUnlock(any(), any()) }
            coVerify(exactly = 0) { hotWalletAccessor.signHashes(any(), any()) }
            coVerify(exactly = 1) {
                derivationsRepository.storeDerivedKeys(
                    wallet.walletId,
                    mapOf(SEED_KEY_BAK to ExtendedPublicKeysMap(mapOf(PATH to DERIVED_KEY))),
                )
            }
            verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
        }

        @Test
        fun `GIVEN a stored key and nothing to sign WHEN run THEN nothing is unlocked`() = runTest {
            // Arrange
            val wallet = hotWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = emptyList()) }

            // Assert
            assertThat(actual.getOrNull()?.signatures).isEmpty()
            coVerify(exactly = 0) { hotWalletAccessor.unlockContextual(any<HotWalletId>()) }
            verify(exactly = 0) { hotWalletAccessor.clearContextualUnlock(any<HotWalletId>()) }
        }

        @Test
        fun `GIVEN an unlock established elsewhere WHEN run THEN asks once and leaves it in place`() = runTest {
            // Arrange
            val wallet = hotWallet()
            val unlock: UnlockHotWallet = mockk()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns storedDerivedKey()
            every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returns unlock
            coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } returns unlock
            coEvery { hotWalletAccessor.signHashesWithUnlock(unlock, any()) } returns signedData()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

            // Assert
            assertThat(actual.getOrNull()?.signatures).containsExactly(SIG)
            coVerify(exactly = 1) { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) }
            verify(exactly = 0) { hotWalletAccessor.clearContextualUnlock(any<HotWalletId>()) }
        }

        @Test
        fun `GIVEN another flow clears the unlock during prepare WHEN run THEN asks again before signing`() =
            runTest {
                // Arrange
                val wallet = hotWallet()
                val first: UnlockHotWallet = mockk()
                val second: UnlockHotWallet = mockk()
                coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
                every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returnsMany
                    listOf(null, null, second)
                coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } returnsMany listOf(first, second)
                coEvery { hotWalletAccessor.derivePublicKeysWithUnlock(first, any()) } returns derivedResponse()
                coEvery { hotWalletAccessor.signHashesWithUnlock(second, any()) } returns signedData()

                // Act
                val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

                // Assert
                assertThat(actual.getOrNull()?.signatures).containsExactly(SIG)
                coVerify(exactly = 2) { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) }
                verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
            }

        @Test
        fun `GIVEN the prompt is cancelled WHEN run THEN returns UserCancelled and nothing is signed`() = runTest {
            // Arrange
            val wallet = hotWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returns null
            coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } throws TangemSdkError.UserCancelled()

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.UserCancelled)
            coVerify(exactly = 0) { hotWalletAccessor.signHashesWithUnlock(any(), any()) }
            verify(exactly = 0) { hotWalletAccessor.clearContextualUnlock(any<HotWalletId>()) }
        }

        @Test
        fun `GIVEN the unlock fails after it was stored WHEN run THEN the stored unlock is cleared`() = runTest {
            // Arrange
            val wallet = hotWallet()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returnsMany listOf(null, mockk())
            coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } throws IllegalStateException("save failed")

            // Act
            val actual = sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.Unknown)
            verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
        }

        @Test
        fun `GIVEN another flow replaced the run's unlock WHEN run ends THEN the replacement is left in place`() =
            runTest {
                // Arrange
                val wallet = hotWallet()
                val unlock: UnlockHotWallet = mockk()
                val replacement: UnlockHotWallet = mockk()
                coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
                every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returnsMany
                    listOf(null, replacement, replacement)
                coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } returns unlock
                coEvery { hotWalletAccessor.derivePublicKeysWithUnlock(unlock, any()) } returns derivedResponse()
                coEvery { hotWalletAccessor.signHashesWithUnlock(unlock, any()) } returns signedData()

                // Act
                sut.run(wallet, SEED_KEY, PATH) { SignRequest(value = VALUE, hashes = listOf(HASH)) }

                // Assert
                verify(exactly = 0) { hotWalletAccessor.clearContextualUnlock(any<HotWalletId>()) }
            }

        @Test
        fun `GIVEN prepare throws WHEN run THEN returns Unknown and the run's unlock is cleared`() = runTest {
            // Arrange
            val wallet = hotWallet()
            val unlock: UnlockHotWallet = mockk()
            coEvery { derivationsRepository.getExistingDerivedKeys(wallet, SEED_KEY_BAK) } returns noStoredKeys()
            every { hotWalletAccessor.getContextualUnlock(HOT_WALLET_ID) } returnsMany listOf(null, unlock)
            coEvery { hotWalletAccessor.unlockContextual(HOT_WALLET_ID) } returns unlock
            coEvery { hotWalletAccessor.derivePublicKeysWithUnlock(unlock, any()) } returns derivedResponse()

            // Act
            val actual = sut.run<String>(wallet, SEED_KEY, PATH) { error("boom") }

            // Assert
            assertThat(actual.leftOrNull()).isEqualTo(DeriveAndSignError.Unknown)
            verify(exactly = 1) { hotWalletAccessor.clearContextualUnlock(HOT_WALLET_ID) }
        }
    }

    private fun stubCardSession() {
        coEvery {
            tangemSdkManager.runTaskAsync(
                runnable = any<CardSessionRunnable<Signed<String>>>(),
                preflightReadFilter = any(),
                cardId = any(),
                initialMessage = any(),
                accessCode = any(),
                iconScanRes = any(),
                registerColdWallet = any(),
            )
        } coAnswers {
            val runnable = firstArg<CardSessionRunnable<Signed<String>>>()
            suspendCancellableCoroutine { continuation ->
                runnable.run(session) { result ->
                    sessionResult = result
                    if (continuation.isActive) continuation.resume(result)
                }
            }
        }
    }

    private companion object {
        const val VALUE = "prepared"
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