package com.tangem.data.polymarket

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.remote.response.ApiResponse
import com.tangem.data.polymarket.error.PolymarketRedeemErrorResolver
import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionClaimBatch
import com.tangem.domain.polymarket.model.PredictionClaimReceipt
import com.tangem.domain.polymarket.model.PredictionClaimState
import com.tangem.grow.datasource.polymarket.PolymarketApi
import com.tangem.grow.datasource.polymarket.models.PolymarketApprovalCallDto
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchRequest
import com.tangem.grow.datasource.polymarket.models.PolymarketRelayedBatchResponse
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DefaultPolymarketRedeemRepositoryTest {

    private val polymarketApi: PolymarketApi = mockk()
    private val errorResolver: PolymarketRedeemErrorResolver = mockk()

    private val repository = DefaultPolymarketRedeemRepository(
        polymarketApi = polymarketApi,
        errorResolver = errorResolver,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(polymarketApi, errorResolver)
    }

    @Test
    fun `GIVEN a market WHEN the call is read THEN the market and its neg risk flag reach the query`() = runTest {
        // Arrange
        coEvery { polymarketApi.getRedeemCall(any(), any(), any()) } returns ApiResponse.Success(
            PolymarketApprovalCallDto(target = TARGET, value = "0", data = DATA),
        )

        // Act
        val actual = repository.getCall(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = true)

        // Assert
        coVerify(exactly = 1) {
            polymarketApi.getRedeemCall(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = true)
        }
        assertThat(actual).isEqualTo(PolymarketApprovalCall(target = TARGET, value = "0", data = DATA).right())
    }

    @Test
    fun `GIVEN a signed batch WHEN submitted THEN calls carries exactly one entry sent verbatim`() = runTest {
        // Arrange
        val body = slot<PolymarketRelayedBatchRequest>()
        coEvery { polymarketApi.submitRedeem(any(), capture(body)) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "IN_PROGRESS", transactionHash = null),
        )

        // Act
        val actual = repository.submit(walletId = WALLET_ID, batch = batch())

        // Assert
        assertThat(body.captured.calls).hasSize(1)
        assertThat(body.captured.calls.single().target).isEqualTo(TARGET)
        assertThat(body.captured.calls.single().value).isEqualTo("0")
        assertThat(body.captured.calls.single().data).isEqualTo(DATA)
        assertThat(body.captured.nonce).isEqualTo("9")
        assertThat(body.captured.deadline).isEqualTo("1787346200")
        assertThat(actual).isEqualTo(
            PredictionClaimReceipt(
                transactionId = TX_ID,
                state = PredictionClaimState.IN_PROGRESS,
                transactionHash = null,
            ).right(),
        )
    }

    @Test
    fun `GIVEN a confirmed claim WHEN state read THEN the receipt carries the chain hash`() = runTest {
        // Arrange
        coEvery { polymarketApi.getRedeemState(any(), any()) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "CONFIRMED", transactionHash = HASH),
        )

        // Act
        val actual = repository.getState(walletId = WALLET_ID, transactionId = TX_ID)

        // Assert
        coVerify(exactly = 1) { polymarketApi.getRedeemState(walletId = WALLET_ID, transactionId = TX_ID) }
        assertThat(actual).isEqualTo(
            PredictionClaimReceipt(
                transactionId = TX_ID,
                state = PredictionClaimState.CONFIRMED,
                transactionHash = HASH,
            ).right(),
        )
    }

    @Test
    fun `GIVEN an unknown state WHEN state read THEN polling continues rather than reporting a terminal`() = runTest {
        // Arrange
        coEvery { polymarketApi.getRedeemState(any(), any()) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "SOMETHING_NEW", transactionHash = null),
        )

        // Act
        val actual = repository.getState(walletId = WALLET_ID, transactionId = TX_ID)

        // Assert
        assertThat(actual.getOrNull()?.state).isEqualTo(PredictionClaimState.IN_PROGRESS)
    }

    private fun batch() = PredictionClaimBatch(
        ownerAddress = OWNER,
        depositWalletAddress = DEPOSIT_WALLET,
        nonce = "9",
        deadline = "1787346200",
        call = PolymarketApprovalCall(target = TARGET, value = "0", data = DATA),
        signature = "0xdeadbeef",
    )

    private companion object {
        const val WALLET_ID = "a1b2"
        const val CONDITION_ID = "0x94947a23"
        const val TX_ID = "0x4f2c"
        const val HASH = "0x7ab1"
        const val OWNER = "0xabc0000000000000000000000000000000000001"
        const val DEPOSIT_WALLET = "0xdef0000000000000000000000000000000000002"
        const val TARGET = "0xada2005600dec949baf300f4c6120000bdb6eaab"
        const val DATA = "0x01b7037c"
    }
}