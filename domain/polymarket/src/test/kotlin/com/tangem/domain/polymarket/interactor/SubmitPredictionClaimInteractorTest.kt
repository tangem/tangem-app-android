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
import io.mockk.every
import com.tangem.domain.polymarket.redeem.PredictionRedeemCallValidator
import com.tangem.domain.polymarket.model.PolymarketPositionId
import com.tangem.domain.polymarket.model.PolymarketPosition
import com.tangem.domain.polymarket.PolymarketPositionsRepository
import arrow.core.nonEmptyListOf
import io.mockk.slot
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionRedeemCalls
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
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
    private val positionsRepository: PolymarketPositionsRepository = mockk()
    private val signer: PolymarketTypedDataSigner = mockk()
    private var nowMillis = NOW_MILLIS

    private val interactor = SubmitPredictionClaimInteractor(
        redeemRepository = redeemRepository,
        polymarketRepository = polymarketRepository,
        positionsRepository = positionsRepository,
        signer = signer,
        currentTimeMillis = { nowMillis },
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(redeemRepository, polymarketRepository, positionsRepository, signer)
        nowMillis = NOW_MILLIS
        coEvery { redeemRepository.getCalls(any(), any(), any()) } returns CALLS.right()
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
    fun `GIVEN two redeem calls WHEN signed and submitted THEN both travel verbatim and in order`() = runTest {
        // Arrange
        val batch = slot<PredictionClaimBatch>()
        coEvery { redeemRepository.getCalls(any(), any(), any()) } returns
            PredictionRedeemCalls(version = PolymarketMarketVersion.V2, calls = nonEmptyListOf(V2_CALL_NO, V2_CALL_YES)).right()
        coEvery { redeemRepository.submit(any(), capture(batch)) } returns
            receipt(PredictionClaimState.CONFIRMED).right()

        // Act
        interactor.claim(params(version = PolymarketMarketVersion.V2))

        // Assert
        coVerify {
            signer.signRelayedBatch(
                USER_WALLET_ID,
                match { it.calls.toList() == listOf(V2_CALL_NO, V2_CALL_YES) && it.depositWalletAddress == DEPOSIT_WALLET },
            )
        }
        assertThat(batch.captured.calls).containsExactly(V2_CALL_NO, V2_CALL_YES).inOrder()
    }

    @Test
    fun `GIVEN served calls that do not redeem the position WHEN claimed THEN nothing is signed and they are refused`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.getCalls(any(), any(), any()) } returns
                PredictionRedeemCalls(version = PolymarketMarketVersion.V1, calls = nonEmptyListOf(V2_CALL_YES)).right()

            // Act
            val actual = interactor.claim(params())

            // Assert
            assertThat(actual).isEqualTo(
                PredictionClaimFailure.CallRejected(PredictionRedeemCallValidator.Rejection.Target(0)).left(),
            )
            coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
        }

    @Test
    fun `GIVEN a position without a version WHEN claimed THEN it is read again and the fresh version decides`() =
        runTest {
            // Arrange
            coEvery { positionsRepository.getPosition(any(), any()) } returns
                mockk<PolymarketPosition.Open> { every { version } returns PolymarketMarketVersion.V1 }.right()

            // Act
            val actual = interactor.claim(params(version = null))

            // Assert
            assertThat(actual).isEqualTo(receipt(PredictionClaimState.CONFIRMED).right())
            coVerify(exactly = 1) {
                positionsRepository.getPosition(
                    addresses = match { it.ownerAddress == OWNER && it.depositWalletAddress == DEPOSIT_WALLET },
                    id = PolymarketPositionId(conditionId = CONDITION_ID, assetId = ASSET_ID),
                )
            }
        }

    @Test
    fun `GIVEN a position still without a version WHEN claimed THEN nothing is signed`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPosition(any(), any()) } returns
            mockk<PolymarketPosition.Open> { every { version } returns null }.right()

        // Act
        val actual = interactor.claim(params(version = null))

        // Assert
        assertThat(actual).isEqualTo(
            PredictionClaimFailure.VersionUnavailable(detail = "Position has no market version").left(),
        )
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
    }

    @Test
    fun `GIVEN a version-less position and nothing to redeem WHEN claimed THEN the lookup answer wins`() = runTest {
        // Arrange
        coEvery { redeemRepository.getCalls(any(), any(), any()) } returns
            PolymarketRedeemError.NothingToRedeem(detail = "redeemed").left()

        // Act
        val actual = interactor.claim(params(version = null))

        // Assert
        assertThat(actual).isEqualTo(PredictionClaimFailure.NothingToRedeem(detail = "redeemed").left())
        coVerify(exactly = 0) { positionsRepository.getPosition(any(), any()) }
    }

    @Test
    fun `GIVEN a stale position version WHEN the lookup serves another THEN the position is read again and decides`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.getCalls(any(), any(), any()) } returns
                PredictionRedeemCalls(version = PolymarketMarketVersion.V2, calls = nonEmptyListOf(V2_CALL_YES)).right()
            coEvery { positionsRepository.getPosition(any(), any()) } returns
                mockk<PolymarketPosition.Open> { every { version } returns PolymarketMarketVersion.V2 }.right()

            // Act
            val actual = interactor.claim(params(version = PolymarketMarketVersion.V1))

            // Assert
            assertThat(actual).isEqualTo(receipt(PredictionClaimState.CONFIRMED).right())
            coVerify(exactly = 1) { positionsRepository.getPosition(any(), any()) }
        }

    @Test
    fun `GIVEN an unknown market version WHEN claimed THEN the calls are refused before signing`() = runTest {
        // Arrange
        coEvery { positionsRepository.getPosition(any(), any()) } returns
            mockk<PolymarketPosition.Open> { every { version } returns PolymarketMarketVersion.UNKNOWN }.right()

        // Act
        val actual = interactor.claim(params(version = PolymarketMarketVersion.UNKNOWN))

        // Assert
        assertThat(actual).isEqualTo(
            PredictionClaimFailure.CallRejected(PredictionRedeemCallValidator.Rejection.UnsupportedVersion).left(),
        )
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
    }

    @Test
    fun `GIVEN nothing to redeem WHEN claimed THEN nothing is signed and NothingToRedeem is raised`() = runTest {
        // Arrange
        coEvery { redeemRepository.getCalls(any(), any(), any()) } returns
            PolymarketRedeemError.NothingToRedeem(detail = "already redeemed").left()

        // Act
        val actual = interactor.claim(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionClaimFailure.NothingToRedeem(detail = "already redeemed").left())
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
    }

    @Test
    fun `GIVEN the call lookup fails WHEN claimed THEN nothing is signed and CallUnavailable is raised`() =
        runTest {
            // Arrange
            coEvery { redeemRepository.getCalls(any(), any(), any()) } returns
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

    private fun params(version: PolymarketMarketVersion? = PolymarketMarketVersion.V1) =
        SubmitPredictionClaimInteractor.Params(
            userWalletId = USER_WALLET_ID,
            walletId = WALLET_ID,
            ownerAddress = OWNER,
            depositWalletAddress = DEPOSIT_WALLET,
            conditionId = CONDITION_ID,
            assetId = ASSET_ID,
            isNegRisk = true,
            version = version,
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
        const val CONDITION_ID = "0x94947a23d1f1f6e0dfb6f2b2c4d1a4e9c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5"
        const val ASSET_ID = "21742633143463906290569050155826241533067272736897614950488156847949938836455"
        const val TX_ID = "0x4f2c"
        const val SIGNATURE = "0xsignature"
        const val NOW_MILLIS = 1_787_346_200_000L
        const val ASSET_POSITION_WORD = "3011e4ede0f6befa0ad3f571001d3e1ffeef3d4af78c3112aaac90416e3a4300"
        val CALL = PolymarketApprovalCall(
            target = "0xada2005600dec949baf300f4c6120000bdb6eaab",
            value = "0",
            data = "0x01b7037c" +
                word("c011a7e12a19f7b1f670d46f03b03f3342e82dfb") +
                word("0") +
                word(CONDITION_ID) +
                word("80") +
                word("2") +
                word("1") +
                word("2"),
        )
        val V2_CALL_NO = v2Call(outcome = 0)
        val V2_CALL_YES = v2Call(outcome = 1)
        val CALLS = PredictionRedeemCalls(version = PolymarketMarketVersion.V1, calls = nonEmptyListOf(CALL))

        fun word(hex: String) = hex.removePrefix("0x").padStart(64, '0')

        fun v2Call(outcome: Int) = PolymarketApprovalCall(
            target = "0x12121212006e4cd160d18e3f00711da5c3372600",
            value = "0",
            data = "0xd217a3cc" + ASSET_POSITION_WORD + word(outcome.toString()) + word("4c4b40"),
        )
    }
}