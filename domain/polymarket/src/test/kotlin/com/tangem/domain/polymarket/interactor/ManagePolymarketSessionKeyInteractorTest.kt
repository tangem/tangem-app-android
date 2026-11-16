package com.tangem.domain.polymarket.interactor

import com.tangem.domain.polymarket.sessionkey.FakePolymarketPendingAuthorizationStore
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketDepositWalletLock
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketSessionKeyRepository
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketAuthError
import com.tangem.domain.polymarket.model.PolymarketClobIdentity
import com.tangem.domain.polymarket.model.PolymarketSessionCredentialsError
import com.tangem.domain.polymarket.model.PolymarketSessionKey
import com.tangem.domain.polymarket.model.PolymarketSessionKeyAuthorization
import com.tangem.domain.polymarket.model.PolymarketSessionKeyBatch
import com.tangem.domain.polymarket.model.PolymarketSessionKeyError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyLifecycleError
import com.tangem.domain.polymarket.model.PolymarketSessionKeyProgress
import com.tangem.domain.polymarket.model.PolymarketSessionKeyRevocation
import com.tangem.domain.polymarket.model.PolymarketSessionKeyStatus
import com.tangem.domain.polymarket.model.PolymarketSessionSigner
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketSignedSessionKeyBatches
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.sessionkey.PolymarketDeviceIdProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketPendingAuthorization
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionCredentialsProvider
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyDeviceState
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyStore
import com.tangem.domain.polymarket.sessionkey.PolymarketSessionKeyTradingStore
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
internal class ManagePolymarketSessionKeyInteractorTest {

    private val signBatches: SignPolymarketSessionKeyBatchesInteractor = mockk()
    private val sessionKeyRepository: PolymarketSessionKeyRepository = mockk()
    private val polymarketRepository: PolymarketRepository = mockk()
    private val sessionKeyStore: PolymarketSessionKeyStore = mockk()
    private val deviceIdProvider: PolymarketDeviceIdProvider = mockk()
    private val sessionCredentialsProvider: PolymarketSessionCredentialsProvider = mockk()
    private val tradingStore: PolymarketSessionKeyTradingStore = mockk(relaxed = true)
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()
    private val deriveApiCredentials: DeriveApiCredentialsUseCase = mockk()
    private val pendingAuthorizations = FakePolymarketPendingAuthorizationStore()
    private val depositWalletLock = PolymarketDepositWalletLock(pendingAuthorizations)

    @BeforeEach
    fun setup() {
        clearMocks(
            signBatches,
            sessionKeyRepository,
            polymarketRepository,
            sessionKeyStore,
            deviceIdProvider,
            sessionCredentialsProvider,
            tradingStore,
            getApiCredentials,
            deriveApiCredentials,
        )
        coEvery { sessionKeyStore.getOrCreate(USER_WALLET_ID) } returns KEY.right()
        coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns KEY
        coEvery { deviceIdProvider.deviceId() } returns DEVICE_ID
        every { deviceIdProvider.deviceName } returns DEVICE_NAME
        coEvery { sessionCredentialsProvider.get(USER_WALLET_ID) } returns
            PolymarketClobIdentity.Session(address = KEY, credentials = SESSION_CREDENTIALS).right()
        coEvery { getApiCredentials(USER_WALLET_ID) } returns OWNER_CREDENTIALS
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
    }

    @Nested
    inner class Enable {

        @Test
        fun `GIVEN the relayer authorizes and Polymarket lists the key WHEN enable THEN the key is marked ready`() =
            runTest {
                // Arrange
                val authorization = authorization()
                givenSigned(nonEmptyListOf(authorize()), signed(authorization))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(
                    listOf(key(status = PolymarketSessionKeyStatus.PENDING)),
                    listOf(key(status = PolymarketSessionKeyStatus.ACTIVE)),
                )
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                    emptyList<PolymarketSessionSigner>().right(),
                    listOf(signer(KEY)).right(),
                )

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList()

                // Assert
                assertThat(actual).containsExactly(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Authorizing(key(status = PolymarketSessionKeyStatus.PENDING)),
                    PolymarketSessionKeyProgress.CheckingReadiness(key()),
                    PolymarketSessionKeyProgress.Finished(key().right()),
                ).inOrder()
                coVerify(exactly = 1) { tradingStore.markReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) }
            }

        @Test
        fun `GIVEN the key listed with every scope and its own expiry WHEN enable THEN it finishes not ready`() =
            runTest {
                // Arrange
                val authorization = authorization()
                givenSigned(nonEmptyListOf(authorize()), signed(authorization))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(listOf(key()))
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(
                    signer(KEY, validUntil = VALID_UNTIL.minusSeconds(60)).copy(scopes = listOf("all")),
                ).right()

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
            }

        @Test
        fun `GIVEN the key listed with CLOB and another expiry WHEN enable THEN it finishes not ready`() =
            runTest {
                // Arrange
                val authorization = authorization()
                givenSigned(nonEmptyListOf(authorize()), signed(authorization))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(listOf(key()))
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                    listOf(signer(KEY, validUntil = VALID_UNTIL.plusSeconds(1))).right()

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
            }

        @Test
        fun `GIVEN the key listed in the recorded second WHEN enable THEN it is marked ready`() =
            runTest {
                // Arrange
                val authorization = authorization()
                givenSigned(nonEmptyListOf(authorize()), signed(authorization))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(listOf(key()))
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                    listOf(signer(KEY, validUntil = VALID_UNTIL.plusMillis(500))).right()

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { tradingStore.markReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) }
            }

        @Test
        fun `GIVEN the first send goes unanswered WHEN enable THEN the same request is sent again`() = runTest {
            // Arrange
            val authorization = authorization()
            givenSigned(nonEmptyListOf(authorize()), signed(authorization))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returnsMany listOf(
                PolymarketSessionKeyError.Network.left(),
                PolymarketSessionKeyError.UpstreamUnavailable.left(),
                key(status = PolymarketSessionKeyStatus.PENDING).right(),
            )
            givenWalletReads(listOf(key()))

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
            coVerify(exactly = 1) { signBatches(any(), any()) }
            coVerify(exactly = 3) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) }
        }

        @Test
        fun `GIVEN the BFF refuses the key WHEN enable THEN nothing is resent`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns
                PolymarketSessionKeyError.SessionKeyLimitReached.left()

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyLimitReached).left(),
                ),
            ).inOrder()
            coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
        }

        @Test
        fun `GIVEN the signing is cancelled WHEN enable THEN nothing is sent`() = runTest {
            // Arrange
            val error = PolymarketSessionKeyError.SigningFailed(PolymarketSigningError.UserCancelled)
            coEvery { signBatches(USER_WALLET_ID, any()) } returns error.left()

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.Failed(error).left()),
            )
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
        }

        @Test
        fun `GIVEN the pending key is dropped while revoking WHEN enable THEN the authorization failed`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKING)), emptyList())

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.AuthorizationFailed(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
        }

        @Test
        fun `GIVEN the relayer fails the authorization WHEN enable THEN the key is not marked ready`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.FAILED)))

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.AuthorizationFailed(
                        key(status = PolymarketSessionKeyStatus.FAILED),
                    ).left(),
                ),
            )
            coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
            coVerify(exactly = 0) { tradingStore.markAuthorized(any(), any()) }
        }

        @Test
        fun `GIVEN the key stays pending for 29 minutes WHEN enable THEN polling waits for the backend`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            val pendingReads = List(size = 29 * 20) { listOf(key(status = PolymarketSessionKeyStatus.PENDING)) }
            givenWalletReads(*(pendingReads + listOf(listOf(key()))).toTypedArray())

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
        }

        @Test
        fun `GIVEN the key never leaves pending WHEN enable THEN it ends unsettled`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.PENDING)))

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(key(status = PolymarketSessionKeyStatus.PENDING))
                        .left(),
                ),
            )
            assertThat(testScheduler.currentTime).isAtLeast(SETTLE_TIMEOUT_MILLIS)
        }

        @Test
        fun `GIVEN Polymarket never lists the active key WHEN enable THEN it succeeds without being marked ready`() =
            runTest {
                // Arrange
                givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                    listOf(signer(FOREIGN)).right()

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList()

                // Assert
                assertThat(actual).containsExactly(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Authorizing(key()),
                    PolymarketSessionKeyProgress.CheckingReadiness(key()),
                    PolymarketSessionKeyProgress.Finished(key().right()),
                ).inOrder()
                coVerify(exactly = LISTING_ATTEMPTS) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
                assertThat(testScheduler.currentTime).isEqualTo(LISTING_WAIT_MILLIS)
                coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
                coVerify(exactly = 1) { tradingStore.markAuthorized(USER_WALLET_ID, KEY) }
            }

        @Test
        fun `GIVEN the active key has no expiry WHEN enable THEN it succeeds without asking Polymarket`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns
                key(validUntil = null).right()

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key(validUntil = null).right()))
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
            coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
        }

        @Test
        fun `GIVEN the session credentials cannot be derived WHEN enable THEN the key is not marked ready`() =
            runTest {
                // Arrange
                val cause = PolymarketSessionCredentialsError.Key(PolymarketSessionKeyError.KeyMissing)
                givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()
                coEvery { sessionCredentialsProvider.get(USER_WALLET_ID) } returns cause.left()

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.CredentialsUnavailable(key(), cause).left(),
                    ),
                )
            }

        @Test
        fun `GIVEN the owner had no credentials WHEN enable THEN the ClobAuth signed with it derives them`() =
            runTest {
                // Arrange
                val clobAuth = PolymarketSignedOnboarding.SignedClobAuth(l1Signature = "0xl1", timestamp = "100")
                givenSigned(nonEmptyListOf(authorize()), signed(authorization(), ownerClobAuth = clobAuth))
                coEvery { deriveApiCredentials(USER_WALLET_ID, OWNER, "0xl1", "100") } returns
                    OWNER_CREDENTIALS.right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { deriveApiCredentials(USER_WALLET_ID, OWNER, "0xl1", "100") }
            }

        @Test
        fun `GIVEN the owner has no credentials WHEN enable THEN readiness cannot be checked`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()
            coEvery { getApiCredentials(USER_WALLET_ID) } returns null

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.OwnerCredentialsUnavailable(key()).left(),
                ),
            )
        }

        @Test
        fun `GIVEN a signing in progress WHEN a deposit wallet batch starts THEN it is busy at once`() =
            runTest {
                // Arrange
                val signing = CompletableDeferred<Unit>()
                coEvery { signBatches(USER_WALLET_ID, any()) } coAnswers {
                    signing.await()
                    signed(authorization()).right()
                }
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()
                var isBatchRun = false
                val interactor = createInteractor()

                // Act
                val enabling = launch { interactor.enable(USER_WALLET_ID).toList() }
                runCurrent()
                val batch = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) { isBatchRun = true }
                signing.complete(Unit)
                advanceUntilIdle()

                // Assert
                assertThat(batch).isEqualTo(PolymarketDepositWalletLock.Busy.left())
                assertThat(isBatchRun).isFalse()
                enabling.join()
            }

        @Test
        fun `GIVEN the collector is slow on authorizing WHEN another deposit wallet operation starts THEN it runs`() =
            runTest {
                // Arrange
                givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()
                val collecting = CompletableDeferred<Unit>()
                var isClaimStarted = false
                val interactor = createInteractor()

                // Act
                val enabling = launch {
                    interactor.enable(USER_WALLET_ID).collect { progress ->
                        if (progress is PolymarketSessionKeyProgress.Authorizing) collecting.await()
                    }
                }
                runCurrent()
                launch { depositWalletLock.withBatchOrBusy(USER_WALLET_ID) { isClaimStarted = true } }
                runCurrent()
                val isStartedWhileCollecting = isClaimStarted
                collecting.complete(Unit)
                advanceUntilIdle()

                // Assert
                assertThat(isStartedWhileCollecting).isTrue()
                enabling.join()
            }
    }

    @Nested
    inner class SingleFlight {

        @Test
        fun `GIVEN an enable in progress WHEN renew on the same wallet THEN it ends in progress without signing`() =
            runTest {
                // Arrange
                val signing = givenSigningSuspended()
                val interactor = createInteractor()
                val enabling = launch { interactor.enable(USER_WALLET_ID).toList() }
                runCurrent()

                // Act
                val actual = interactor.renew(USER_WALLET_ID).toList()

                // Assert
                assertThat(actual).containsExactly(
                    PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
                )
                coVerify(exactly = 1) { signBatches(any(), any()) }
                signing.complete(Unit)
                enabling.join()
            }

        @Test
        fun `GIVEN an operation ended in an error WHEN another starts on the wallet THEN it runs`() = runTest {
            // Arrange
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null
            val interactor = createInteractor()
            interactor.renew(USER_WALLET_ID).toList()

            // Act
            val actual = interactor.renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual.last()).isNotEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
        }

        @Test
        fun `GIVEN an operation threw WHEN another starts on the wallet THEN it runs`() = runTest {
            // Arrange
            coEvery { signBatches(USER_WALLET_ID, any()) } throws IllegalStateException("signer crashed")
            val interactor = createInteractor()
            runCatching { interactor.enable(USER_WALLET_ID).toList() }
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null

            // Act
            val actual = interactor.renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual.last()).isNotEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
        }

        @Test
        fun `GIVEN an operation was cancelled WHEN another starts on the wallet THEN it runs`() = runTest {
            // Arrange
            givenSigningSuspended()
            val interactor = createInteractor()
            val enabling = launch { interactor.enable(USER_WALLET_ID).toList() }
            runCurrent()
            enabling.cancel()
            enabling.join()
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null

            // Act
            val actual = interactor.renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual.last()).isNotEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StaleSignature {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the signature is refused as stale WHEN enable THEN it ends stale without a second signing`(
            model: StaleModel,
        ) = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns model.error.left()

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.StaleSignature(cause = model.error).left(),
                ),
            )
            coVerify(exactly = 1) { signBatches(any(), any()) }
            coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the signature is refused as stale WHEN revoke THEN it ends stale without a second signing`(
            model: StaleModel,
        ) = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(FOREIGN)), signed(revocation(FOREIGN)))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, any()) } returns model.error.left()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, FOREIGN).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.StaleSignature(cause = model.error).left(),
                ),
            )
            coVerify(exactly = 1) { signBatches(any(), any()) }
        }

        private fun provideTestModels() = listOf(
            StaleModel(error = PolymarketSessionKeyError.NonceAlreadyUsed),
            StaleModel(error = PolymarketSessionKeyError.InvalidDeadline),
            StaleModel(error = PolymarketSessionKeyError.InvalidValidUntil),
            StaleModel(error = PolymarketSessionKeyError.RelayerRejected(detail = "nonce reused")),
        )
    }

    data class StaleModel(val error: PolymarketSessionKeyError)

    @Nested
    inner class Renew {

        @Test
        fun `GIVEN the revocation settles WHEN renew THEN the same key is authorized again`() = runTest {
            // Arrange
            val revocation = revocation(nonce = "7")
            val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(revocation, authorization),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(
                listOf(key(status = PolymarketSessionKeyStatus.REVOKED)),
                listOf(key()),
            )

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Revoked(key(status = PolymarketSessionKeyStatus.REVOKED)),
                PolymarketSessionKeyProgress.Authorizing(key(status = PolymarketSessionKeyStatus.PENDING)),
                PolymarketSessionKeyProgress.CheckingReadiness(key()),
                PolymarketSessionKeyProgress.Finished(key().right()),
            ).inOrder()
            coVerify(exactly = 1) { signBatches(any(), any()) }
            coVerify(exactly = 1) { tradingStore.markReadyOnExchange(USER_WALLET_ID, KEY, VALID_UNTIL) }
        }

        @Test
        fun `GIVEN Polymarket does not list the renewed key yet WHEN renew THEN it is authorized not ready`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(
                    listOf(key(status = PolymarketSessionKeyStatus.REVOKED)),
                    listOf(key()),
                )
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                    emptyList<PolymarketSessionSigner>().right()

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 0) { tradingStore.markReadyOnExchange(any(), any(), any()) }
                coVerify(exactly = 1) { tradingStore.markAuthorized(USER_WALLET_ID, KEY) }
            }

        @Test
        fun `GIVEN the revoking key is missing twice in a row WHEN renew THEN it is taken as revoked and authorized`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(emptyList(), emptyList(), listOf(key()))
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                    emptyList<PolymarketSessionSigner>().right(),
                    listOf(signer(KEY)).right(),
                )

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList()

                // Assert
                assertThat(actual).containsExactly(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                    PolymarketSessionKeyProgress.Revoked(key(status = PolymarketSessionKeyStatus.REVOKED)),
                    PolymarketSessionKeyProgress.Authorizing(key(status = PolymarketSessionKeyStatus.PENDING)),
                    PolymarketSessionKeyProgress.CheckingReadiness(key()),
                    PolymarketSessionKeyProgress.Finished(key().right()),
                ).inOrder()
                coVerifyOrder {
                    polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue)
                    polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue)
                    sessionKeyRepository.getSessionSigners(OWNER_IDENTITY)
                    sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization)
                    polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue)
                }
            }

        @Test
        fun `GIVEN Polymarket keeps listing the dropped key WHEN renew THEN the authorization is not sent`() = runTest {
            // Arrange
            val revocation = revocation(nonce = "7")
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(revocation, authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            givenWalletReads(emptyList())

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            ).inOrder()
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
        }

        @Test
        fun `GIVEN the authorization fails after the revocation WHEN renew THEN revoked is reported first`() = runTest {
            // Arrange
            val revocation = revocation(nonce = "7")
            val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(revocation, authorization),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                PolymarketSessionKeyError.SessionKeyLimitReached.left()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)))

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Revoked(key(status = PolymarketSessionKeyStatus.REVOKED)),
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyLimitReached).left(),
                ),
            ).inOrder()
        }

        @Test
        fun `GIVEN the relayer rejects the authorization once WHEN renew THEN the same authorization is sent again`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returnsMany listOf(
                    PolymarketSessionKeyError.RelayerRejected(detail = "wallet has in-flight action").left(),
                    key(status = PolymarketSessionKeyStatus.PENDING).right(),
                )
                givenWalletReads(
                    listOf(key(status = PolymarketSessionKeyStatus.REVOKED)),
                    listOf(key()),
                )

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { signBatches(any(), any()) }
                coVerify(exactly = 2) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) }
            }

        @Test
        fun `GIVEN the relayer keeps rejecting the authorization WHEN renew THEN it ends stale after bounded resends`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
                val rejection = PolymarketSessionKeyError.RelayerRejected(detail = "wallet has in-flight action")
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    rejection.left()
                givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)))

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = rejection).left(),
                    ),
                )
                coVerify(exactly = 1) { signBatches(any(), any()) }
                coVerify(exactly = 6) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) }
            }

        @Test
        fun `GIVEN the revocation is settling WHEN a claim starts THEN it is busy and the authorization keeps its nonce`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(
                    listOf(key(status = PolymarketSessionKeyStatus.REVOKING)),
                    listOf(key(status = PolymarketSessionKeyStatus.REVOKED)),
                    listOf(key()),
                )
                val claimInteractor = SubmitPredictionClaimInteractor(
                    redeemRepository = mockk(),
                    polymarketRepository = mockk(),
                    positionsRepository = mockk(),
                    signer = mockk(),
                    depositWalletLock = depositWalletLock,
                )
                val interactor = createInteractor()

                // Act
                val renewing = launch { interactor.renew(USER_WALLET_ID).toList() }
                runCurrent()
                val claim = claimInteractor.claim(claimParams())
                advanceUntilIdle()

                // Assert
                assertThat(claim).isEqualTo(PredictionClaimFailure.WalletBusy.left())
                coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) }
                renewing.join()
            }

        @Test
        fun `GIVEN the key returns to active WHEN renew THEN the authorization is not sent`() = runTest {
            // Arrange
            val revocation = revocation(nonce = "7")
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(revocation, authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            givenWalletReads(listOf(key()))

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.RevocationFailed(key()).left()),
            ).inOrder()
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
        }

        @Test
        fun `GIVEN the authorization expired while revoking WHEN renew THEN it ends unsettled after one signing`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                val revokingReads = List(size = 400) { listOf(key(status = PolymarketSessionKeyStatus.REVOKING)) }
                givenWalletReads(*(revokingReads + listOf(listOf(key(status = PolymarketSessionKeyStatus.REVOKED))))
                    .toTypedArray())

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Unsettled(
                            key(status = PolymarketSessionKeyStatus.REVOKING),
                        ).left(),
                    ),
                )
                coVerify(exactly = 1) { signBatches(any(), any()) }
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            }

        @Test
        fun `GIVEN the revocation outlasts the deadline WHEN renew THEN the wallet is released unsettled at the signed deadline`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
                    walletState(listOf(key(status = PolymarketSessionKeyStatus.REVOKING))).right()
                val interactor = createInteractor()
                var progress: List<PolymarketSessionKeyProgress> = emptyList()

                // Act
                val renewing = launch { progress = interactor.renew(USER_WALLET_ID).toList() }
                advanceTimeBy(FOLLOW_UP_DEADLINE_MILLIS - 1)
                val batchBeforeDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}
                advanceTimeBy(2)
                renewing.join()
                val batchAfterDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}

                // Assert
                assertThat(batchBeforeDeadline).isEqualTo(PolymarketDepositWalletLock.Busy.left())
                assertThat(batchAfterDeadline).isEqualTo(Unit.right())
                assertThat(testScheduler.currentTime).isLessThan(FOLLOW_UP_DEADLINE_MILLIS + RELAY_POLL_MILLIS)
                assertThat(progress).containsExactly(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Unsettled(
                            key(status = PolymarketSessionKeyStatus.REVOKING),
                        ).left(),
                    ),
                ).inOrder()
                coVerify(exactly = 1) { signBatches(any(), any()) }
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            }

        @Test
        fun `GIVEN the authorization send outlasts the deadline WHEN renew THEN the wallet is released at the signed deadline`() =
            runTest {
                // Arrange
                val revocation = revocation(nonce = "7")
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(revocation, authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
                    walletState(listOf(key(status = PolymarketSessionKeyStatus.REVOKED))).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(any(), any()) } coAnswers {
                    awaitCancellation()
                }
                val interactor = createInteractor()
                var progress: List<PolymarketSessionKeyProgress> = emptyList()

                // Act
                val renewing = launch { progress = interactor.renew(USER_WALLET_ID).toList() }
                advanceTimeBy(FOLLOW_UP_DEADLINE_MILLIS - 1)
                val batchBeforeDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}
                advanceTimeBy(2)
                renewing.join()
                val batchAfterDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}

                // Assert
                assertThat(batchBeforeDeadline).isEqualTo(PolymarketDepositWalletLock.Busy.left())
                assertThat(batchAfterDeadline).isEqualTo(Unit.right())
                assertThat(progress.last()).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Unsettled(
                            key(status = PolymarketSessionKeyStatus.REVOKED),
                        ).left(),
                    ),
                )
                coVerify(exactly = 1) { signBatches(any(), any()) }
            }

        @Test
        fun `GIVEN signing outlasts the authorization deadline WHEN renew THEN it ends stale and nothing is revoked`() =
            runTest {
                // Arrange
                val signed = signed(revocation(nonce = "7"), authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE))
                coEvery {
                    signBatches(USER_WALLET_ID, nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()))
                } coAnswers {
                    delay(SENDABLE_FOLLOW_UP_MILLIS + RELAY_POLL_MILLIS)
                    signed.right()
                }

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                    ),
                )
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            }

        @Test
        fun `GIVEN no key on the device WHEN renew THEN nothing is signed`() = runTest {
            // Arrange
            coEvery { sessionKeyStore.heldAddress(USER_WALLET_ID) } returns null

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.KeyMissing.left()),
            )
            coVerify(exactly = 0) { signBatches(any(), any()) }
        }
    }

    @Nested
    inner class Replace {

        @Test
        fun `GIVEN the previous key is listed WHEN replace THEN it is revoked before the new key is authorized`() =
            runTest {
                // Arrange
                val revocation = revocation(address = PREVIOUS, nonce = "7")
                val authorization = authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE)
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(PREVIOUS), authorize()),
                    signed(revocation, authorization),
                )
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                    key(address = PREVIOUS, status = PolymarketSessionKeyStatus.REVOKED).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key().right()
                val replacement = PolymarketSessionKeyDeviceState.Replace.ByRevocation(key(address = PREVIOUS))

                // Act
                val actual = createInteractor().replace(USER_WALLET_ID, replacement).toList()

                // Assert
                assertThat(actual).containsAtLeast(
                    PolymarketSessionKeyProgress.Revoked(
                        key(address = PREVIOUS, status = PolymarketSessionKeyStatus.REVOKED),
                    ),
                    PolymarketSessionKeyProgress.Finished(key().right()),
                ).inOrder()
                coVerify(exactly = 1) { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) }
                coVerify(exactly = 1) { tradingStore.markAuthorized(USER_WALLET_ID, KEY) }
                coVerify(exactly = 0) { tradingStore.markAuthorized(USER_WALLET_ID, PREVIOUS) }
            }

        @Test
        fun `GIVEN the previous key is gone from Polymarket WHEN replace THEN it is overwritten in one batch`() =
            runTest {
                // Arrange
                val authorization = authorization(replaces = PREVIOUS)
                givenSigned(nonEmptyListOf(authorize(replaces = PREVIOUS)), signed(authorization))
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns
                    key().right()
                val replacement = PolymarketSessionKeyDeviceState.Replace.ByOverwrite(key(address = PREVIOUS))

                // Act
                val actual = createInteractor().replace(USER_WALLET_ID, replacement).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
            }

        @Test
        fun `GIVEN the previous key expired WHEN replace THEN the new key is plainly enabled`() = runTest {
            // Arrange
            val authorization = authorization()
            givenSigned(nonEmptyListOf(authorize()), signed(authorization))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, authorization) } returns key().right()
            val replacement = PolymarketSessionKeyDeviceState.Replace.ByRevocation(
                key(address = PREVIOUS, validUntil = NOW),
            )

            // Act
            val actual = createInteractor().replace(USER_WALLET_ID, replacement).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
            coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
        }
    }

    @Nested
    inner class Revoke {

        @Test
        fun `GIVEN a foreign signer WHEN revoke THEN it ends revoked`() = runTest {
            // Arrange
            val revocation = revocation(address = FOREIGN)
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(FOREIGN)), signed(revocation))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKING).right()
            givenWalletReads(listOf(key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKED)))

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, FOREIGN).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(
                    key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKED).right(),
                ),
            ).inOrder()
        }

        @Test
        fun `GIVEN an unknown signer the BFF never lists WHEN revoke THEN it counts as revoked`() = runTest {
            // Arrange
            val revocation = revocation(address = FOREIGN)
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(FOREIGN)), signed(revocation))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation) } returns
                key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKING).right()
            givenWalletReads(emptyList())

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, FOREIGN).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(
                    key(address = FOREIGN, status = PolymarketSessionKeyStatus.REVOKED).right(),
                ),
            ).inOrder()
        }

        @Test
        fun `GIVEN the active key is missing twice in a row WHEN revoke THEN it counts as revoked`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation()) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            givenWalletReads(emptyList(), emptyList(), listOf(key()))
            givenSignersWithoutKey()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            ).inOrder()
            coVerify(exactly = 2) { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) }
        }

        @Test
        fun `GIVEN the key is missing from one read and then listed WHEN revoke THEN the listed record settles it`() =
            runTest {
                // Arrange
                givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation()) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                givenWalletReads(
                    emptyList(),
                    listOf(key(status = PolymarketSessionKeyStatus.REVOKING)),
                    emptyList(),
                    listOf(key(status = PolymarketSessionKeyStatus.REVOKED, isEnabled = false)),
                )

                // Act
                val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

                // Assert
                assertThat(actual).containsExactly(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                    PolymarketSessionKeyProgress.Finished(
                        key(status = PolymarketSessionKeyStatus.REVOKED, isEnabled = false).right(),
                    ),
                ).inOrder()
            }

        @Test
        fun `GIVEN the key is missing from one read and then listed active WHEN revoke THEN the revocation failed`() =
            runTest {
                // Arrange
                givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation()) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                givenWalletReads(emptyList(), listOf(key()))

                // Act
                val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

                // Assert
                assertThat(actual).containsExactly(
                    PolymarketSessionKeyProgress.Signing,
                    PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.RevocationFailed(key()).left(),
                    ),
                ).inOrder()
            }

        @Test
        fun `GIVEN a failed read between two reads without the key WHEN revoke THEN it counts as revoked`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation()) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returnsMany listOf(
                walletState(emptyList()).right(),
                PolymarketWalletError.Network.left(),
                walletState(emptyList()).right(),
            )
            givenSignersWithoutKey()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual).containsExactly(
                PolymarketSessionKeyProgress.Signing,
                PolymarketSessionKeyProgress.Revoking(key(status = PolymarketSessionKeyStatus.REVOKING)),
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            ).inOrder()
            coVerify(exactly = 3) { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) }
        }

        @Test
        fun `GIVEN a dropped key Polymarket lists once and then not WHEN revoke THEN it counts as revoked`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                listOf(signer(KEY)).right(),
                emptyList<PolymarketSessionSigner>().right(),
            )

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            )
            coVerify(exactly = 2) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
            coVerify(exactly = 2) { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) }
        }

        @Test
        fun `GIVEN Polymarket keeps listing a dropped key WHEN revoke THEN it ends unsettled`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
            coVerify(exactly = UNLISTING_ATTEMPTS) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN a dropped key Polymarket lists as expired WHEN revoke THEN it counts as revoked`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                listOf(signer(KEY, validUntil = NOW.minusSeconds(1))).right()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            )
            coVerify(exactly = 1) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN Polymarket lists a dropped key without expiry WHEN revoke THEN it ends unsettled`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                listOf(signer(KEY, validUntil = null)).right()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
        }

        @Test
        fun `GIVEN the signers are refused WHEN revoke of a dropped key THEN it ends unsettled at once`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                PolymarketAuthError.InvalidSignature.left()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
            coVerify(exactly = 1) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN the signers fail locally WHEN revoke of a dropped key THEN it ends unsettled at once`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                PolymarketAuthError.Unknown(httpCode = null, detail = "headers").left()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
            coVerify(exactly = 1) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN the signers fail on the server WHEN revoke of a dropped key THEN they are read again`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                PolymarketAuthError.Unknown(httpCode = 503, detail = null).left(),
                emptyList<PolymarketSessionSigner>().right(),
            )

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            )
            coVerify(exactly = 2) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN the signers of a dropped key are not read once WHEN revoke THEN they are read again`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                PolymarketAuthError.Network.left(),
                emptyList<PolymarketSessionSigner>().right(),
            )

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            )
            coVerify(exactly = 2) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN the signers are never read WHEN revoke of a dropped key THEN it ends unsettled`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
                PolymarketAuthError.Network.left()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
            coVerify(exactly = UNLISTING_ATTEMPTS) { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) }
        }

        @Test
        fun `GIVEN no owner credentials WHEN revoke of a dropped key THEN it ends unsettled`() = runTest {
            // Arrange
            givenDroppedWhileRevoking()
            coEvery { getApiCredentials(USER_WALLET_ID) } returns null

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(
                        key(status = PolymarketSessionKeyStatus.REVOKING),
                    ).left(),
                ),
            )
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
        }

        @Test
        fun `GIVEN a failed read after one read without the key WHEN revoke THEN Polymarket is not asked`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation()) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returnsMany listOf(
                walletState(emptyList()).right(),
                PolymarketWalletError.Network.left(),
                walletState(listOf(key(status = PolymarketSessionKeyStatus.REVOKED))).right(),
            )

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList()

            // Assert
            assertThat(actual.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(key(status = PolymarketSessionKeyStatus.REVOKED).right()),
            )
            coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
        }

        @Test
        fun `GIVEN the key is busy WHEN revoke THEN the error is returned without resending`() = runTest {
            // Arrange
            givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, any()) } returns
                PolymarketSessionKeyError.SessionKeyBusy.left()

            // Act
            val actual = createInteractor().revoke(USER_WALLET_ID, KEY).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy).left(),
                ),
            )
            coVerify(exactly = 1) { sessionKeyRepository.revokeSessionKey(any(), any()) }
        }
    }

    @Nested
    inner class SetEnabled {

        @Test
        fun `GIVEN the wallet is busy once WHEN setEnabled THEN it is retried after a pause`() = runTest {
            // Arrange
            coEvery { sessionKeyRepository.setSessionKeyEnabled(USER_WALLET_ID, KEY, false) } returnsMany listOf(
                PolymarketSessionKeyError.SessionKeyBusy.left(),
                key(isEnabled = false).right(),
            )

            // Act
            val actual = createInteractor().setEnabled(USER_WALLET_ID, KEY, isEnabled = false)

            // Assert
            assertThat(actual).isEqualTo(key(isEnabled = false).right())
            assertThat(testScheduler.currentTime).isEqualTo(BUSY_RETRY_DELAY_MILLIS)
        }

        @Test
        fun `GIVEN the wallet stays busy WHEN setEnabled THEN it gives up after bounded retries`() = runTest {
            // Arrange
            coEvery { sessionKeyRepository.setSessionKeyEnabled(USER_WALLET_ID, KEY, true) } returns
                PolymarketSessionKeyError.SessionKeyBusy.left()

            // Act
            val actual = createInteractor().setEnabled(USER_WALLET_ID, KEY, isEnabled = true)

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyError.SessionKeyBusy.left())
            coVerify(exactly = 5) { sessionKeyRepository.setSessionKeyEnabled(USER_WALLET_ID, KEY, true) }
        }

        @Test
        fun `GIVEN the key is not toggleable WHEN setEnabled THEN it is not retried`() = runTest {
            // Arrange
            coEvery { sessionKeyRepository.setSessionKeyEnabled(USER_WALLET_ID, KEY, true) } returns
                PolymarketSessionKeyError.SessionKeyNotToggleable.left()

            // Act
            val actual = createInteractor().setEnabled(USER_WALLET_ID, KEY, isEnabled = true)

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyError.SessionKeyNotToggleable.left())
            coVerify(exactly = 1) { sessionKeyRepository.setSessionKeyEnabled(any(), any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PendingAuthorization {

        @BeforeEach
        fun resetPendingAuthorizations() = runTest {
            pendingAuthorizations.clear(USER_WALLET_ID)
            pendingAuthorizations.storeResult = Unit.right()
            pendingAuthorizations.readFailure = null
        }

        @Test
        fun `GIVEN the revocation is being sent WHEN renew THEN the pair is already stored and cleared once authorized`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                givenSigned(
                    nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                    signed(pending.revocation, pending.authorization),
                )
                var storedWhenRevoking: PolymarketPendingAuthorization? = null
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation) } coAnswers {
                    storedWhenRevoking = pendingAuthorizations.stored(USER_WALLET_ID)
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                }
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)), listOf(key()))

                // Act
                val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                assertThat(storedWhenRevoking).isEqualTo(pending)
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the revocation stays unsettled past the deadline WHEN renew THEN the pair is kept`() = runTest {
            // Arrange
            val pending = pendingOf()
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(pending.revocation, pending.authorization),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returns
                walletState(listOf(key(status = PolymarketSessionKeyStatus.REVOKING))).right()

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(key(status = PolymarketSessionKeyStatus.REVOKING))
                        .left(),
                ),
            )
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN the relayer refuses the revocation as stale WHEN renew THEN the pair is cleared`() = runTest {
            // Arrange
            val pending = pendingOf()
            val error = PolymarketSessionKeyError.RelayerRejected(detail = "expired")
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(pending.revocation, pending.authorization),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation) } returns
                error.left()

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.StaleSignature(cause = error).left(),
                ),
            )
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
        }

        @Test
        fun `GIVEN the store refuses the pair WHEN renew THEN it fails and nothing is sent`() = runTest {
            // Arrange
            val pending = pendingOf()
            val error = PolymarketSessionKeyError.KeyMissing
            pendingAuthorizations.storeResult = error.left()
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(pending.revocation, pending.authorization),
            )

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.Failed(error).left()),
            )
            coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
        }

        @Test
        fun `GIVEN the revocation was executed WHEN resume THEN the stored authorization is sent and the pair cleared`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)), listOf(key()))

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) }
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
                coVerify(exactly = 0) { signBatches(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the resumed authorization send outlasts the deadline WHEN resume THEN the wallet is released unsettled`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } coAnswers {
                    awaitCancellation()
                }
                givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)))
                val interactor = createInteractor()
                var progress: List<PolymarketSessionKeyProgress> = emptyList()

                // Act
                val resuming = launch { progress = interactor.resume(USER_WALLET_ID).toList() }
                advanceTimeBy(FOLLOW_UP_DEADLINE_MILLIS - 1)
                val batchBeforeDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}
                advanceTimeBy(2)
                resuming.join()
                val batchAfterDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}

                // Assert
                assertThat(batchBeforeDeadline).isEqualTo(PolymarketDepositWalletLock.Busy.left())
                assertThat(batchAfterDeadline).isEqualTo(Unit.right())
                assertThat(progress.last()).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Unsettled(
                            key(status = PolymarketSessionKeyStatus.REVOKED),
                        ).left(),
                    ),
                )
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
            }

        @Test
        fun `GIVEN the revocation was not executed WHEN resume THEN it is sent before the authorization`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(7).right()
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation) } returns
                key(status = PolymarketSessionKeyStatus.REVOKING).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(
                listOf(key(validUntil = NOW.plusSeconds(3_600))),
                listOf(key(status = PolymarketSessionKeyStatus.REVOKED)),
                listOf(key()),
            )

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
            coVerifyOrder {
                sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation)
                sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization)
            }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN the key is already recorded WHEN resume THEN nothing is sent and the pair is cleared`(
            model: RecordedModel,
        ) = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pendingOf())
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(9).right()
            givenWalletReads(listOf(key(status = model.status)), listOf(key()))

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
            coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
        }

        @Test
        fun `GIVEN the authorization expired WHEN resume THEN it ends stale, sends nothing and clears the pair`() =
            runTest {
                // Arrange
                val expired = expiredPendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, expired)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                givenWalletReads(emptyList())

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                    ),
                )
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the authorization is refused for good WHEN resume THEN the pair is cleared`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                PolymarketSessionKeyError.SessionKeyLimitReached.left()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)))

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyLimitReached).left(),
                ),
            )
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
        }

        @Test
        fun `GIVEN the revoked key stays active WHEN resume THEN the revocation failed and the pair is cleared`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(7).right()
                coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation) } returns
                    key(status = PolymarketSessionKeyStatus.REVOKING).right()
                givenWalletReads(listOf(key(validUntil = NOW.plusSeconds(3_600))), listOf(key()))

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.RevocationFailed(key()).left(),
                    ),
                )
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the wallet nonce cannot be read WHEN resume THEN it fails on the network and keeps the pair`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns
                    DataError.UserWalletError.WrongUserWallet(message = "boom").left()
                givenWalletReads(emptyList())

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network).left(),
                    ),
                )
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            }

        @Test
        fun `GIVEN the wallet nonce moved past the authorization WHEN resume THEN it ends stale and clears the pair`() =
            runTest {
                // Arrange
                pendingAuthorizations.store(USER_WALLET_ID, pendingOf())
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(9).right()
                givenWalletReads(emptyList())

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                    ),
                )
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @ParameterizedTest
        @MethodSource("provideUnrevokedModels")
        fun `GIVEN the nonce is past the revocation but the key shows none WHEN resume THEN it is unsettled and kept`(
            model: UnrevokedModel,
        ) = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
            givenWalletReads(model.sessionKeys)

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.Unsettled(model.sessionKeys.single()).left(),
                ),
            )
            coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN the wallet nonce read hangs WHEN resume THEN the wallet is released at the deadline`() = runTest {
            // Arrange
            pendingAuthorizations.store(USER_WALLET_ID, pendingOf())
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } coAnswers { awaitCancellation() }
            val interactor = createInteractor()
            var progress: List<PolymarketSessionKeyProgress> = emptyList()

            // Act
            val resuming = launch { progress = interactor.resume(USER_WALLET_ID).toList() }
            advanceTimeBy(FOLLOW_UP_DEADLINE_MILLIS - 1)
            val batchBeforeDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}
            advanceTimeBy(2)
            resuming.join()
            val batchAfterDeadline = depositWalletLock.withBatchOrBusy(USER_WALLET_ID) {}

            // Assert
            assertThat(batchBeforeDeadline).isEqualTo(PolymarketDepositWalletLock.Busy.left())
            assertThat(batchAfterDeadline).isEqualTo(Unit.right())
            assertThat(progress.last()).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                ),
            )
            coVerify(exactly = 0) { polymarketRepository.getWalletStatusByWalletId(any()) }
        }

        @Test
        fun `GIVEN the revocation nonce is already used WHEN renew THEN it ends stale and keeps the pair`() = runTest {
            // Arrange
            val pending = pendingOf()
            val error = PolymarketSessionKeyError.NonceAlreadyUsed
            givenSigned(
                nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY), authorize()),
                signed(pending.revocation, pending.authorization),
            )
            coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, pending.revocation) } returns
                error.left()

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(
                    PolymarketSessionKeyLifecycleError.StaleSignature(cause = error).left(),
                ),
            )
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN the authorization ends with an unknown server error WHEN resume THEN the pair is kept`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            val error = PolymarketSessionKeyError.Unknown(httpCode = 500, reason = null, detail = null)
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                error.left()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)))

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.Failed(error).left()),
            )
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN the revoked key record is dropped and unlisted WHEN resume THEN the authorization is sent`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                    emptyList<PolymarketSessionSigner>().right(),
                    listOf(signer(KEY)).right(),
                )
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(emptyList(), listOf(key()))

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) }
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the revoked key record is dropped but still listed WHEN resume THEN the pair is kept unsettled`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns listOf(signer(KEY)).right()
                givenWalletReads(emptyList())

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                val error = (actual as PolymarketSessionKeyProgress.Finished).result.leftOrNull()
                assertThat(error).isInstanceOf(PolymarketSessionKeyLifecycleError.Unsettled::class.java)
                val dropped = (error as PolymarketSessionKeyLifecycleError.Unsettled).sessionKey
                assertThat(dropped.address).isEqualTo(KEY)
                assertThat(dropped.status).isEqualTo(PolymarketSessionKeyStatus.REVOKING)
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
            }

        @Test
        fun `GIVEN the revoked key record is dropped and signers unread WHEN resume twice THEN the pair is kept and sent`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany
                    List(UNLISTING_ATTEMPTS) { PolymarketAuthError.Network.left() } + listOf(
                        emptyList<PolymarketSessionSigner>().right(),
                        listOf(signer(KEY)).right(),
                    )
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(emptyList(), emptyList(), listOf(key()))
                val interactor = createInteractor()

                // Act
                val first = interactor.resume(USER_WALLET_ID).toList().last()
                val keptAfterFirst = pendingAuthorizations.stored(USER_WALLET_ID)
                val second = interactor.resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(first).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.Network).left(),
                    ),
                )
                assertThat(keptAfterFirst).isEqualTo(pending)
                assertThat(second).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the revoked key record is dropped and listed for a while WHEN resume THEN the authorization is sent`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returnsMany listOf(
                    listOf(signer(KEY)).right(),
                    listOf(signer(KEY)).right(),
                    emptyList<PolymarketSessionSigner>().right(),
                    listOf(signer(KEY)).right(),
                )
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                    key(status = PolymarketSessionKeyStatus.PENDING).right()
                givenWalletReads(emptyList(), listOf(key()))

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
                coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN the revoked key record is dropped and unlisted WHEN the authorization fails THEN it follows a revocation`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                givenSignersWithoutKey()
                coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                    PolymarketSessionKeyError.SessionKeyBusy.left()
                givenWalletReads(emptyList())

                // Act
                val progress = createInteractor().resume(USER_WALLET_ID).toList()

                // Assert
                assertThat(progress.map { it::class }).containsExactly(
                    PolymarketSessionKeyProgress.Revoking::class,
                    PolymarketSessionKeyProgress.Revoked::class,
                    PolymarketSessionKeyProgress.Finished::class,
                ).inOrder()
                val revoked = progress.filterIsInstance<PolymarketSessionKeyProgress.Revoked>().single().sessionKey
                assertThat(revoked.address).isEqualTo(KEY)
                assertThat(revoked.status).isEqualTo(PolymarketSessionKeyStatus.REVOKED)
                assertThat(progress.last()).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.Failed(PolymarketSessionKeyError.SessionKeyBusy).left(),
                    ),
                )
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
            }

        @Test
        fun `GIVEN the revoked key record is dropped and no owner credentials WHEN resume THEN the pair is kept`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                coEvery { getApiCredentials(USER_WALLET_ID) } returns null
                givenWalletReads(emptyList())

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                val error = (actual as PolymarketSessionKeyProgress.Finished).result.leftOrNull()
                assertThat(error).isInstanceOf(PolymarketSessionKeyLifecycleError.Unsettled::class.java)
                val dropped = (error as PolymarketSessionKeyLifecycleError.Unsettled).sessionKey
                assertThat(dropped.address).isEqualTo(KEY)
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
                coVerify(exactly = 0) { sessionKeyRepository.getSessionSigners(any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
            }

        @Test
        fun `GIVEN the revocation is settling at its nonce WHEN resume THEN it is awaited, not resent`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(7).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(
                listOf(key(status = PolymarketSessionKeyStatus.REVOKING)),
                listOf(key(status = PolymarketSessionKeyStatus.REVOKED)),
                listOf(key()),
            )

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
            coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
            coVerify(exactly = 1) { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
        }

        @Test
        fun `GIVEN the revocation is within its send margin WHEN resume THEN it ends stale and the pair is kept`() =
            runTest {
                // Arrange
                val pending = pendingOf()
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(7).right()
                givenWalletReads(listOf(key(validUntil = NOW.plusSeconds(3_600))))
                delay(REVOCATION_EXPIRED_AFTER_MILLIS)

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                    ),
                )
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
            }

        @Test
        fun `GIVEN the revocation deadline passed while the authorization is alive WHEN resume THEN the pair is cleared`() =
            runTest {
                // Arrange
                pendingAuthorizations.store(USER_WALLET_ID, pendingOf())
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(7).right()
                givenWalletReads(listOf(key(validUntil = NOW.plusSeconds(3_600))))
                delay(REVOCATION_DEADLINE_PASSED_AFTER_MILLIS)

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                    ),
                )
                coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
            }

        @Test
        fun `GIVEN an executable pair WHEN enable THEN it is refused as in progress and the pair is kept`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
            coVerify(exactly = 0) { signBatches(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN an executable pair WHEN renew THEN it is refused as in progress and the pair is kept`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)

            // Act
            val actual = createInteractor().renew(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
            coVerify(exactly = 0) { signBatches(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN an expired pair WHEN enable THEN it proceeds and the pair is cleared`() = runTest {
            // Arrange
            val expired = expiredPendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, expired)
            givenSigned(nonEmptyListOf(authorize()), signed(authorization()))
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketSessionKeyProgress.Finished(key().right()))
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isNull()
        }

        @Test
        fun `GIVEN a pair 20 s before its deadline WHEN enable THEN it is refused as in progress and kept`() = runTest {
            // Arrange
            val pending = pendingDueIn(seconds = 20)
            pendingAuthorizations.store(USER_WALLET_ID, pending)

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
            coVerify(exactly = 0) { signBatches(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN the pair cannot be read WHEN enable THEN it is refused as in progress and nothing is signed`() =
            runTest {
                // Arrange
                pendingAuthorizations.readFailure = PolymarketSessionKeyError.Failure(IllegalStateException("locked"))

                // Act
                val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.OperationInProgress.left(),
                    ),
                )
                coVerify(exactly = 0) { signBatches(any(), any()) }
            }

        @Test
        fun `GIVEN the pair cannot be read WHEN resume THEN it fails, sends nothing and keeps the pair`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            val error = PolymarketSessionKeyError.Failure(IllegalStateException("locked"))
            pendingAuthorizations.readFailure = error

            // Act
            val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.Failed(error).left()),
            )
            coVerify(exactly = 0) { sessionKeyRepository.revokeSessionKey(any(), any()) }
            coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        @Test
        fun `GIVEN a pair 20 s before its deadline WHEN resume THEN it ends stale and keeps the pair until the deadline`() =
            runTest {
                // Arrange
                val pending = pendingDueIn(seconds = 20)
                pendingAuthorizations.store(USER_WALLET_ID, pending)
                coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
                givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)))

                // Act
                val actual = createInteractor().resume(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(
                    PolymarketSessionKeyProgress.Finished(
                        PolymarketSessionKeyLifecycleError.StaleSignature(cause = null).left(),
                    ),
                )
                coVerify(exactly = 0) { sessionKeyRepository.authorizeSessionKey(any(), any()) }
                assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
            }

        @Test
        fun `GIVEN a stored pair WHEN resume THEN the wallet nonce is read before the session keys`() = runTest {
            // Arrange
            val pending = pendingOf()
            pendingAuthorizations.store(USER_WALLET_ID, pending)
            coEvery { polymarketRepository.getRelayerNonce(OWNER) } returns BigInteger.valueOf(8).right()
            coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, pending.authorization) } returns
                key(status = PolymarketSessionKeyStatus.PENDING).right()
            givenWalletReads(listOf(key(status = PolymarketSessionKeyStatus.REVOKED)), listOf(key()))

            // Act
            createInteractor().resume(USER_WALLET_ID).toList()

            // Assert
            coVerifyOrder {
                polymarketRepository.getRelayerNonce(OWNER)
                polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue)
            }
        }

        @Test
        fun `GIVEN a pair 40 s before its deadline WHEN enable THEN it is refused as in progress and kept`() = runTest {
            // Arrange
            val pending = pendingDueIn(seconds = 40)
            pendingAuthorizations.store(USER_WALLET_ID, pending)

            // Act
            val actual = createInteractor().enable(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(
                PolymarketSessionKeyProgress.Finished(PolymarketSessionKeyLifecycleError.OperationInProgress.left()),
            )
            coVerify(exactly = 0) { signBatches(any(), any()) }
            assertThat(pendingAuthorizations.stored(USER_WALLET_ID)).isEqualTo(pending)
        }

        private fun pendingDueIn(seconds: Long) =
            pendingOf(authorization = authorization(nonce = "8", deadline = (NOW.epochSecond + seconds).toString()))

        private fun provideTestModels() = listOf(
            RecordedModel(status = PolymarketSessionKeyStatus.PENDING),
            RecordedModel(status = PolymarketSessionKeyStatus.ACTIVE),
        )

        private fun provideUnrevokedModels() = listOf(
            UnrevokedModel(sessionKeys = listOf(key(validUntil = NOW.plusSeconds(3_600)))),
        )

        private fun expiredPendingOf(): PolymarketPendingAuthorization {
            val pastDeadline = (NOW.epochSecond - 1).toString()
            return pendingOf(
                revocation = revocation(nonce = "7").copy(deadline = pastDeadline),
                authorization = authorization(nonce = "8", deadline = pastDeadline),
            )
        }

        private fun pendingOf(
            revocation: PolymarketSessionKeyRevocation = revocation(nonce = "7"),
            authorization: PolymarketSessionKeyAuthorization =
                authorization(nonce = "8", deadline = FOLLOW_UP_DEADLINE),
        ) = PolymarketPendingAuthorization(
            revocation = revocation,
            authorization = authorization,
            purpose = PolymarketPendingAuthorization.Purpose.EXTEND,
        )
    }

    data class RecordedModel(val status: PolymarketSessionKeyStatus)

    data class UnrevokedModel(val sessionKeys: List<PolymarketSessionKey>)

    private fun TestScope.createInteractor() = ManagePolymarketSessionKeyInteractor(
        signBatches = signBatches,
        sessionKeyRepository = sessionKeyRepository,
        polymarketRepository = polymarketRepository,
        sessionKeyStore = sessionKeyStore,
        deviceIdProvider = deviceIdProvider,
        sessionCredentialsProvider = sessionCredentialsProvider,
        tradingStore = tradingStore,
        getApiCredentials = getApiCredentials,
        deriveApiCredentials = deriveApiCredentials,
        depositWalletLock = depositWalletLock,
        pendingAuthorizations = pendingAuthorizations,
        clock = SchedulerClock(testScheduler),
    )

    private fun givenSigned(
        batches: NonEmptyList<PolymarketSessionKeyBatch>,
        signed: PolymarketSignedSessionKeyBatches,
    ) {
        coEvery { signBatches(USER_WALLET_ID, batches) } returns signed.right()
    }

    private fun givenSigningSuspended(): CompletableDeferred<Unit> {
        val signing = CompletableDeferred<Unit>()
        coEvery { signBatches(USER_WALLET_ID, any()) } coAnswers {
            signing.await()
            signed(authorization()).right()
        }
        coEvery { sessionKeyRepository.authorizeSessionKey(USER_WALLET_ID, any()) } returns key().right()
        return signing
    }

    private fun givenWalletReads(vararg sessionKeys: List<PolymarketSessionKey>) {
        coEvery { polymarketRepository.getWalletStatusByWalletId(USER_WALLET_ID.stringValue) } returnsMany
            sessionKeys.map { keys -> walletState(keys).right() }
    }

    private fun givenDroppedWhileRevoking() {
        givenSigned(nonEmptyListOf(PolymarketSessionKeyBatch.Revoke(KEY)), signed(revocation()))
        coEvery { sessionKeyRepository.revokeSessionKey(USER_WALLET_ID, revocation()) } returns
            key(status = PolymarketSessionKeyStatus.REVOKING).right()
        givenWalletReads(emptyList())
    }

    private fun givenSignersWithoutKey() {
        coEvery { sessionKeyRepository.getSessionSigners(OWNER_IDENTITY) } returns
            emptyList<PolymarketSessionSigner>().right()
    }

    private fun walletState(sessionKeys: List<PolymarketSessionKey>) = PolymarketWalletState(
        depositWalletAddress = DEPOSIT_WALLET,
        status = PolymarketWalletStatus.READY_TO_TRADE,
        sessionKeys = sessionKeys,
    )

    private class SchedulerClock(private val scheduler: TestCoroutineScheduler) : Clock() {

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId?): Clock = this

        override fun instant(): Instant = NOW.plusMillis(scheduler.currentTime)
    }

    private fun claimParams() = SubmitPredictionClaimInteractor.Params(
        userWalletId = USER_WALLET_ID,
        walletId = USER_WALLET_ID.stringValue,
        ownerAddress = OWNER,
        depositWalletAddress = OWNER,
        conditionId = "0xcondition",
        assetId = "1",
        isNegRisk = false,
        version = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0x2222222222222222222222222222222222222222"
        const val KEY = "0x5e55000000000000000000000000000000000001"
        const val PREVIOUS = "0x5e55000000000000000000000000000000000002"
        const val FOREIGN = "0x5e55000000000000000000000000000000000003"
        const val DEVICE_ID = "device"
        const val DEVICE_NAME = "Pixel"
        const val SETTLE_TIMEOUT_MILLIS = 30 * 60 * 1_000L
        const val BUSY_RETRY_DELAY_MILLIS = 1_500L
        const val LISTING_ATTEMPTS = 10
        const val LISTING_WAIT_MILLIS = (LISTING_ATTEMPTS - 1) * 3_000L
        const val UNLISTING_ATTEMPTS = 20
        const val RELAY_POLL_MILLIS = 3_000L
        const val SENDABLE_FOLLOW_UP_MILLIS = 1_200_000L - 30_000L
        const val FOLLOW_UP_DEADLINE_MILLIS = 1_200_000L
        const val REVOCATION_EXPIRED_AFTER_MILLIS = 600_000L - 30_000L
        const val REVOCATION_DEADLINE_PASSED_AFTER_MILLIS = 601_000L
        val NOW: Instant = Instant.ofEpochSecond(1_789_430_000)
        val VALID_UNTIL: Instant = NOW.plusSeconds(180L * 24 * 60 * 60)
        val DEADLINE = (NOW.epochSecond + 600).toString()
        val FOLLOW_UP_DEADLINE = (NOW.epochSecond + 1_200).toString()
        val CALL = PolymarketApprovalCall(target = DEPOSIT_WALLET, value = "0", data = "0x")
        val OWNER_CREDENTIALS = PolymarketApiCredentials(apiKey = "owner", secret = "s", passphrase = "p")
        val SESSION_CREDENTIALS = PolymarketApiCredentials(apiKey = "session", secret = "s", passphrase = "p")
        val OWNER_IDENTITY = PolymarketClobIdentity.Owner(address = OWNER, credentials = OWNER_CREDENTIALS)

        fun key(
            address: String = KEY,
            status: PolymarketSessionKeyStatus = PolymarketSessionKeyStatus.ACTIVE,
            isEnabled: Boolean = true,
            validUntil: Instant? = VALID_UNTIL,
        ) = PolymarketSessionKey(
            address = address,
            deviceId = DEVICE_ID,
            deviceName = DEVICE_NAME,
            isEnabled = isEnabled,
            status = status,
            scopes = listOf("CLOB"),
            validUntil = validUntil,
            renewAfter = null,
            authorizedAt = null,
            createdAt = NOW,
        )

        fun signer(address: String, validUntil: Instant? = VALID_UNTIL) =
            PolymarketSessionSigner(address = address, scopes = listOf("CLOB"), validUntil = validUntil)

        fun authorize(replaces: String? = null) = PolymarketSessionKeyBatch.Authorize(
            sessionKeyAddress = KEY,
            deviceId = DEVICE_ID,
            deviceName = DEVICE_NAME,
            replacesSessionKeyAddress = replaces,
        )

        fun authorization(nonce: String = "7", deadline: String = DEADLINE, replaces: String? = null) =
            PolymarketSessionKeyAuthorization(
                ownerAddress = OWNER,
                depositWalletAddress = DEPOSIT_WALLET,
                sessionKeyAddress = KEY,
                deviceId = DEVICE_ID,
                deviceName = DEVICE_NAME,
                validUntil = VALID_UNTIL.epochSecond.toString(),
                nonce = nonce,
                deadline = deadline,
                call = CALL,
                signature = "0xsig$nonce",
                replacesSessionKeyAddress = replaces,
            )

        fun revocation(address: String = KEY, nonce: String = "7") = PolymarketSessionKeyRevocation(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            sessionKeyAddress = address,
            nonce = nonce,
            deadline = DEADLINE,
            call = CALL,
            signature = "0xrevoke$nonce",
        )

        fun signed(
            authorization: PolymarketSessionKeyAuthorization,
            ownerClobAuth: PolymarketSignedOnboarding.SignedClobAuth? = null,
        ) = PolymarketSignedSessionKeyBatches(
            ownerClobAuth = ownerClobAuth,
            batches = listOf(PolymarketSignedSessionKeyBatches.Signed.Authorization(authorization)),
        )

        fun signed(revocation: PolymarketSessionKeyRevocation) = PolymarketSignedSessionKeyBatches(
            ownerClobAuth = null,
            batches = listOf(PolymarketSignedSessionKeyBatches.Signed.Revocation(revocation)),
        )

        fun signed(
            revocation: PolymarketSessionKeyRevocation,
            authorization: PolymarketSessionKeyAuthorization,
        ) = PolymarketSignedSessionKeyBatches(
            ownerClobAuth = null,
            batches = listOf(
                PolymarketSignedSessionKeyBatches.Signed.Revocation(revocation),
                PolymarketSignedSessionKeyBatches.Signed.Authorization(authorization),
            ),
        )
    }
}