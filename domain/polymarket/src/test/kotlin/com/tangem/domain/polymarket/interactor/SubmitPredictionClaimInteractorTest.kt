package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.core.error.DataError
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketRedeemRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.time.Duration.Companion.minutes

internal class SubmitPredictionClaimInteractorTest {

    private val redeemRepository: PolymarketRedeemRepository = mockk()
    private val polymarketRepository: PolymarketRepository = mockk()
    private val signer: PolymarketTypedDataSigner = mockk()
    private var nowMillis = NOW_MILLIS

    private val interactor = SubmitPredictionClaimInteractor(
        redeemRepository = redeemRepository,
        polymarketRepository = polymarketRepository,
        signer = signer,
        currentTimeMillis = { nowMillis },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(redeemRepository, polymarketRepository, signer)
        nowMillis = NOW_MILLIS
        coEvery { redeemRepository.getCall(any(), any(), any()) } returns CALL.right()
        coEvery { polymarketRepository.getRelayerNonce(any()) } returns BigInteger.valueOf(9).right()
        coEvery { signer.signRelayedBatch(any(), any()) } returns SIGNATURE.right()
        coEvery { redeemRepository.submit(any(), any()) } returns receipt(PredictionClaimState.CONFIRMED).right()
    }

    @Test
    fun `GIVEN the submit answers CONFIRMED WHEN claimed THEN the receipt is returned without polling`() =
        runTest {
            // Arrange
            val params = params()

            // Act
            val actual = interactor.claim(params)

            // Assert
            assertThat(actual).isEqualTo(receipt(PredictionClaimState.CONFIRMED).right())
            coVerify(exactly = 0) { redeemRepository.getState(any(), any()) }
        }

    @Test
    fun `GIVEN the batch WHEN signed THEN it carries exactly the call the backend returned`() = runTest {
        // Arrange
        val params = params()

        // Act
        interactor.claim(params)

        // Assert
        coVerify {
            signer.signRelayedBatch(
                USER_WALLET_ID,
                match { it.calls == listOf(CALL) && it.depositWalletAddress == DEPOSIT_WALLET },
            )
        }
    }

    @Test
    fun `GIVEN the call lookup fails WHEN claimed THEN nothing is signed and CallUnavailable is raised`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.getCall(any(), any(), any()) } returns
                PolymarketRedeemError.NotFound.left()

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(PredictionClaimFailure.CallUnavailable.left())
            coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
        }

    @Test
    fun `GIVEN the nonce is unavailable WHEN claimed THEN nothing is signed and NonceUnavailable is raised`() =
        runTest {
            // Arrange
            coEvery { polymarketRepository.getRelayerNonce(any()) } returns
                DataError.NetworkError.NoInternetConnection.left()

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(PredictionClaimFailure.NonceUnavailable.left())
            coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
        }

    @Test
    fun `GIVEN the user cancels the signature WHEN claimed THEN nothing is submitted and the cause survives`() =
        runTest {
            // Arrange
            coEvery { signer.signRelayedBatch(any(), any()) } returns PolymarketSigningError.UserCancelled.left()

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual)
                .isEqualTo(PredictionClaimFailure.SigningFailed(PolymarketSigningError.UserCancelled).left())
            coVerify(exactly = 0) { redeemRepository.submit(any(), any()) }
        }

    @Test
    fun `GIVEN the signature takes longer than the deadline window WHEN claimed THEN nothing is submitted and Expired is raised`() =
        runTest {
            // Arrange
            coEvery { signer.signRelayedBatch(any(), any()) } coAnswers {
                nowMillis += 11.minutes.inWholeMilliseconds
                SIGNATURE.right()
            }

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(PredictionClaimFailure.Expired.left())
            coVerify(exactly = 0) { redeemRepository.submit(any(), any()) }
        }

    @Test
    fun `GIVEN the relayer refuses WHEN claimed THEN the rejection is raised with its cause`() = runTest {
        // Arrange
        val rejection = PolymarketRedeemError.RelayerRejected(detail = "position is not redeemable")
        coEvery { redeemRepository.submit(any(), any()) } returns rejection.left()

        // Act
        val actual = interactor.claim(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionClaimFailure.Rejected(rejection).left())
        coVerify(exactly = 1) { redeemRepository.submit(any(), any()) }
    }

    @Test
    fun `GIVEN the submit loses the connection WHEN it is sent again THEN the second attempt settles it`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.submit(any(), any()) } returnsMany listOf(
                PolymarketRedeemError.Network.left(),
                receipt(PredictionClaimState.CONFIRMED).right(),
            )

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(receipt(PredictionClaimState.CONFIRMED).right())
            coVerify(exactly = 2) { redeemRepository.submit(any(), any()) }
        }

    @Test
    fun `GIVEN the connection fails twice WHEN claimed THEN it is not sent a third time`() = runTest {
        // Arrange
        coEvery { redeemRepository.submit(any(), any()) } returns PolymarketRedeemError.Network.left()

        // Act
        val actual = interactor.claim(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionClaimFailure.Rejected(PolymarketRedeemError.Network).left())
        coVerify(exactly = 2) { redeemRepository.submit(any(), any()) }
    }

    @Test
    fun `GIVEN the submit answers IN_PROGRESS WHEN polled to CONFIRMED THEN the settled receipt is returned`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.submit(any(), any()) } returns
                receipt(PredictionClaimState.IN_PROGRESS).right()
            coEvery { redeemRepository.getState(any(), any()) } returnsMany listOf(
                receipt(PredictionClaimState.IN_PROGRESS).right(),
                receipt(PredictionClaimState.CONFIRMED).right(),
            )

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(receipt(PredictionClaimState.CONFIRMED).right())
        }

    @Test
    fun `GIVEN the claim lands as FAILED WHEN polled THEN Failed carries the receipt`() = runTest {
        // Arrange
        coEvery { redeemRepository.submit(any(), any()) } returns receipt(PredictionClaimState.IN_PROGRESS).right()
        coEvery { redeemRepository.getState(any(), any()) } returns receipt(PredictionClaimState.FAILED).right()

        // Act
        val actual = interactor.claim(params())

        // Assert
        assertThat(actual)
            .isEqualTo(PredictionClaimFailure.Failed(receipt(PredictionClaimState.FAILED)).left())
    }

    @Test
    fun `GIVEN polling never settles WHEN the attempts run out THEN Unconfirmed carries the last receipt`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.submit(any(), any()) } returns
                receipt(PredictionClaimState.IN_PROGRESS).right()
            coEvery { redeemRepository.getState(any(), any()) } returns
                receipt(PredictionClaimState.IN_PROGRESS).right()

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual)
                .isEqualTo(PredictionClaimFailure.Unconfirmed(receipt(PredictionClaimState.IN_PROGRESS)).left())
            coVerify(exactly = 20) { redeemRepository.getState(WALLET_ID, TX_ID) }
        }

    @Test
    fun `GIVEN the poll answers that the claim is unknown WHEN it is read THEN the polling stops and the claim is Unconfirmed`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.submit(any(), any()) } returns
                receipt(PredictionClaimState.IN_PROGRESS).right()
            coEvery { redeemRepository.getState(any(), any()) } returns PolymarketRedeemError.NotFound.left()

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual)
                .isEqualTo(PredictionClaimFailure.Unconfirmed(receipt(PredictionClaimState.IN_PROGRESS)).left())
            coVerify(exactly = 1) { redeemRepository.getState(any(), any()) }
        }

    @Test
    fun `GIVEN the poll is unauthorized WHEN it is read THEN the polling stops and the claim is Unconfirmed`() = runTest {
        // Arrange
        coEvery { redeemRepository.submit(any(), any()) } returns receipt(PredictionClaimState.IN_PROGRESS).right()
        coEvery { redeemRepository.getState(any(), any()) } returns PolymarketRedeemError.Unauthorized.left()

        // Act
        val actual = interactor.claim(params())

        // Assert
        assertThat(actual)
            .isEqualTo(PredictionClaimFailure.Unconfirmed(receipt(PredictionClaimState.IN_PROGRESS)).left())
        coVerify(exactly = 1) { redeemRepository.getState(any(), any()) }
    }

    @Test
    fun `GIVEN a poll read fails WHEN the next one settles THEN the failure does not end the polling`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.submit(any(), any()) } returns
                receipt(PredictionClaimState.IN_PROGRESS).right()
            coEvery { redeemRepository.getState(any(), any()) } returnsMany listOf(
                PolymarketRedeemError.Network.left(),
                receipt(PredictionClaimState.CONFIRMED).right(),
            )

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(receipt(PredictionClaimState.CONFIRMED).right())
        }

    private fun params() = SubmitPredictionClaimInteractor.Params(
        userWalletId = USER_WALLET_ID,
        walletId = WALLET_ID,
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        conditionId = CONDITION_ID,
        isNegRisk = true,
    )

    private fun receipt(state: PredictionClaimState) = PredictionClaimReceipt(
        transactionId = TX_ID,
        state = state,
        transactionHash = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val WALLET_ID = "ab"
        const val OWNER = "0xabc0000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xdef0000000000000000000000000000000000002"
        const val CONDITION_ID = "0x94947a23"
        const val TX_ID = "0x4f2c"
        const val SIGNATURE = "0xsignature"
        const val NOW_MILLIS = 1_787_346_200_000L
        val CALL = PolymarketApprovalCall(
            target = "0xada2005600dec949baf300f4c6120000bdb6eaab",
            value = "0",
            data = "0x01b7037c",
        )
    }
}