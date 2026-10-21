package com.tangem.data.staking

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.data.staking.StakeKitRepositoryFixtures.apiError
import com.tangem.data.staking.store.P2PEthPoolRegionBlockedStore
import com.tangem.data.staking.store.P2PVaultLimitsStore
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.domain.staking.model.ethpool.P2PEthPoolBroadcastResult
import com.tangem.domain.staking.model.ethpool.P2PEthPoolNetwork
import com.tangem.domain.staking.model.ethpool.P2PEthPoolUnsignedTx
import com.tangem.domain.staking.model.stakekit.StakingError
import com.tangem.domain.staking.toggles.StakingFeatureToggles
import com.tangem.domain.txhistory.TxHistoryFeatureToggles
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.ethpool.models.request.P2PEthPoolBroadcastRequest
import com.tangem.grow.datasource.ethpool.models.request.P2PEthPoolTransactionRequest
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolBroadcastResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolErrorDetailsDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolTransactionResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolUnsignedTxDTO
import com.tangem.store.datasource.txhistory.db.dao.P2PEthPoolVaultDao
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.joda.time.DateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultP2PEthPoolRepositoryTransactionsTest {

    private val api: P2PEthPoolApi = mockk()
    private val vaultsStore: P2PEthPoolVaultsStore = mockk(relaxed = true)
    private val limitsStore: P2PVaultLimitsStore = mockk(relaxed = true)
    private val tangemTechApi: TangemTechApi = mockk(relaxed = true)
    private val featureToggles: StakingFeatureToggles = mockk(relaxed = true)
    private val regionBlockedStore: P2PEthPoolRegionBlockedStore = mockk(relaxed = true)
    private val vaultDao: P2PEthPoolVaultDao = mockk(relaxed = true)
    private val txHistoryFeatureToggles: TxHistoryFeatureToggles = mockk(relaxed = true)

    private val repository = DefaultP2PEthPoolRepository(
        p2pEthPoolApi = api,
        p2pEthPoolVaultsStore = vaultsStore,
        p2pVaultLimitsStore = limitsStore,
        p2pEthPoolVaultDao = vaultDao,
        tangemTechApi = tangemTechApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
        stakingFeatureToggles = featureToggles,
        txHistoryFeatureToggle = txHistoryFeatureToggles,
        p2pEthPoolRegionBlockedStore = regionBlockedStore,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
    }

    internal enum class TxEndpoint { DEPOSIT, UNSTAKE, WITHDRAW }

    private fun unsignedTxDTO() = P2PEthPoolUnsignedTxDTO(
        serializeTx = "0x02f8",
        to = VAULT,
        data = "0xdata",
        value = "1.5",
        nonce = 7,
        chainId = 1,
        gasLimit = BigDecimal("21000"),
        maxFeePerGas = BigDecimal("30"),
        maxPriorityFeePerGas = BigDecimal("2"),
    )

    private fun txResponse() = P2PEthPoolTransactionResponse(
        amount = BigDecimal("1.5"),
        vaultAddress = VAULT,
        delegatorAddress = DELEGATOR,
        unsignedTransaction = unsignedTxDTO(),
        createdAt = DateTime(0L),
    )

    private val expectedTx = P2PEthPoolUnsignedTx(
        serializeTx = "0x02f8",
        to = VAULT,
        data = "0xdata",
        value = BigDecimal("1.5"),
        nonce = 7,
        chainId = 1,
        gasLimit = BigDecimal("21000"),
        maxFeePerGas = BigDecimal("30"),
        maxPriorityFeePerGas = BigDecimal("2"),
    )

    private fun errorDetails(code: Int, message: String = "boom") = P2PEthPoolErrorDetailsDTO(
        code = code,
        message = message,
        name = "Error",
    )

    private fun <T : Any> ok(result: T): ApiResponse<P2PEthPoolResponse<T>> =
        ApiResponse.Success(P2PEthPoolResponse(error = null, result = result))

    private fun <T : Any> envelopeError(code: Int, message: String): ApiResponse<P2PEthPoolResponse<T>> =
        ApiResponse.Success(P2PEthPoolResponse(error = errorDetails(code, message), result = null))

    private fun stub(endpoint: TxEndpoint, response: ApiResponse<P2PEthPoolResponse<P2PEthPoolTransactionResponse>>) {
        when (endpoint) {
            TxEndpoint.DEPOSIT -> coEvery { api.createDepositTransaction(any(), any()) } returns response
            TxEndpoint.UNSTAKE -> coEvery { api.createUnstakeTransaction(any(), any()) } returns response
            TxEndpoint.WITHDRAW -> coEvery { api.createWithdrawTransaction(any(), any()) } returns response
        }
    }

    private suspend fun call(
        endpoint: TxEndpoint,
        amount: String = "1.5",
        network: P2PEthPoolNetwork = P2PEthPoolNetwork.MAINNET,
    ): Either<StakingError, P2PEthPoolUnsignedTx> = when (endpoint) {
        TxEndpoint.DEPOSIT -> repository.createDepositTransaction(network, DELEGATOR, VAULT, amount)
        TxEndpoint.UNSTAKE -> repository.createUnstakeTransaction(network, DELEGATOR, VAULT, amount)
        TxEndpoint.WITHDRAW -> repository.createWithdrawTransaction(network, DELEGATOR, VAULT, amount)
    }

    private fun verifyNoTransactionCalls() {
        coVerify(exactly = 0) { api.createDepositTransaction(any(), any()) }
        coVerify(exactly = 0) { api.createUnstakeTransaction(any(), any()) }
        coVerify(exactly = 0) { api.createWithdrawTransaction(any(), any()) }
    }

    private fun verifyOnlyEndpointCalled(endpoint: TxEndpoint, request: P2PEthPoolTransactionRequest) {
        val deposit = if (endpoint == TxEndpoint.DEPOSIT) 1 else 0
        val unstake = if (endpoint == TxEndpoint.UNSTAKE) 1 else 0
        val withdraw = if (endpoint == TxEndpoint.WITHDRAW) 1 else 0
        coVerify(exactly = deposit) { api.createDepositTransaction("mainnet", request) }
        coVerify(exactly = unstake) { api.createUnstakeTransaction("mainnet", request) }
        coVerify(exactly = withdraw) { api.createWithdrawTransaction("mainnet", request) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StakingTransactions {

        @ParameterizedTest
        @EnumSource(TxEndpoint::class)
        fun `GIVEN endpoint success WHEN create transaction THEN right with converted unsigned tx`(
            endpoint: TxEndpoint,
        ) = runTest {
            // Arrange
            stub(endpoint, ok(txResponse()))

            // Act
            val actual = call(endpoint)

            // Assert
            assertThat(actual).isEqualTo(expectedTx.right())
        }

        @ParameterizedTest
        @EnumSource(TxEndpoint::class)
        fun `GIVEN endpoint WHEN create transaction THEN request built from args on mainnet path`(
            endpoint: TxEndpoint,
        ) = runTest {
            // Arrange
            stub(endpoint, ok(txResponse()))

            // Act
            call(endpoint)

            // Assert
            verifyOnlyEndpointCalled(endpoint, P2PEthPoolTransactionRequest(DELEGATOR, VAULT, BigDecimal("1.5")))
        }

        @Test
        fun `GIVEN testnet WHEN create deposit transaction THEN hoodi network path used`() = runTest {
            // Arrange
            stub(TxEndpoint.DEPOSIT, ok(txResponse()))

            // Act
            call(TxEndpoint.DEPOSIT, network = P2PEthPoolNetwork.TESTNET)

            // Assert
            coVerify { api.createDepositTransaction("hoodi", any()) }
        }

        @ParameterizedTest
        @EnumSource(TxEndpoint::class)
        fun `GIVEN non numeric amount WHEN create transaction THEN invalid amount without api call`(
            endpoint: TxEndpoint,
        ) = runTest {
            // Act
            val actual = call(endpoint, amount = "abc")

            // Assert
            assertThat(actual).isEqualTo(StakingError.InvalidAmount("Invalid amount format: abc").left())
            verifyNoTransactionCalls()
        }

        @ParameterizedTest
        @EnumSource(TxEndpoint::class)
        fun `GIVEN envelope validation error WHEN create transaction THEN mapped invalid amount`(
            endpoint: TxEndpoint,
        ) = runTest {
            // Arrange
            stub(endpoint, envelopeError(100101, "Amount too low"))

            // Act
            val actual = call(endpoint)

            // Assert
            assertThat(actual).isEqualTo(StakingError.InvalidAmount("Amount too low").left())
        }

        @Test
        fun `GIVEN envelope invalid delegator error WHEN create deposit transaction THEN mapped invalid amount with prefix`() =
            runTest {
                // Arrange
                stub(TxEndpoint.DEPOSIT, envelopeError(127106, "bad"))

                // Act
                val actual = call(TxEndpoint.DEPOSIT)

                // Assert
                assertThat(actual).isEqualTo(StakingError.InvalidAmount("Invalid delegator address: bad").left())
            }

        @Test
        fun `GIVEN envelope unknown code WHEN create unstake transaction THEN unknown error with code in message`() =
            runTest {
                // Arrange
                stub(TxEndpoint.UNSTAKE, envelopeError(999, "boom"))

                // Act
                val actual = call(TxEndpoint.UNSTAKE)

                // Assert
                val error = actual.leftOrNull()
                assertThat(error).isInstanceOf(StakingError.UnknownError::class.java)
                assertThat((error as StakingError.UnknownError).exception).hasMessageThat().isEqualTo("999: boom")
            }

        @Test
        fun `GIVEN envelope with error and result WHEN create withdraw transaction THEN error wins`() = runTest {
            // Arrange
            stub(
                TxEndpoint.WITHDRAW,
                ApiResponse.Success(P2PEthPoolResponse(error = errorDetails(100101, "x"), result = txResponse())),
            )

            // Act
            val actual = call(TxEndpoint.WITHDRAW)

            // Assert
            assertThat(actual).isEqualTo(StakingError.InvalidAmount("x").left())
        }

        @ParameterizedTest
        @EnumSource(TxEndpoint::class)
        fun `GIVEN api error WHEN create transaction THEN unknown error wrapping cause`(endpoint: TxEndpoint) =
            runTest {
                // Arrange
                val cause = ApiResponseError.TimeoutException()
                stub(endpoint, apiError(cause))

                // Act
                val actual = call(endpoint)

                // Assert
                assertThat(actual).isEqualTo(StakingError.UnknownError(cause).left())
            }

        // Pins current behaviour: requireNotNull throws past either {} instead of returning Left
        @Test
        fun `GIVEN success with null result WHEN create deposit transaction THEN throws illegal argument`() = runTest {
            // Arrange
            stub(
                TxEndpoint.DEPOSIT,
                ApiResponse.Success(P2PEthPoolResponse<P2PEthPoolTransactionResponse>(error = null, result = null)),
            )

            // Act
            val error = runCatching { call(TxEndpoint.DEPOSIT) }.exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Result is null in successful response")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Broadcast {

        private val broadcastResponse = P2PEthPoolBroadcastResponse(
            hash = "0xhash",
            status = "pending",
            gasUsed = "21000",
            from = DELEGATOR,
            to = VAULT,
        )

        private val expectedBroadcast = P2PEthPoolBroadcastResult(
            hash = "0xhash",
            status = "pending",
            blockNumber = null,
            transactionIndex = null,
            gasUsed = BigDecimal("21000"),
            cumulativeGasUsed = null,
            effectiveGasPrice = null,
            from = DELEGATOR,
            to = VAULT,
        )

        @Test
        fun `GIVEN success WHEN broadcastTransaction THEN right with converted result`() = runTest {
            // Arrange
            coEvery {
                api.broadcastTransaction("mainnet", P2PEthPoolBroadcastRequest("0xsigned"))
            } returns ok(broadcastResponse)

            // Act
            val actual = repository.broadcastTransaction(P2PEthPoolNetwork.MAINNET, "0xsigned")

            // Assert
            assertThat(actual).isEqualTo(expectedBroadcast.right())
        }

        @Test
        fun `GIVEN envelope error WHEN broadcastTransaction THEN mapped error`() = runTest {
            // Arrange
            coEvery { api.broadcastTransaction(any(), any()) } returns envelopeError(127108, "sim")

            // Act
            val actual = repository.broadcastTransaction(P2PEthPoolNetwork.MAINNET, "0xsigned")

            // Assert
            val error = actual.leftOrNull()
            assertThat(error).isInstanceOf(StakingError.UnknownError::class.java)
            assertThat((error as StakingError.UnknownError).exception).hasMessageThat()
                .isEqualTo("Transaction simulation failed: sim")
        }

        @Test
        fun `GIVEN api error WHEN broadcastTransaction THEN unknown error`() = runTest {
            // Arrange
            val cause = ApiResponseError.NetworkException()
            coEvery { api.broadcastTransaction(any(), any()) } returns apiError(cause)

            // Act
            val actual = repository.broadcastTransaction(P2PEthPoolNetwork.MAINNET, "0xsigned")

            // Assert
            assertThat(actual).isEqualTo(StakingError.UnknownError(cause).left())
        }

        // Pins current behaviour: requireNotNull throws past either {} instead of returning Left
        @Test
        fun `GIVEN success with null result WHEN broadcastTransaction THEN throws illegal argument`() = runTest {
            // Arrange
            coEvery { api.broadcastTransaction(any(), any()) } returns
                ApiResponse.Success(P2PEthPoolResponse<P2PEthPoolBroadcastResponse>(error = null, result = null))

            // Act
            val error = runCatching { repository.broadcastTransaction(P2PEthPoolNetwork.MAINNET, "0xsigned") }
                .exceptionOrNull()

            // Assert
            assertThat(error).isInstanceOf(IllegalArgumentException::class.java)
            assertThat(error).hasMessageThat().isEqualTo("Result is null in successful response")
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class AccountInfo {

        @Test
        fun `GIVEN success WHEN getAccountInfo THEN right with converted account`() = runTest {
            // Arrange
            coEvery { api.getAccountInfo("mainnet", DELEGATOR, VAULT) } returns
                ok<P2PEthPoolAccountResponse>(MockP2PEthPoolAccountResponseFactory.createWithBalance())

            // Act
            val actual = repository.getAccountInfo(P2PEthPoolNetwork.MAINNET, DELEGATOR, VAULT)

            // Assert
            val expected = MockStaking.p2pAccount(
                vaultAddress = MockP2PEthPoolAccountResponseFactory.defaultVaultAddress,
                delegatorAddress = "0x5aa711F440Eb6d4361148bBD89d03464628ace84",
                staked = BigDecimal("1.5"),
                earned = BigDecimal("0.05"),
                availableToUnstake = BigDecimal("1.5"),
                availableToWithdraw = BigDecimal.ZERO,
                exitQueueTotal = BigDecimal.ZERO,
            )
            assertThat(actual).isEqualTo(expected.right())
        }

        @Test
        fun `GIVEN envelope invalid vault error WHEN getAccountInfo THEN data error`() = runTest {
            // Arrange
            coEvery { api.getAccountInfo(any(), any(), any()) } returns envelopeError(127101, "v")

            // Act
            val actual = repository.getAccountInfo(P2PEthPoolNetwork.MAINNET, DELEGATOR, VAULT)

            // Assert
            val error = actual.leftOrNull()
            assertThat(error).isInstanceOf(StakingError.DataError::class.java)
            val exception = (error as StakingError.DataError).exception
            assertThat(exception).isInstanceOf(IllegalStateException::class.java)
            assertThat(exception).hasMessageThat().isEqualTo("Invalid vault: v")
        }

        @Test
        fun `GIVEN api error WHEN getAccountInfo THEN unknown error`() = runTest {
            // Arrange
            val cause = ApiResponseError.NetworkException()
            coEvery { api.getAccountInfo(any(), any(), any()) } returns apiError(cause)

            // Act
            val actual = repository.getAccountInfo(P2PEthPoolNetwork.MAINNET, DELEGATOR, VAULT)

            // Assert
            assertThat(actual).isEqualTo(StakingError.UnknownError(cause).left())
        }
    }

    private companion object {

        const val DELEGATOR = MockStaking.DELEGATOR_ADDRESS
        const val VAULT = MockStaking.VAULT_ADDRESS
    }
}