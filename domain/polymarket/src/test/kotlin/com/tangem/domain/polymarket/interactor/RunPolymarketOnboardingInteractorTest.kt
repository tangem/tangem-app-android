package com.tangem.domain.polymarket.interactor

import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketOnboardedStore
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketSignRequest
import com.tangem.domain.polymarket.signing.PolymarketSigned
import com.tangem.domain.polymarket.signing.PolymarketSigningSession
import com.tangem.domain.polymarket.signing.PolymarketTypedPayload
import com.tangem.domain.polymarket.usecase.DeployDepositWalletUseCase
import com.tangem.domain.polymarket.usecase.DeriveApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.DerivePolymarketAddressesUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketApiCredentialsUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketRelayerNonceUseCase
import com.tangem.domain.polymarket.usecase.GetPolymarketWalletStatusUseCase
import com.tangem.domain.polymarket.usecase.SubmitApprovalsUseCase
import com.tangem.domain.polymarket.usecase.SyncBalanceAllowanceUseCase
import com.tangem.domain.polymarket.usecase.toOnboardingError
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.math.BigInteger
import kotlin.reflect.KClass

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class RunPolymarketOnboardingInteractorTest {

    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()
    private val getRelayerNonce: GetPolymarketRelayerNonceUseCase = mockk()
    private val deployDepositWallet: DeployDepositWalletUseCase = mockk()
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()
    private val deriveApiCredentials: DeriveApiCredentialsUseCase = mockk()
    private val submitApprovals: SubmitApprovalsUseCase = mockk()
    private val syncBalanceAllowance: SyncBalanceAllowanceUseCase = mockk()
    private val onboardedStore: PolymarketOnboardedStore = mockk()
    private val predictionAccountStatusFetcher: PredictionAccountStatusFetcher = mockk()

    private lateinit var session: FakeSigningSession
    private lateinit var useCase: RunPolymarketOnboardingInteractor

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            deriveAddresses,
            getWalletStatus,
            getRelayerNonce,
            deployDepositWallet,
            getApiCredentials,
            deriveApiCredentials,
            submitApprovals,
            syncBalanceAllowance,
            onboardedStore,
            predictionAccountStatusFetcher,
        )
        session = FakeSigningSession()
        useCase = RunPolymarketOnboardingInteractor(
            signingSession = session,
            deriveAddresses = deriveAddresses,
            getWalletStatus = getWalletStatus,
            getRelayerNonce = getRelayerNonce,
            deployDepositWallet = deployDepositWallet,
            getApiCredentials = getApiCredentials,
            deriveApiCredentials = deriveApiCredentials,
            submitApprovals = submitApprovals,
            syncBalanceAllowance = syncBalanceAllowance,
            polymarketOnboardedStore = onboardedStore,
            predictionAccountStatusFetcher = predictionAccountStatusFetcher,
            nowMillis = { NOW_MILLIS },
        )
        every { deriveAddresses.fromOwner(USER_WALLET_ID, OWNER) } returns ADDRESSES.right()
        coEvery { getRelayerNonce(ADDRESSES) } returns NONCE.right()
        coEvery { deployDepositWallet(ADDRESSES) } returns
            PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right()
        coEvery { submitApprovals(ADDRESSES, SIGNED_APPROVALS) } returns
            PolymarketWalletStatus.APPROVALS_IN_PROGRESS.right()
        coEvery { deriveApiCredentials(USER_WALLET_ID, OWNER, L1_SIGNATURE, TIMESTAMP) } returns CREDENTIALS.right()
        coEvery { getApiCredentials(USER_WALLET_ID) } returns null
        coEvery { syncBalanceAllowance(OWNER, CREDENTIALS) } returns Unit.right()
        coEvery { onboardedStore.isOnboardedSync(any()) } returns false
        coEvery { onboardedStore.markOnboarded(any()) } just Runs
        coEvery { predictionAccountStatusFetcher(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN nothing is set up WHEN collected THEN drives the run to Ready`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.NOT_CREATED).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        assertThat(session.signedPayloads).containsExactly(listOf(CLOB_AUTH_PAYLOAD, BATCH_PAYLOAD))
        coVerify(exactly = 1) { deployDepositWallet(ADDRESSES) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED_APPROVALS) }
        coVerify(exactly = 1) { deriveApiCredentials(USER_WALLET_ID, OWNER, L1_SIGNATURE, TIMESTAMP) }
        coVerifyOrder {
            deployDepositWallet(ADDRESSES)
            deriveApiCredentials(USER_WALLET_ID, OWNER, L1_SIGNATURE, TIMESTAMP)
        }
    }

    @Test
    fun `GIVEN a full run WHEN it reaches Ready THEN primes the cache with the derived credentials`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.NOT_CREATED).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            skipItems(3)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        coVerify(exactly = 1) { syncBalanceAllowance(OWNER, CREDENTIALS) }
    }

    @Test
    fun `GIVEN a wallet already onboarded WHEN collected THEN primes the cache with the stored credentials`() =
        runTest {
            // Arrange
            coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
            coEvery { getWalletStatus(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }
            coVerify(exactly = 1) { syncBalanceAllowance(OWNER, CREDENTIALS) }
            assertThat(session.signedPayloads).isEmpty()
        }

    @Test
    fun `GIVEN the cache prime fails WHEN collected THEN still reports Ready`() = runTest {
        // Arrange
        coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { getWalletStatus(ADDRESSES) } returns
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { syncBalanceAllowance(OWNER, CREDENTIALS) } returns
            PolymarketOnboardingError.Network.left()

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN the cache prime throws WHEN collected THEN still reports Ready`() = runTest {
        // Arrange
        coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { getWalletStatus(ADDRESSES) } returns
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { syncBalanceAllowance(OWNER, CREDENTIALS) } throws IllegalArgumentException("bad secret")

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN the status never settles WHEN collected THEN it keeps waiting`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returns
            walletState(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS).right()

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )

            advanceTimeBy(WELL_PAST_THE_OLD_CEILING_MILLIS)
            runCurrent()
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { submitApprovals(any(), any()) }
    }

    @Test
    fun `GIVEN the nonce read fails WHEN collected THEN fails before signing anything`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.NOT_CREATED).right()
        coEvery { getRelayerNonce(ADDRESSES) } returns PolymarketOnboardingError.Network.left()

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Failed(error = PolymarketOnboardingError.Network, isRetryable = true),
            )
            awaitComplete()
        }
        assertThat(session.signedPayloads).isEmpty()
    }

    @Test
    fun `GIVEN the wallet is onboarded and credentials are stored WHEN collected THEN Ready without a tap`() =
        runTest {
            // Arrange
            coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
            coEvery { getWalletStatus(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }
            assertThat(session.signedPayloads).isEmpty()
            coVerify(exactly = 0) { getRelayerNonce(any()) }
            coVerify(exactly = 0) { deployDepositWallet(any()) }
            coVerify(exactly = 0) { submitApprovals(any(), any()) }
        }

    @Test
    fun `GIVEN the wallet is onboarded but credentials are missing WHEN collected THEN taps only for them`() =
        runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }
            assertThat(session.signedPayloads).containsExactly(listOf(CLOB_AUTH_PAYLOAD))
            coVerify(exactly = 1) { deriveApiCredentials(USER_WALLET_ID, OWNER, L1_SIGNATURE, TIMESTAMP) }
            coVerify(exactly = 0) { deployDepositWallet(any()) }
            coVerify(exactly = 0) { submitApprovals(any(), any()) }
        }

    @Test
    fun `GIVEN a deploy is already in flight WHEN collected THEN does not deploy again`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED_APPROVALS) }
    }

    @Test
    fun `GIVEN the wallet is deployed WHEN collected THEN submits without waiting for a deploy`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED_APPROVALS) }
    }

    @Test
    fun `GIVEN the wallet is deployed and credentials are stored WHEN collected THEN does not re-derive credentials`() =
        runTest {
            // Arrange
            coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
            coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                walletState(PolymarketWalletStatus.DEPLOYED).right(),
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
            )

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                )
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
                awaitComplete()
            }
            coVerify(exactly = 0) { deriveApiCredentials(any(), any(), any(), any()) }
            coVerify(exactly = 0) { deployDepositWallet(any()) }
            coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED_APPROVALS) }
        }

    @Test
    fun `GIVEN a confirmed wallet the backend demoted to deployed WHEN collected THEN Ready as an existing account`() =
        runTest {
            // Arrange
            coEvery { onboardedStore.isOnboardedSync(USER_WALLET_ID) } returns true
            coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
            coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                walletState(PolymarketWalletStatus.DEPLOYED).right(),
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
            )

            // Act
            val actual = useCase(USER_WALLET_ID).toList().last()

            // Assert
            assertThat(actual).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
        }

    @Test
    fun `GIVEN approvals are in flight and credentials are stored WHEN collected THEN only waits`() = runTest {
        // Arrange
        coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.APPROVALS_IN_PROGRESS).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        assertThat(session.signedPayloads).isEmpty()
        coVerify(exactly = 0) { submitApprovals(any(), any()) }
    }

    @Test
    fun `GIVEN approvals are in flight and credentials are missing WHEN collected THEN taps but does not resubmit`() =
        runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                walletState(PolymarketWalletStatus.APPROVALS_IN_PROGRESS).right(),
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
            )

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                )
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
                awaitComplete()
            }
            assertThat(session.signedPayloads).containsExactly(listOf(CLOB_AUTH_PAYLOAD))
            coVerify(exactly = 0) { submitApprovals(any(), any()) }
        }

    @Test
    fun `GIVEN previous approvals failed WHEN collected THEN re-signs and resubmits without deploying`() =
        runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                walletState(PolymarketWalletStatus.APPROVALS_FAILED).right(),
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
            )

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                )
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
                awaitComplete()
            }
            assertThat(session.signedPayloads).containsExactly(listOf(CLOB_AUTH_PAYLOAD, BATCH_PAYLOAD))
            coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED_APPROVALS) }
            coVerify(exactly = 0) { deployDepositWallet(any()) }
        }

    @Test
    fun `GIVEN an unrecognised status WHEN collected THEN owes approvals but does not deploy`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.UNKNOWN).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.UNKNOWN),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED_APPROVALS) }
    }

    @Test
    fun `GIVEN a previous deploy failed WHEN collected THEN deploys again`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYMENT_FAILED).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
        coVerify(exactly = 1) { deployDepositWallet(ADDRESSES) }
    }

    @Test
    fun `GIVEN the deploy fails while waiting WHEN collected THEN fails retryably`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.NOT_CREATED).right(),
            walletState(PolymarketWalletStatus.DEPLOYMENT_FAILED).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.DeploymentFailed,
                    isRetryable = true,
                ),
            )
            awaitComplete()
        }
        coVerify(exactly = 0) { submitApprovals(any(), any()) }
    }

    @Test
    fun `GIVEN the approvals fail while waiting WHEN collected THEN fails retryably`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.APPROVALS_FAILED).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.ApprovalsFailed,
                    isRetryable = true,
                ),
            )
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN one poll fails WHEN collected THEN the run continues`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            PolymarketOnboardingError.Network.left(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN three polls fail in a row WHEN collected THEN fails with Network`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            PolymarketOnboardingError.Network.left(),
            PolymarketOnboardingError.Network.left(),
            PolymarketOnboardingError.Network.left(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.Network,
                    isRetryable = true,
                ),
            )
            awaitComplete()
        }
        coVerify(exactly = 4) { getWalletStatus(ADDRESSES) }
    }

    @Test
    fun `GIVEN a non-network poll error WHEN collected THEN fails immediately`() = runTest {
        // Arrange
        val error = PolymarketOnboardingError.Wallet(PolymarketWalletError.Unauthorized)
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            error.left(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Failed(error = error, isRetryable = false),
            )
            awaitComplete()
        }
    }

    @Test
    fun `GIVEN the user cancels the tap WHEN collected THEN fails retryably and stops`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.NOT_CREATED).right()
        session.signing = { PolymarketSigningError.UserCancelled.left() }

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Failed(
                    error = PolymarketOnboardingError.Signing(PolymarketSigningError.UserCancelled),
                    isRetryable = true,
                ),
            )
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
    }

    @Test
    fun `GIVEN a run is polling WHEN the collector cancels THEN polling stops`() = runTest {
        // Arrange
        var pollCount = 0
        coEvery { getWalletStatus(ADDRESSES) } coAnswers {
            pollCount++
            walletState(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS).right()
        }

        // Act
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            cancelAndIgnoreRemainingEvents()
        }
        val pollCountAtCancellation = pollCount
        advanceTimeBy(TWELVE_POLL_INTERVALS_MILLIS)

        // Assert
        assertThat(pollCount).isEqualTo(pollCountAtCancellation)
    }

    @Test
    fun `GIVEN reading stored credentials throws WHEN collected THEN fails with Unknown instead of crashing`() =
        runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.NOT_CREATED).right()
            coEvery { getApiCredentials(USER_WALLET_ID) } throws IllegalStateException("keystore")

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Failed(
                        error = PolymarketOnboardingError.Unknown,
                        isRetryable = true,
                    ),
                )
                awaitComplete()
            }
        }

    private fun walletState(status: PolymarketWalletStatus) = PolymarketWalletState(
        depositWalletAddress = DEPOSIT_WALLET,
        status = status,
    )

    private class FakeSigningSession(
        private val ownerAddress: String = OWNER,
        var derivation: Either<PolymarketSigningError, Unit> = Unit.right(),
        var signing: (List<PolymarketTypedPayload>) -> Either<PolymarketSigningError, List<String>> = { payloads ->
            payloads.map { payload ->
                when (payload) {
                    is PolymarketTypedPayload.ClobAuth -> L1_SIGNATURE
                    is PolymarketTypedPayload.RelayedBatch -> BATCH_SIGNATURE
                }
            }.right()
        },
    ) : PolymarketSigningSession {

        val signedPayloads = mutableListOf<List<PolymarketTypedPayload>>()
        var runs = 0

        override suspend fun <T> run(
            userWalletId: UserWalletId,
            prepare: suspend (ownerAddress: String) -> PolymarketSignRequest<T>,
        ): Either<PolymarketSigningError, PolymarketSigned<T>> {
            runs++
            derivation.onLeft { return it.left() }
            val request = prepare(ownerAddress)
            if (request.payloads.isEmpty()) return PolymarketSigned(request.value, emptyList()).right()

            signedPayloads += request.payloads
            return signing(request.payloads).map { signatures -> PolymarketSigned(request.value, signatures) }
        }
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val L1_SIGNATURE = "0xaa"
        const val BATCH_SIGNATURE = "0xbb"
        const val NOW_MILLIS = 1_735_689_600_000L
        const val TIMESTAMP = "1735689600"
        const val TWELVE_POLL_INTERVALS_MILLIS = 30_000L
        const val WELL_PAST_THE_OLD_CEILING_MILLIS = 300_000L
        const val STATUS_LATENCY_MILLIS = 300L
        const val IN_SESSION_TIMEOUT_MILLIS = 10_000L

        val USER_WALLET_ID = UserWalletId("011")
        val NONCE: BigInteger = BigInteger.valueOf(7)

        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = USER_WALLET_ID,
        )

        val APPROVALS = PolymarketApprovalsPayload(
            depositWalletAddress = DEPOSIT_WALLET,
            nonce = "7",
            deadline = "1735690200",
            calls = PolymarketApprovalCalls.build(),
        )

        val CLOB_AUTH_PAYLOAD = PolymarketTypedPayload.ClobAuth(PolymarketClobAuthData(timestamp = TIMESTAMP))
        val BATCH_PAYLOAD = PolymarketTypedPayload.RelayedBatch(APPROVALS)

        val SIGNED_APPROVALS = PolymarketSignedOnboarding.SignedApprovals(
            payload = APPROVALS,
            signature = BATCH_SIGNATURE,
        )

        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
    }

    @Test
    fun `GIVEN the run reaches Ready WHEN collected THEN the wallet is recorded as confirmed`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
        coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS

        // Act
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
            awaitComplete()
        }

        // Assert
        coVerifyOrder {
            onboardedStore.markOnboarded(USER_WALLET_ID)
            syncBalanceAllowance(OWNER, CREDENTIALS)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ExistingAccount {

        @Test
        fun `GIVEN an existing deposit wallet WHEN credentials are missing THEN they restore`() = runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }
            coVerify(exactly = 1) { deriveApiCredentials(USER_WALLET_ID, OWNER, L1_SIGNATURE, TIMESTAMP) }
            coVerify(exactly = 0) { deployDepositWallet(any()) }
        }
    }
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StatusFromPostResponse {

        @Test
        fun `GIVEN the deploy answers DEPLOYMENT_FAILED WHEN collected THEN fails without another status read`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.NOT_CREATED).right()
                coEvery { deployDepositWallet(ADDRESSES) } returns PolymarketWalletStatus.DEPLOYMENT_FAILED.right()

                // Act & Assert
                useCase(USER_WALLET_ID).test {
                    assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                    assertThat(awaitItem()).isEqualTo(
                        PolymarketOnboardingProgress.Failed(
                            error = PolymarketOnboardingError.DeploymentFailed,
                            isRetryable = true,
                        ),
                    )
                    awaitComplete()
                }
                coVerify(exactly = 1) { getWalletStatus(ADDRESSES) }
                coVerify(exactly = 0) { deriveApiCredentials(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN the approvals answer APPROVALS_FAILED WHEN collected THEN fails without another status read`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.DEPLOYED).right()
                coEvery { submitApprovals(ADDRESSES, SIGNED_APPROVALS) } returns PolymarketWalletStatus.APPROVALS_FAILED.right()

                // Act & Assert
                useCase(USER_WALLET_ID).test {
                    assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                    assertThat(awaitItem()).isEqualTo(
                        PolymarketOnboardingProgress.Failed(
                            error = PolymarketOnboardingError.ApprovalsFailed,
                            isRetryable = true,
                        ),
                    )
                    awaitComplete()
                }
                coVerify(exactly = 1) { getWalletStatus(ADDRESSES) }
            }

        @Test
        fun `GIVEN the deploy answers READY_TO_TRADE WHEN collected THEN reaches Ready without approvals`() = runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.NOT_CREATED).right()
            coEvery { deployDepositWallet(ADDRESSES) } returns PolymarketWalletStatus.READY_TO_TRADE.right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
                awaitComplete()
            }
            coVerify(exactly = 1) { getWalletStatus(ADDRESSES) }
            coVerify(exactly = 0) { submitApprovals(any(), any()) }
        }

        @Test
        fun `GIVEN the deploy answers APPROVALS_IN_PROGRESS WHEN collected THEN waits without resubmitting`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                    walletState(PolymarketWalletStatus.NOT_CREATED).right(),
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
                )
                coEvery { deployDepositWallet(ADDRESSES) } returns
                    PolymarketWalletStatus.APPROVALS_IN_PROGRESS.right()

                // Act & Assert
                useCase(USER_WALLET_ID).test {
                    assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                    assertThat(awaitItem()).isEqualTo(
                        PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                    )
                    assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
                    awaitComplete()
                }
                coVerify(exactly = 2) { getWalletStatus(ADDRESSES) }
                coVerify(exactly = 0) { submitApprovals(any(), any()) }
            }
    }
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AccountStatusRefresh {

        @Test
        fun `GIVEN the run reaches Ready WHEN collected THEN the account status is refreshed last`() = runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS

            // Act
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }

            // Assert
            coVerifyOrder {
                onboardedStore.markOnboarded(USER_WALLET_ID)
                syncBalanceAllowance(OWNER, CREDENTIALS)
                predictionAccountStatusFetcher(PredictionAccountStatusFetcher.Params(
                    userWalletId = USER_WALLET_ID,
                ))
            }
        }

        @Test
        fun `GIVEN the account status refresh fails WHEN collected THEN still reports Ready`() = runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
            coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
            coEvery { predictionAccountStatusFetcher(any()) } returns IllegalStateException("store is gone").left()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }
            coVerify(exactly = 1) { predictionAccountStatusFetcher(PredictionAccountStatusFetcher.Params(
                    userWalletId = USER_WALLET_ID,
                )) }
        }

        @Test
        fun `GIVEN the run fails WHEN collected THEN the account status is not refreshed`() = runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.NOT_CREATED).right()
            coEvery { deployDepositWallet(ADDRESSES) } returns PolymarketWalletStatus.DEPLOYMENT_FAILED.right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                skipItems(1)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Failed(
                        error = PolymarketOnboardingError.DeploymentFailed,
                        isRetryable = true,
                    ),
                )
                awaitComplete()
            }
            coVerify(exactly = 0) { predictionAccountStatusFetcher(any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OneSession {

        @Test
        fun `GIVEN a fresh wallet WHEN collected THEN one session signs ClobAuth and approvals together`() = runTest {
            // Arrange
            coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                walletState(PolymarketWalletStatus.NOT_CREATED).right(),
                walletState(PolymarketWalletStatus.DEPLOYED).right(),
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
            )

            // Act
            useCase(USER_WALLET_ID).test { cancelAndConsumeRemainingEvents() }

            // Assert
            assertThat(session.runs).isEqualTo(1)
            assertThat(session.signedPayloads.single().map { it::class })
                .containsExactly(PolymarketTypedPayload.ClobAuth::class, PolymarketTypedPayload.RelayedBatch::class)
                .inOrder()
        }

        @Test
        fun `GIVEN Reconnect WHEN collected THEN signs only ClobAuth and never reads the nonce`() = runTest {
            // Arrange
            coEvery { onboardedStore.isOnboardedSync(USER_WALLET_ID) } returns true
            coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

            // Act
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                awaitComplete()
            }

            // Assert
            assertThat(session.signedPayloads.single()).containsExactly(
                PolymarketTypedPayload.ClobAuth(PolymarketClobAuthData(timestamp = TIMESTAMP)),
            )
            coVerify(exactly = 0) { getRelayerNonce(any()) }
        }

        @Test
        fun `GIVEN credentials are stored but approvals are owed WHEN collected THEN signs only the approvals`() =
            runTest {
                // Arrange
                coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
                coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                    walletState(PolymarketWalletStatus.DEPLOYED).right(),
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
                )

                // Act
                useCase(USER_WALLET_ID).test { cancelAndConsumeRemainingEvents() }

                // Assert
                assertThat(session.signedPayloads.single().single())
                    .isInstanceOf(PolymarketTypedPayload.RelayedBatch::class.java)
                coVerify(exactly = 0) { deriveApiCredentials(any(), any(), any(), any()) }
            }

        @Test
        fun `GIVEN the status read fails inside the session WHEN collected THEN fails retryably and signs nothing`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } returns PolymarketOnboardingError.Network.left()

                // Act
                val progress = useCase(USER_WALLET_ID).toList()

                // Assert
                assertThat(progress).containsExactly(
                    PolymarketOnboardingProgress.AwaitingSignature,
                    PolymarketOnboardingProgress.Failed(PolymarketOnboardingError.Network, isRetryable = true),
                ).inOrder()
                assertThat(session.signedPayloads).isEmpty()
            }

        @Test
        fun `GIVEN the status read hangs WHEN collected THEN fails with Network after the in-session timeout`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } coAnswers { awaitCancellation() }

                // Act
                val progress = useCase(USER_WALLET_ID).toList()

                // Assert
                assertThat(progress).containsExactly(
                    PolymarketOnboardingProgress.AwaitingSignature,
                    PolymarketOnboardingProgress.Failed(PolymarketOnboardingError.Network, isRetryable = true),
                ).inOrder()
                assertThat(currentTime).isEqualTo(10_000)
                assertThat(session.signedPayloads).isEmpty()
            }

        @Test
        fun `GIVEN the nonce read hangs WHEN collected THEN fails with Network after the in-session timeout`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } returns walletState(PolymarketWalletStatus.NOT_CREATED).right()
                coEvery { getRelayerNonce(ADDRESSES) } coAnswers { awaitCancellation() }

                // Act
                val progress = useCase(USER_WALLET_ID).toList()

                // Assert
                assertThat(progress).containsExactly(
                    PolymarketOnboardingProgress.AwaitingSignature,
                    PolymarketOnboardingProgress.Failed(PolymarketOnboardingError.Network, isRetryable = true),
                ).inOrder()
                assertThat(currentTime).isEqualTo(10_000)
                assertThat(session.signedPayloads).isEmpty()
            }

        @Test
        fun `GIVEN the derivation is cancelled WHEN collected THEN fails with UserCancelled`() = runTest {
            // Arrange
            session.derivation = PolymarketSigningError.UserCancelled.left()

            // Act
            val progress = useCase(USER_WALLET_ID).toList()

            // Assert
            assertThat(progress.first()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat((progress.last() as PolymarketOnboardingProgress.Failed).error)
                .isEqualTo(PolymarketSigningError.UserCancelled.toOnboardingError())
            assertThat(progress).hasSize(2)
            coVerify(exactly = 0) { getWalletStatus(any()) }
        }

        @Test
        fun `GIVEN approvals in flight and no credentials WHEN collected THEN signs only ClobAuth without a nonce`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                    walletState(PolymarketWalletStatus.APPROVALS_IN_PROGRESS).right(),
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
                )

                // Act
                useCase(USER_WALLET_ID).test { cancelAndConsumeRemainingEvents() }

                // Assert
                assertThat(session.signedPayloads.single().single())
                    .isInstanceOf(PolymarketTypedPayload.ClobAuth::class.java)
                coVerify(exactly = 0) { submitApprovals(any(), any()) }
            }

        @Test
        fun `GIVEN a likely fresh account WHEN collected THEN the nonce is read while the status is in flight`() =
            runTest {
                // Arrange
                val reads = mutableListOf<String>()
                coEvery { getWalletStatus(ADDRESSES) } coAnswers {
                    reads += "status started"
                    delay(STATUS_LATENCY_MILLIS)
                    reads += "status answered"
                    walletState(PolymarketWalletStatus.NOT_CREATED).right()
                } andThenMany listOf(
                    walletState(PolymarketWalletStatus.DEPLOYED).right(),
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
                )
                coEvery { getRelayerNonce(ADDRESSES) } coAnswers {
                    reads += "nonce"
                    NONCE.right()
                }

                // Act
                val last = useCase(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(last).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = true))
                assertThat(reads.indexOf("nonce")).isLessThan(reads.indexOf("status answered"))
                assertThat(session.signedPayloads.single().map { it::class })
                    .containsExactly(PolymarketTypedPayload.ClobAuth::class, PolymarketTypedPayload.RelayedBatch::class)
                    .inOrder()
            }

        @Test
        fun `GIVEN the nonce read ahead fails and no approvals are owed WHEN collected THEN the run still reaches Ready`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } coAnswers {
                    delay(STATUS_LATENCY_MILLIS)
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
                }
                coEvery { getRelayerNonce(ADDRESSES) } returns PolymarketOnboardingError.Network.left()

                // Act
                val actual = useCase(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                assertThat(session.signedPayloads).containsExactly(listOf(CLOB_AUTH_PAYLOAD))
                coVerify(exactly = 1) { getRelayerNonce(ADDRESSES) }
            }

        @Test
        fun `GIVEN the nonce read ahead hangs and no approvals are owed WHEN collected THEN Ready without waiting for it`() =
            runTest {
                // Arrange
                coEvery { getWalletStatus(ADDRESSES) } coAnswers {
                    delay(STATUS_LATENCY_MILLIS)
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right()
                }
                coEvery { getRelayerNonce(ADDRESSES) } coAnswers { awaitCancellation() }

                // Act
                val actual = useCase(USER_WALLET_ID).toList().last()

                // Assert
                assertThat(actual).isEqualTo(PolymarketOnboardingProgress.Ready(isNewAccount = false))
                assertThat(currentTime).isLessThan(IN_SESSION_TIMEOUT_MILLIS)
                coVerify(exactly = 1) { getRelayerNonce(ADDRESSES) }
            }

        @Test
        fun `GIVEN previous approvals failed and credentials are stored WHEN collected THEN re-signs and resubmits the batch`() =
            runTest {
                // Arrange
                coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
                coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                    walletState(PolymarketWalletStatus.APPROVALS_FAILED).right(),
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
                )

                // Act
                useCase(USER_WALLET_ID).test { cancelAndConsumeRemainingEvents() }

                // Assert
                assertThat(session.signedPayloads.single().single())
                    .isInstanceOf(PolymarketTypedPayload.RelayedBatch::class.java)
                coVerify(exactly = 1) { submitApprovals(ADDRESSES, any()) }
            }

        @Test
        fun `GIVEN the backend reports another deposit wallet WHEN collected THEN nothing is signed`() = runTest {
            // Arrange
            val mismatch = PolymarketOnboardingError.AddressMismatch(expected = DEPOSIT_WALLET, actual = OWNER)
            coEvery { getWalletStatus(ADDRESSES) } returns mismatch.left()

            // Act
            val progress = useCase(USER_WALLET_ID).toList()

            // Assert
            assertThat(progress).containsExactly(
                PolymarketOnboardingProgress.AwaitingSignature,
                PolymarketOnboardingProgress.Failed(error = mismatch, isRetryable = false),
            ).inOrder()
            assertThat(session.signedPayloads).isEmpty()
            coVerify(exactly = 0) { getRelayerNonce(any()) }
        }

        @Test
        fun `GIVEN nothing is owed and credentials are stored WHEN collected THEN the session signs nothing`() =
            runTest {
                // Arrange
                coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS
                coEvery { getWalletStatus(ADDRESSES) } returns
                    walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

                // Act
                useCase(USER_WALLET_ID).test { cancelAndConsumeRemainingEvents() }

                // Assert
                assertThat(session.runs).isEqualTo(1)
                assertThat(session.signedPayloads).isEmpty()
            }
    }

    @ParameterizedTest
    @MethodSource("stateMachine")
    fun `GIVEN a backend status and stored credentials WHEN collected THEN signs and settles exactly what is owed`(
        row: StateMachineRow,
    ) = runTest {
        assertSettles(row)
    }

    @ParameterizedTest
    @MethodSource("pinnedRows")
    fun `GIVEN a pinned backend status WHEN collected THEN signs and settles exactly the pinned steps`(
        row: StateMachineRow,
    ) = runTest {
        assertSettles(row)
    }

    private suspend fun assertSettles(row: StateMachineRow) {
        // Arrange
        coEvery { getApiCredentials(USER_WALLET_ID) } returns CREDENTIALS.takeIf { row.hasCredentials }
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(row.status).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )
        coEvery { submitApprovals(ADDRESSES, any()) } returns PolymarketWalletStatus.APPROVALS_IN_PROGRESS.right()

        // Act
        val last = useCase(USER_WALLET_ID).toList().last()

        // Assert
        assertThat(last).isEqualTo(
            PolymarketOnboardingProgress.Ready(isNewAccount = row.status != PolymarketWalletStatus.READY_TO_TRADE),
        )
        if (row.signed.isEmpty()) {
            assertThat(session.signedPayloads).isEmpty()
        } else {
            assertThat(session.signedPayloads).hasSize(1)
            assertThat(session.signedPayloads.single().map { it::class }).containsExactlyElementsIn(row.signed).inOrder()
        }
        if (row.readsNonce || row.hasCredentials) {
            coVerify(exactly = if (row.readsNonce) 1 else 0) { getRelayerNonce(ADDRESSES) }
        }
        coVerify(exactly = if (row.deploys) 1 else 0) { deployDepositWallet(ADDRESSES) }
        coVerify(exactly = if (row.hasCredentials) 0 else 1) { deriveApiCredentials(any(), any(), any(), any()) }
        coVerify(exactly = if (row.submitsApprovals) 1 else 0) { submitApprovals(ADDRESSES, any()) }
    }

    data class StateMachineRow(
        val status: PolymarketWalletStatus,
        val hasCredentials: Boolean,
        val signed: List<KClass<out PolymarketTypedPayload>>,
        val readsNonce: Boolean,
        val deploys: Boolean,
        val submitsApprovals: Boolean,
    )

    private fun stateMachine(): List<StateMachineRow> = PolymarketWalletStatus.entries.flatMap { status ->
        listOf(false, true).map { hasCredentials ->
            val owes = status != PolymarketWalletStatus.READY_TO_TRADE &&
                status != PolymarketWalletStatus.APPROVALS_IN_PROGRESS
            StateMachineRow(
                status = status,
                hasCredentials = hasCredentials,
                signed = listOfNotNull(
                    PolymarketTypedPayload.ClobAuth::class.takeIf { !hasCredentials },
                    PolymarketTypedPayload.RelayedBatch::class.takeIf { owes },
                ),
                readsNonce = owes,
                deploys = status == PolymarketWalletStatus.NOT_CREATED ||
                    status == PolymarketWalletStatus.DEPLOYMENT_FAILED,
                submitsApprovals = owes,
            )
        }
    }

    private fun pinnedRows(): List<StateMachineRow> = listOf(
        StateMachineRow(
            status = PolymarketWalletStatus.DEPLOYMENT_FAILED,
            hasCredentials = false,
            signed = listOf(PolymarketTypedPayload.ClobAuth::class, PolymarketTypedPayload.RelayedBatch::class),
            readsNonce = true,
            deploys = true,
            submitsApprovals = true,
        ),
        StateMachineRow(
            status = PolymarketWalletStatus.NOT_CREATED,
            hasCredentials = false,
            signed = listOf(PolymarketTypedPayload.ClobAuth::class, PolymarketTypedPayload.RelayedBatch::class),
            readsNonce = true,
            deploys = true,
            submitsApprovals = true,
        ),
        StateMachineRow(
            status = PolymarketWalletStatus.READY_TO_TRADE,
            hasCredentials = false,
            signed = listOf(PolymarketTypedPayload.ClobAuth::class),
            readsNonce = false,
            deploys = false,
            submitsApprovals = false,
        ),
        StateMachineRow(
            status = PolymarketWalletStatus.READY_TO_TRADE,
            hasCredentials = true,
            signed = emptyList(),
            readsNonce = false,
            deploys = false,
            submitsApprovals = false,
        ),
        StateMachineRow(
            status = PolymarketWalletStatus.APPROVALS_IN_PROGRESS,
            hasCredentials = false,
            signed = listOf(PolymarketTypedPayload.ClobAuth::class),
            readsNonce = false,
            deploys = false,
            submitsApprovals = false,
        ),
        StateMachineRow(
            status = PolymarketWalletStatus.APPROVALS_FAILED,
            hasCredentials = true,
            signed = listOf(PolymarketTypedPayload.RelayedBatch::class),
            readsNonce = true,
            deploys = false,
            submitsApprovals = true,
        ),
    )
}