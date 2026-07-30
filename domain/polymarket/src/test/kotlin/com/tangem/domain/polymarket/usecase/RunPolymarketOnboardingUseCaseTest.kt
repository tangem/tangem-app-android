package com.tangem.domain.polymarket.usecase

import app.cash.turbine.test
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApiCredentials
import com.tangem.domain.polymarket.model.PolymarketOnboardingProgress
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketWalletState
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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