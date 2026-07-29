package com.tangem.domain.polymarket.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.approval.PolymarketApprovalCalls
import com.tangem.domain.polymarket.model.PolymarketAddresses
import com.tangem.domain.polymarket.model.PolymarketOnboardingError
import com.tangem.domain.polymarket.model.PolymarketOnboardingSignatures
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.signing.PolymarketApprovalsPayload
import com.tangem.domain.polymarket.signing.PolymarketClobAuthData
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigInteger

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SignOnboardingDigestsUseCaseTest {

    private val signer: PolymarketTypedDataSigner = mockk()

    private val useCase = SignOnboardingDigestsUseCase(signer = signer)

    private val addresses = PolymarketAddresses(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        userWalletId = UserWalletId("011"),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(signer)
    }

    @Test
    fun `GIVEN a relayer nonce WHEN invoke THEN signs a batch built from the derived address`() = runTest {
        // Arrange
        val approvals = slot<PolymarketApprovalsPayload>()
        coEvery { signer.signOnboarding(addresses.userWalletId, any(), capture(approvals)) } returns
            PolymarketOnboardingSignatures(l1Signature = L1_SIGNATURE, batchSignature = BATCH_SIGNATURE).right()

        // Act
        val result = useCase(addresses = addresses, relayerNonce = BigInteger.TEN)

        // Assert
        assertThat(approvals.captured.depositWalletAddress).isEqualTo(DEPOSIT_WALLET)
        assertThat(approvals.captured.nonce).isEqualTo("10")
        assertThat(approvals.captured.calls).isEqualTo(PolymarketApprovalCalls.build())
        assertThat(result.getOrNull()?.approvals).isSameInstanceAs(approvals.captured)
    }

    @Test
    fun `GIVEN a signed payload WHEN invoke THEN returns the timestamp that was signed`() = runTest {
        // Arrange
        val clobAuth = slot<PolymarketClobAuthData>()
        coEvery { signer.signOnboarding(addresses.userWalletId, capture(clobAuth), any()) } returns
            PolymarketOnboardingSignatures(l1Signature = L1_SIGNATURE, batchSignature = BATCH_SIGNATURE).right()

        // Act
        val result = useCase(addresses = addresses, relayerNonce = BigInteger.ZERO)

        // Assert
        assertThat(result.getOrNull()?.clobAuthTimestamp).isEqualTo(clobAuth.captured.timestamp)
    }

    @Test
    fun `GIVEN a signed payload WHEN invoke THEN the deadline is ten minutes after the timestamp`() = runTest {
        // Arrange
        val clobAuth = slot<PolymarketClobAuthData>()
        val approvals = slot<PolymarketApprovalsPayload>()
        coEvery { signer.signOnboarding(addresses.userWalletId, capture(clobAuth), capture(approvals)) } returns
            PolymarketOnboardingSignatures(l1Signature = L1_SIGNATURE, batchSignature = BATCH_SIGNATURE).right()

        // Act
        useCase(addresses = addresses, relayerNonce = BigInteger.ZERO)

        // Assert
        val timestamp = clobAuth.captured.timestamp.toLong()
        val deadline = approvals.captured.deadline.toLong()
        assertThat(deadline - timestamp).isEqualTo(DEADLINE_OFFSET_SECONDS)
    }

    @Test
    fun `GIVEN both signatures WHEN invoke THEN maps them to the matching fields`() = runTest {
        // Arrange
        coEvery { signer.signOnboarding(any(), any(), any()) } returns
            PolymarketOnboardingSignatures(l1Signature = L1_SIGNATURE, batchSignature = BATCH_SIGNATURE).right()

        // Act
        val result = useCase(addresses = addresses, relayerNonce = BigInteger.ZERO)

        // Assert
        assertThat(result.getOrNull()?.l1Signature).isEqualTo(L1_SIGNATURE)
        assertThat(result.getOrNull()?.batchSignature).isEqualTo(BATCH_SIGNATURE)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN signing fails WHEN invoke THEN wraps the cause`(cause: PolymarketSigningError) = runTest {
        // Arrange
        coEvery { signer.signOnboarding(any(), any(), any()) } returns cause.left()

        // Act
        val result = useCase(addresses = addresses, relayerNonce = BigInteger.ZERO)

        // Assert
        assertThat(result).isEqualTo(PolymarketOnboardingError.Signing(cause).left())
    }

    private fun provideTestModels() = listOf(
        PolymarketSigningError.NotDerived,
        PolymarketSigningError.MissingWallet,
        PolymarketSigningError.UserCancelled,
        PolymarketSigningError.CardError,
        PolymarketSigningError.Unknown,
    )

    private companion object {
        const val OWNER = "0x1111111111111111111111111111111111111111"
        const val DEPOSIT_WALLET = "0xfAeA0f08159fcF2f573fE24E9E989B0d48f7651B"
        const val L1_SIGNATURE = "0xaa"
        const val BATCH_SIGNATURE = "0xbb"
        const val DEADLINE_OFFSET_SECONDS = 600L
    }
}