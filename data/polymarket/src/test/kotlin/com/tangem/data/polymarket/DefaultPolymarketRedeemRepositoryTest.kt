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
import arrow.core.nonEmptyListOf
import arrow.core.left
import io.mockk.every
import com.tangem.core.remote.response.ApiResponseError.HttpException.Code
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.grow.datasource.polymarket.models.PolymarketRedeemCallResponse
import com.tangem.domain.polymarket.model.PolymarketRedeemError
import com.tangem.domain.polymarket.model.PredictionRedeemCalls
import com.tangem.domain.polymarket.model.PolymarketMarketVersion
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
    fun `GIVEN a versioned answer with two calls WHEN the calls are read THEN both come back in order`() = runTest {
        // Arrange
        coEvery { polymarketApi.getRedeemCall(any(), any(), any()) } returns ApiResponse.Success(
            PolymarketRedeemCallResponse(
                version = "v2",
                calls = listOf(
                    PolymarketApprovalCallDto(target = ROUTER, value = "0", data = DATA),
                    PolymarketApprovalCallDto(target = ROUTER, value = "0", data = SECOND_DATA),
                ),
                target = ROUTER,
                value = "0",
                data = DATA,
            ),
        )

        // Act
        val actual = repository.getCalls(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = true)

        // Assert
        coVerify(exactly = 1) {
            polymarketApi.getRedeemCall(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = true)
        }
        assertThat(actual).isEqualTo(
            PredictionRedeemCalls(
                version = PolymarketMarketVersion.V2,
                calls = nonEmptyListOf(
                    PolymarketApprovalCall(target = ROUTER, value = "0", data = DATA),
                    PolymarketApprovalCall(target = ROUTER, value = "0", data = SECOND_DATA),
                ),
            ).right(),
        )
    }

    @Test
    fun `GIVEN an answer without calls WHEN the calls are read THEN the flat call is the only one and has no version`() =
        runTest {
            // Arrange
            coEvery { polymarketApi.getRedeemCall(any(), any(), any()) } returns ApiResponse.Success(
                PolymarketRedeemCallResponse(target = TARGET, value = "0", data = DATA),
            )

            // Act
            val actual = repository.getCalls(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = false)

            // Assert
            assertThat(actual).isEqualTo(
                PredictionRedeemCalls(
                    version = null,
                    calls = nonEmptyListOf(PolymarketApprovalCall(target = TARGET, value = "0", data = DATA)),
                ).right(),
            )
        }

    @Test
    fun `GIVEN an empty calls array beside a flat call WHEN the calls are read THEN the array is believed`() = runTest {
        // Arrange
        coEvery { polymarketApi.getRedeemCall(any(), any(), any()) } returns ApiResponse.Success(
            PolymarketRedeemCallResponse(version = "v2", calls = emptyList(), target = TARGET, value = "0", data = DATA),
        )

        // Act
        val actual = repository.getCalls(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = false)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketRedeemError.Unexpected(httpCode = null, detail = "Redeem call answer holds no calls").left(),
        )
    }

    @Test
    fun `GIVEN no calls and an incomplete flat call WHEN the calls are read THEN the failure names it`() = runTest {
        // Arrange
        coEvery { polymarketApi.getRedeemCall(any(), any(), any()) } returns ApiResponse.Success(
            PolymarketRedeemCallResponse(version = "v1", target = TARGET, data = DATA),
        )

        // Act
        val actual = repository.getCalls(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = false)

        // Assert
        assertThat(actual).isEqualTo(
            PolymarketRedeemError.Unexpected(
                httpCode = null,
                detail = "Redeem call answer holds neither calls nor a complete flat call",
            ).left(),
        )
    }

    @Test
    fun `GIVEN the lookup fails WHEN the calls are read THEN the call-lookup resolver decides`() = runTest {
        // Arrange
        val error = ApiResponseError.HttpException(code = Code.CONFLICT, message = "", errorBody = null)
        @Suppress("UNCHECKED_CAST")
        coEvery { polymarketApi.getRedeemCall(any(), any(), any()) } returns
            ApiResponse.Error(error) as ApiResponse<PolymarketRedeemCallResponse>
        every { errorResolver.resolveCallLookup(error) } returns PolymarketRedeemError.NothingToRedeem(detail = null)

        // Act
        val actual = repository.getCalls(walletId = WALLET_ID, conditionId = CONDITION_ID, isNegRisk = false)

        // Assert
        assertThat(actual).isEqualTo(PolymarketRedeemError.NothingToRedeem(detail = null).left())
    }

    @Test
    fun `GIVEN a signed batch WHEN submitted THEN every call is sent verbatim and in order`() = runTest {
        // Arrange
        val body = slot<PolymarketRelayedBatchRequest>()
        coEvery { polymarketApi.submitRedeem(any(), capture(body)) } returns ApiResponse.Success(
            PolymarketRelayedBatchResponse(transactionId = TX_ID, state = "IN_PROGRESS", transactionHash = null),
        )

        // Act
        val actual = repository.submit(walletId = WALLET_ID, batch = batch())

        // Assert
        assertThat(body.captured.calls).containsExactly(
            PolymarketApprovalCallDto(target = TARGET, value = "0", data = DATA),
            PolymarketApprovalCallDto(target = ROUTER, value = "0", data = SECOND_DATA),
        ).inOrder()
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
        calls = nonEmptyListOf(
            PolymarketApprovalCall(target = TARGET, value = "0", data = DATA),
            PolymarketApprovalCall(target = ROUTER, value = "0", data = SECOND_DATA),
        ),
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
        const val ROUTER = "0x12121212006e4cd160d18e3f00711da5c3372600"
        const val DATA = "0x01b7037c"
        const val SECOND_DATA = "0xd217a3cc"
    }
}