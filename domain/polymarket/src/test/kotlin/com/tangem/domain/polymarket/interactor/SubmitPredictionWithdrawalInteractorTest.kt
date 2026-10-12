package com.tangem.domain.polymarket.interactor

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.polymarket.PolymarketBridgeRepository
import com.tangem.domain.polymarket.PolymarketRepository
import com.tangem.domain.polymarket.PolymarketWithdrawRepository
import com.tangem.domain.polymarket.PredictionWithdrawalStore
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PolymarketSigningError
import com.tangem.domain.polymarket.model.PredictionTransferCall
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
import com.tangem.domain.polymarket.model.PredictionWithdrawError
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PredictionWithdrawalRecord
import com.tangem.domain.polymarket.model.PredictionWithdrawalState
import com.tangem.domain.polymarket.signing.PolymarketTypedDataSigner
import io.mockk.Ordering
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

internal class SubmitPredictionWithdrawalInteractorTest {

    private val bridgeRepository: PolymarketBridgeRepository = mockk()
    private val withdrawRepository: PolymarketWithdrawRepository = mockk()
    private val polymarketRepository: PolymarketRepository = mockk()
    private val signer: PolymarketTypedDataSigner = mockk()
    private val store: PredictionWithdrawalStore = mockk(relaxUnitFun = true)
    private val codec: PredictionTransferCallCodec = mockk()

    private val interactor = SubmitPredictionWithdrawalInteractor(
        bridgeRepository = bridgeRepository,
        withdrawRepository = withdrawRepository,
        polymarketRepository = polymarketRepository,
        signer = signer,
        store = store,
        codec = codec,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(bridgeRepository, withdrawRepository, polymarketRepository, signer, store, codec)
        coEvery { store.get(any()) } returns null
        coEvery { bridgeRepository.createWithdrawAddress(any(), any(), any(), any(), any()) } returns BRIDGE.right()
        coEvery { polymarketRepository.getRelayerNonce(any()) } returns BigInteger.valueOf(9).right()
        coEvery { signer.signRelayedBatch(any(), any()) } returns SIGNATURE.right()
        coEvery { withdrawRepository.submit(any(), any()) } returns receipt().right()
        every { codec.build(any(), any()) } returns batch().call
        every { codec.decode(any()) } returns PredictionTransferCall(
            recipient = BRIDGE,
            amountBaseUnits = BigInteger("12500000"),
        )
    }

    @Test
    fun `GIVEN a prepared withdrawal WHEN confirmed THEN the signed batch is stored before it is sent`() = runTest {
        // Arrange
        val prepared = interactor.prepare(params()).getOrNull()!!

        // Act
        val actual = interactor.confirm(prepared)

        // Assert
        assertThat(actual).isEqualTo(receipt().right())
        coVerify(ordering = Ordering.ORDERED) {
            store.save(USER_WALLET_ID, match { it.isSignedRecord(transactionId = null) })
            withdrawRepository.submit(WALLET_ID, any())
            store.save(USER_WALLET_ID, match { it.isSignedRecord(transactionId = TX_ID) })
        }
    }

    @Test
    fun `GIVEN the prepared deadline has passed WHEN confirmed THEN nothing is signed and Expired is raised`() =
        runTest {
            // Arrange
            val prepared = interactor.prepare(params()).getOrNull()!!.copy(deadline = "1")

            // Act
            val result = interactor.confirm(prepared)

            // Assert
            assertThat(result).isEqualTo(PredictionWithdrawalFailure.Expired.left())
            coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
            coVerify(exactly = 0) { store.save(any(), any()) }
        }

    @Test
    fun `GIVEN the store cannot be read WHEN prepared THEN PersistenceFailed and no address is requested`() = runTest {
        // Arrange
        val cause = IllegalStateException("unreadable")
        coEvery { store.get(USER_WALLET_ID) } throws cause

        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.PersistenceFailed(cause).left())
        coVerify(exactly = 0) { bridgeRepository.createWithdrawAddress(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { polymarketRepository.getRelayerNonce(any()) }
    }

    @Test
    fun `GIVEN the relayer rejects the batch WHEN confirmed THEN the record is cleared and Rejected is returned`() =
        runTest {
            // Arrange
            val prepared = interactor.prepare(params()).getOrNull()!!
            val error = PredictionWithdrawError.RelayerRejected(detail = "signature mismatch")
            coEvery { withdrawRepository.submit(any(), any()) } returns error.left()

            // Act
            val actual = interactor.confirm(prepared)

            // Assert
            assertThat(actual).isEqualTo(PredictionWithdrawalFailure.Rejected(error).left())
            coVerify(exactly = 1) { store.clear(USER_WALLET_ID) }
            coVerify(exactly = 1) { store.save(USER_WALLET_ID, any()) }
        }

    @Test
    fun `GIVEN the request is invalid WHEN confirmed THEN the record is cleared and Rejected is returned`() = runTest {
        // Arrange
        val prepared = interactor.prepare(params()).getOrNull()!!
        val error = PredictionWithdrawError.InvalidRequest(detail = "bad deadline")
        coEvery { withdrawRepository.submit(any(), any()) } returns error.left()

        // Act
        val actual = interactor.confirm(prepared)

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.Rejected(error).left())
        coVerify(exactly = 1) { store.clear(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN the relayer rejects the batch and the clear throws WHEN confirmed THEN Rejected is still returned`() =
        runTest {
            // Arrange
            val prepared = interactor.prepare(params()).getOrNull()!!
            val error = PredictionWithdrawError.RelayerRejected(detail = null)
            coEvery { withdrawRepository.submit(any(), any()) } returns error.left()
            coEvery { store.clear(USER_WALLET_ID) } throws IllegalStateException("disk full")

            // Act
            val actual = interactor.confirm(prepared)

            // Assert
            assertThat(actual).isEqualTo(PredictionWithdrawalFailure.Rejected(error).left())
        }

    @Test
    fun `GIVEN the nonce is taken WHEN confirmed THEN the record is kept`() = runTest {
        // Arrange
        val prepared = interactor.prepare(params()).getOrNull()!!
        val error = PredictionWithdrawError.NonceTaken(detail = "nonce 9 is taken")
        coEvery { withdrawRepository.submit(any(), any()) } returns error.left()

        // Act
        val actual = interactor.confirm(prepared)

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.Rejected(error).left())
        coVerify(exactly = 0) { store.clear(any()) }
    }

    @Test
    fun `GIVEN the relayer is unavailable WHEN confirmed THEN the record is kept`() = runTest {
        // Arrange
        val prepared = interactor.prepare(params()).getOrNull()!!
        coEvery { withdrawRepository.submit(any(), any()) } returns PredictionWithdrawError.RelayerUnavailable.left()

        // Act
        interactor.confirm(prepared)

        // Assert
        coVerify(exactly = 0) { store.clear(any()) }
    }

    @Test
    fun `GIVEN the store cannot be read WHEN resent THEN PersistenceFailed and nothing is submitted`() = runTest {
        // Arrange
        val cause = IllegalStateException("unreadable")
        coEvery { store.get(USER_WALLET_ID) } throws cause

        // Act
        val actual = interactor.resend(userWalletId = USER_WALLET_ID, walletId = WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.PersistenceFailed(cause).left())
        coVerify(exactly = 0) { withdrawRepository.submit(any(), any()) }
    }

    @Test
    fun `GIVEN the relayer rejects the resend WHEN resent THEN the record is cleared`() = runTest {
        // Arrange
        coEvery { store.get(USER_WALLET_ID) } returns record(transactionId = null)
        val error = PredictionWithdrawError.RelayerRejected(detail = null)
        coEvery { withdrawRepository.submit(any(), any()) } returns error.left()

        // Act
        val actual = interactor.resend(userWalletId = USER_WALLET_ID, walletId = WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.Rejected(error).left())
        coVerify(exactly = 1) { store.clear(USER_WALLET_ID) }
    }

    @Test
    fun `GIVEN a withdrawal WHEN prepared THEN nothing is signed or sent yet`() = runTest {
        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual.getOrNull()?.decoded?.recipient).isEqualTo(BRIDGE)
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
        coVerify(exactly = 0) { withdrawRepository.submit(any(), any()) }
        coVerify(exactly = 0) { store.save(any(), any()) }
    }

    @Test
    fun `GIVEN an accepted submission WHEN resent THEN the same batch goes out and no nonce is read`() = runTest {
        // Arrange
        coEvery { store.get(USER_WALLET_ID) } returns PredictionWithdrawalRecord(
            batch = batch(),
            transactionId = TX_ID,
        )

        // Act
        val actual = interactor.resend(userWalletId = USER_WALLET_ID, walletId = WALLET_ID)

        // Assert
        assertThat(actual).isEqualTo(receipt().right())
        coVerify(exactly = 1) { withdrawRepository.submit(WALLET_ID, batch()) }
        coVerify(exactly = 0) { polymarketRepository.getRelayerNonce(any()) }
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
    }

    @Test
    fun `GIVEN calldata that does not decode back WHEN prepared THEN nothing is signed`() = runTest {
        // Arrange
        every { codec.decode(any()) } returns null

        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.CalldataMismatch.left())
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
        coVerify(exactly = 0) { withdrawRepository.submit(any(), any()) }
    }

    @Test
    fun `GIVEN calldata that decodes to another recipient WHEN prepared THEN nothing is signed`() = runTest {
        // Arrange
        every { codec.decode(any()) } returns PredictionTransferCall(
            recipient = RECIPIENT,
            amountBaseUnits = BigInteger("12500000"),
        )

        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.CalldataMismatch.left())
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
    }

    @Test
    fun `GIVEN an unfinished withdrawal WHEN a new one is prepared THEN it is refused`() = runTest {
        // Arrange
        coEvery { store.get(USER_WALLET_ID) } returns PredictionWithdrawalRecord(
            batch = batch(),
            transactionId = TX_ID,
        )

        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.AlreadyInFlight.left())
        coVerify(exactly = 0) { signer.signRelayedBatch(any(), any()) }
    }

    @Test
    fun `GIVEN the pre-submit save throws WHEN confirmed THEN PersistenceFailed and nothing is submitted`() = runTest {
        // Arrange
        val prepared = interactor.prepare(params()).getOrNull()!!
        val cause = IllegalStateException("disk full")
        coEvery { store.save(USER_WALLET_ID, any()) } throws cause

        // Act
        val actual = interactor.confirm(prepared)

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.PersistenceFailed(cause).left())
        coVerify(exactly = 0) { withdrawRepository.submit(any(), any()) }
    }

    @Test
    fun `GIVEN the card tap is cancelled WHEN confirmed THEN SigningFailed carries the cause and nothing is stored`() =
        runTest {
            // Arrange
            val prepared = interactor.prepare(params()).getOrNull()!!
            coEvery { signer.signRelayedBatch(any(), any()) } returns PolymarketSigningError.UserCancelled.left()

            // Act
            val actual = interactor.confirm(prepared)

            // Assert
            assertThat(actual).isEqualTo(
                PredictionWithdrawalFailure.SigningFailed(PolymarketSigningError.UserCancelled).left(),
            )
            coVerify(exactly = 0) { store.save(any(), any()) }
            coVerify(exactly = 0) { withdrawRepository.submit(any(), any()) }
        }

    @Test
    fun `GIVEN the post-202 save throws WHEN confirmed THEN the receipt is still returned`() = runTest {
        // Arrange
        val prepared = interactor.prepare(params()).getOrNull()!!
        coEvery { store.save(USER_WALLET_ID, match { it.transactionId == null }) } returns Unit
        coEvery { store.save(USER_WALLET_ID, match { it.transactionId == TX_ID }) } throws
            IllegalStateException("disk full")

        // Act
        val actual = interactor.confirm(prepared)

        // Assert
        assertThat(actual).isEqualTo(receipt().right())
        coVerify(exactly = 1) { withdrawRepository.submit(WALLET_ID, any()) }
    }

    private fun params() = SubmitPredictionWithdrawalInteractor.Params(
        userWalletId = USER_WALLET_ID,
        walletId = WALLET_ID,
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        amount = BigDecimal("12.50"),
        toChainId = "8453",
        toTokenAddress = "0x833589fcd6edb6e08f4c7c32d4f71b54bda02913",
        recipientAddress = RECIPIENT,
    )

    private fun batch() = PredictionWithdrawalBatch(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        nonce = "9",
        deadline = "1787346200",
        call = PolymarketApprovalCall(target = PolymarketContracts.COLLATERAL, value = "0", data = "0xa9059cbb"),
        signature = SIGNATURE,
    )

    private fun PredictionWithdrawalRecord.isSignedRecord(transactionId: String?): Boolean =
        this.transactionId == transactionId && batch == batch().copy(deadline = batch.deadline)

    private fun record(transactionId: String?) = PredictionWithdrawalRecord(
        batch = batch(),
        transactionId = transactionId,
    )

    private fun receipt() = PredictionWithdrawalReceipt(
        transactionId = TX_ID,
        state = PredictionWithdrawalState.IN_PROGRESS,
        transactionHash = null,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val WALLET_ID = "a1b2"
        const val TX_ID = "0x4f2c"
        const val OWNER = "0xabc0000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xdef0000000000000000000000000000000000002"
        const val RECIPIENT = "0x9a1b000000000000000000000000000000007f30"
        const val BRIDGE = "0x5f3b00000000000000000000000000000000c1a2"
        const val SIGNATURE = "0xdeadbeef"
    }
}