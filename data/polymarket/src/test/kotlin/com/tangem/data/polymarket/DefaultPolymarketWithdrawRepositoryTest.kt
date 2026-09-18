package com.tangem.data.polymarket

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.polymarket.error.PredictionWithdrawErrorResolver
import com.tangem.domain.polymarket.approval.PolymarketContracts
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionWithdrawalBatch
import com.tangem.domain.polymarket.model.PredictionWithdrawalReceipt
import com.tangem.domain.polymarket.model.PredictionWithdrawalState
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultPolymarketWithdrawRepositoryTest {

    private val polymarketApi: PolymarketApi = mockk()
    private val errorResolver: PredictionWithdrawErrorResolver = mockk()

    private val repository = DefaultPolymarketWithdrawRepository(
        polymarketApi = polymarketApi,
        errorResolver = errorResolver,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketApi, errorResolver)
    }

    @Test
    fun `GIVEN a signed batch WHEN submitted THEN calls carries exactly one entry sent verbatim`() = runTest {
        // Arrange
        val body = slot<PolymarketRelayedBatchRequest>()
        coEvery { polymarketApi.submitWithdraw(any(), capture(body)) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "IN_PROGRESS", transactionHash = null),
        )

        // Act
        val actual = repository.submit(walletId = WALLET_ID, batch = batch())

        // Assert
        assertThat(body.captured.calls).hasSize(1)
        assertThat(body.captured.calls.single().target).isEqualTo(PolymarketContracts.COLLATERAL)
        assertThat(body.captured.calls.single().value).isEqualTo("0")
        assertThat(body.captured.calls.single().data).isEqualTo(DATA)
        assertThat(actual).isEqualTo(
            PredictionWithdrawalReceipt(
                transactionId = TX_ID,
                state = PredictionWithdrawalState.IN_PROGRESS,
                transactionHash = null,
            ).right(),
        )
    }

    @Test
    fun `GIVEN a confirmed withdrawal without a hash WHEN state read THEN the hash stays null`() = runTest {
        // Arrange
        coEvery { polymarketApi.getWithdrawState(any(), any()) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "CONFIRMED", transactionHash = null),
        )

        // Act
        val actual = repository.getState(walletId = WALLET_ID, transactionId = TX_ID)

        // Assert
        assertThat(actual).isEqualTo(
            PredictionWithdrawalReceipt(
                transactionId = TX_ID,
                state = PredictionWithdrawalState.CONFIRMED,
                transactionHash = null,
            ).right(),
        )
    }

    @Test
    fun `GIVEN an unknown state WHEN state read THEN polling continues rather than reporting a terminal`() = runTest {
        // Arrange
        coEvery { polymarketApi.getWithdrawState(any(), any()) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "SOMETHING_NEW", transactionHash = null),
        )

        // Act
        val actual = repository.getState(walletId = WALLET_ID, transactionId = TX_ID)

        // Assert
        assertThat(actual.getOrNull()?.state).isEqualTo(PredictionWithdrawalState.IN_PROGRESS)
    }

    private fun batch() = PredictionWithdrawalBatch(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        nonce = "9",
        deadline = "1787346200",
        call = PolymarketApprovalCall(target = PolymarketContracts.COLLATERAL, value = "0", data = DATA),
        signature = "0xdeadbeef",
    )

    private companion object {
        const val WALLET_ID = "a1b2"
        const val TX_ID = "0x4f2c"
        const val OWNER = "0xabc0000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xdef0000000000000000000000000000000000002"
        const val DATA = "0xa9059cbb"
    }
}