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
import com.tangem.domain.polymarket.model.PredictionTransferCall
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
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
        coEvery { signer.signWithdrawal(any(), any()) } returns SIGNATURE.right()
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
            store.save(USER_WALLET_ID, any())
            withdrawRepository.submit(WALLET_ID, any())
        }
    }

    @Test
    fun `GIVEN a withdrawal WHEN prepared THEN nothing is signed or sent yet`() = runTest {
        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual.getOrNull()?.decoded?.recipient).isEqualTo(BRIDGE)
        coVerify(exactly = 0) { signer.signWithdrawal(any(), any()) }
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
        coVerify(exactly = 0) { signer.signWithdrawal(any(), any()) }
    }

    @Test
    fun `GIVEN calldata that does not decode back WHEN prepared THEN nothing is signed`() = runTest {
        // Arrange
        every { codec.decode(any()) } returns null

        // Act
        val actual = interactor.prepare(params())

        // Assert
        assertThat(actual).isEqualTo(PredictionWithdrawalFailure.CalldataMismatch.left())
        coVerify(exactly = 0) { signer.signWithdrawal(any(), any()) }
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
        coVerify(exactly = 0) { signer.signWithdrawal(any(), any()) }
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
        coVerify(exactly = 0) { signer.signWithdrawal(any(), any()) }
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