package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketApprovalsBatch
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketSignedOnboarding
import com.tangem.domain.polymarket.model.PolymarketWalletError
import com.tangem.domain.polymarket.model.PolymarketWalletStatus
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class SubmitApprovalsUseCaseTest {

    private val polymarketRepository: PolymarketRepository = mockk()

    private val useCase = SubmitApprovalsUseCase(polymarketRepository = polymarketRepository)

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketRepository)
    }

    @Test
    fun `GIVEN a signed onboarding WHEN invoke THEN submits exactly the signed fields`() = runTest {
        // Arrange
        val batch = slot<PolymarketApprovalsBatch>()
        coEvery { polymarketRepository.submitApprovals(capture(batch)) } returns
            PolymarketWalletStatus.APPROVALS_IN_PROGRESS.right()

        // Act
        val actual = useCase(addresses = ADDRESSES, signed = SIGNED)

        // Assert
        assertThat(actual).isEqualTo(PolymarketWalletStatus.APPROVALS_IN_PROGRESS.right())
        assertThat(batch.captured).isEqualTo(
            PolymarketApprovalsBatch(
                ownerAddress = OWNER,
                depositWalletAddress = SIGNED_DEPOSIT_WALLET,
                nonce = NONCE,
                deadline = DEADLINE,
                calls = SIGNED.approvals.calls,
                signature = BATCH_SIGNATURE,
            ),
        )
        assertThat(batch.captured.calls).isSameInstanceAs(SIGNED.approvals.calls)
    }

    @Test
    fun `GIVEN the relayer rejects the batch WHEN invoke THEN wraps the wallet error`() = runTest {
        // Arrange
        coEvery { polymarketRepository.submitApprovals(any()) } returns
            PolymarketWalletError.RelayerRejected.NonceReused.left()

        // Act
        val actual = useCase(addresses = ADDRESSES, signed = SIGNED)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketOnboardingError.Wallet(PolymarketWalletError.RelayerRejected.NonceReused).left(),
        )
    }

    @Test
    fun `GIVEN there is no connection WHEN invoke THEN returns Network`() = runTest {
        // Arrange
        coEvery { polymarketRepository.submitApprovals(any()) } returns PolymarketWalletError.Network.left()

        // Act
        val actual = useCase(addresses = ADDRESSES, signed = SIGNED)

        // Assert
        assertThat(actual).isEqualTo(PolymarketOnboardingError.Network.left())
    }

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val SIGNED_DEPOSIT_WALLET = "0xC0FFEE0000000000000000000000000000C0FFEE"
        const val NONCE = "7"
        const val DEADLINE = "1735690200"
        const val BATCH_SIGNATURE = "0xbb"

        val ADDRESSES = PolymarketAddresses(
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            userWalletId = UserWalletId("011"),
        )

        val SIGNED = PolymarketSignedOnboarding(
            l1Signature = "0xaa",
            clobAuthTimestamp = "1735689600",
            batchSignature = BATCH_SIGNATURE,
            approvals = PolymarketApprovalsPayload(
                depositWalletAddress = SIGNED_DEPOSIT_WALLET,
                nonce = NONCE,
                deadline = DEADLINE,
                calls = PolymarketApprovalCalls.build(),
            ),
        )
    }
}