package com.tangem.domain.polymarket.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger

internal class RunPolymarketOnboardingUseCaseTest {

    private val deriveAddresses: DerivePolymarketAddressesUseCase = mockk()
    private val getWalletStatus: GetPolymarketWalletStatusUseCase = mockk()
    private val getRelayerNonce: GetPolymarketRelayerNonceUseCase = mockk()
    private val signOnboardingDigests: SignOnboardingDigestsUseCase = mockk()
    private val deployDepositWallet: DeployDepositWalletUseCase = mockk()
    private val getApiCredentials: GetPolymarketApiCredentialsUseCase = mockk()
    private val deriveApiCredentials: DeriveApiCredentialsUseCase = mockk()
    private val submitApprovals: SubmitApprovalsUseCase = mockk()

    private val useCase = RunPolymarketOnboardingUseCase(
        deriveAddresses = deriveAddresses,
        getWalletStatus = getWalletStatus,
        getRelayerNonce = getRelayerNonce,
        signOnboardingDigests = signOnboardingDigests,
        deployDepositWallet = deployDepositWallet,
        getApiCredentials = getApiCredentials,
        deriveApiCredentials = deriveApiCredentials,
        submitApprovals = submitApprovals,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            deriveAddresses,
            getWalletStatus,
            getRelayerNonce,
            signOnboardingDigests,
            deployDepositWallet,
            getApiCredentials,
            deriveApiCredentials,
            submitApprovals,
        )
        coEvery { deriveAddresses(USER_WALLET_ID) } returns ADDRESSES.right()
        coEvery { getRelayerNonce(ADDRESSES) } returns NONCE.right()
        coEvery { signOnboardingDigests(ADDRESSES, NONCE) } returns SIGNED.right()
        coEvery { deployDepositWallet(ADDRESSES) } returns
            PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS.right()
        coEvery { submitApprovals(ADDRESSES, SIGNED) } returns
            PolymarketWalletStatus.APPROVALS_IN_PROGRESS.right()
        coEvery { deriveApiCredentials(OWNER, L1_SIGNATURE, TIMESTAMP) } returns CREDENTIALS.right()
        coEvery { getApiCredentials(OWNER) } returns null
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
            awaitComplete()
        }
        coVerify(exactly = 1) { signOnboardingDigests(ADDRESSES, NONCE) }
        coVerify(exactly = 1) { deployDepositWallet(ADDRESSES) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED) }
        coVerify(exactly = 1) { deriveApiCredentials(OWNER, L1_SIGNATURE, TIMESTAMP) }
        coVerifyOrder {
            deployDepositWallet(ADDRESSES)
            deriveApiCredentials(OWNER, L1_SIGNATURE, TIMESTAMP)
        }
    }

    @Test
    fun `GIVEN the status never settles WHEN collected THEN ends with StillWorking`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returns
            walletState(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS).right()

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.StillWorking(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            awaitComplete()
        }
        coVerify(exactly = 0) { submitApprovals(any(), any()) }
    }

    @Test
    fun `GIVEN the wallet is onboarded and credentials are stored WHEN collected THEN Ready without a tap`() =
        runTest {
            // Arrange
            coEvery { getApiCredentials(OWNER) } returns CREDENTIALS
            coEvery { getWalletStatus(ADDRESSES) } returns
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right()

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
                awaitComplete()
            }
            coVerify(exactly = 0) { signOnboardingDigests(any(), any()) }
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
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
                awaitComplete()
            }
            coVerify(exactly = 1) { signOnboardingDigests(ADDRESSES, NONCE) }
            coVerify(exactly = 1) { deriveApiCredentials(OWNER, L1_SIGNATURE, TIMESTAMP) }
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED) }
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
        coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED) }
    }

    @Test
    fun `GIVEN the wallet is deployed and credentials are stored WHEN collected THEN does not re-derive credentials`() =
        runTest {
            // Arrange
            coEvery { getApiCredentials(OWNER) } returns CREDENTIALS
            coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
                walletState(PolymarketWalletStatus.DEPLOYED).right(),
                walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
            )

            // Act & Assert
            useCase(USER_WALLET_ID).test {
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                )
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
                awaitComplete()
            }
            coVerify(exactly = 0) { deriveApiCredentials(any(), any(), any()) }
            coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED) }
        }

    @Test
    fun `GIVEN approvals are in flight and credentials are stored WHEN collected THEN only waits`() = runTest {
        // Arrange
        coEvery { getApiCredentials(OWNER) } returns CREDENTIALS
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.APPROVALS_IN_PROGRESS).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
            awaitComplete()
        }
        coVerify(exactly = 0) { signOnboardingDigests(any(), any()) }
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
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                )
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
                awaitComplete()
            }
            coVerify(exactly = 1) { signOnboardingDigests(ADDRESSES, NONCE) }
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
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
                assertThat(awaitItem()).isEqualTo(
                    PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
                )
                assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
                awaitComplete()
            }
            coVerify(exactly = 1) { signOnboardingDigests(ADDRESSES, NONCE) }
            coVerify(exactly = 1) { submitApprovals(ADDRESSES, SIGNED) }
            coVerify(exactly = 0) { deployDepositWallet(any()) }
        }

    @Test
    fun `GIVEN an unrecognised status WHEN collected THEN waits instead of acting`() = runTest {
        // Arrange
        coEvery { getWalletStatus(ADDRESSES) } returnsMany listOf(
            walletState(PolymarketWalletStatus.UNKNOWN).right(),
            walletState(PolymarketWalletStatus.DEPLOYED).right(),
            walletState(PolymarketWalletStatus.READY_TO_TRADE).right(),
        )

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.UNKNOWN),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
            awaitComplete()
        }
        coVerify(exactly = 0) { deployDepositWallet(any()) }
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.DEPLOYMENT_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.AwaitingSignature)
            assertThat(awaitItem()).isEqualTo(
                PolymarketOnboardingProgress.Working(PolymarketWalletStatus.APPROVALS_IN_PROGRESS),
            )
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Ready)
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
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
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
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
        coEvery { signOnboardingDigests(ADDRESSES, NONCE) } returns
            PolymarketOnboardingError.Signing(PolymarketSigningError.UserCancelled).left()

        // Act & Assert
        useCase(USER_WALLET_ID).test {
            assertThat(awaitItem()).isEqualTo(PolymarketOnboardingProgress.Deriving)
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

    private fun walletState(status: PolymarketWalletStatus) = PolymarketWalletState(
        depositWalletAddress = DEPOSIT_WALLET,
        status = status,
    )

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val L1_SIGNATURE = "0xaa"
        const val TIMESTAMP = "1735689600"

        val USER_WALLET_ID = UserWalletId("011")
        val NONCE: BigInteger = BigInteger.valueOf(7)

        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = USER_WALLET_ID,
        )

        val SIGNED = PolymarketSignedOnboarding(
            l1Signature = L1_SIGNATURE,
            clobAuthTimestamp = TIMESTAMP,
            batchSignature = "0xbb",
            approvals = PolymarketApprovalsPayload(
                depositWalletAddress = DEPOSIT_WALLET,
                nonce = "7",
                deadline = "1735690200",
                calls = PolymarketApprovalCalls.build(),
            ),
        )

        val CREDENTIALS = PolymarketApiCredentials(apiKey = "key", secret = "secret", passphrase = "pass")
    }
}