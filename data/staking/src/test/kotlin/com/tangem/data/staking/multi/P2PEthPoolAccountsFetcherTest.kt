package com.tangem.data.staking.multi

import com.google.common.truth.Truth.assertThat
import com.tangem.common.test.data.staking.MockP2PEthPoolAccountResponseFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.domain.models.staking.StakingID
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.ethpool.models.request.P2PEthPoolAccountsListRequest
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountListItem
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolAccountsListResponse
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolErrorDetailsDTO
import com.tangem.grow.datasource.ethpool.models.response.P2PEthPoolResponse
import com.tangem.test.mock.staking.MockStaking
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class P2PEthPoolAccountsFetcherTest {

    private val api: P2PEthPoolApi = mockk()

    private val fetcher = P2PEthPoolAccountsFetcher(
        p2pEthPoolApi = api,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    private val vaultA = MockStaking.vault(vaultAddress = VAULT_A)
    private val vaultB = MockStaking.vault(vaultAddress = VAULT_B)

    @BeforeEach
    fun resetMocks() {
        clearMocks(api)
    }

    private fun stub(vault: String, response: ApiResponse<P2PEthPoolResponse<P2PEthPoolAccountsListResponse>>) {
        coEvery { api.getAccountsList(network = any(), vaultAddress = vault, body = any()) } returns response
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Success {

        @Test
        fun `GIVEN two vaults with accounts WHEN fetchBatch THEN union of all accounts`() = runTest {
            // Arrange
            stub(VAULT_A, success(okItem(account(ADDR_1, VAULT_A))))
            stub(VAULT_B, success(okItem(account(ADDR_2, VAULT_B))))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA, vaultB), setOf(ADDR_1, ADDR_2))

            // Assert
            assertThat(actual).isEqualTo(setOf(account(ADDR_1, VAULT_A), account(ADDR_2, VAULT_B)))
        }

        @Test
        fun `GIVEN same account listed twice WHEN fetchBatch THEN deduplicated`() = runTest {
            // Arrange
            val duplicated = account(ADDR_1, VAULT_A)
            stub(VAULT_A, success(okItem(duplicated), okItem(duplicated)))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA), setOf(ADDR_1))

            // Assert
            assertThat(actual).isEqualTo(setOf(duplicated))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Errors {

        @Test
        fun `GIVEN http error for one vault WHEN fetchBatch THEN only other vault accounts returned`() = runTest {
            // Arrange
            stub(VAULT_A, httpError())
            stub(VAULT_B, success(okItem(account(ADDR_2, VAULT_B))))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA, vaultB), setOf(ADDR_1, ADDR_2))

            // Assert
            assertThat(actual).isEqualTo(setOf(account(ADDR_2, VAULT_B)))
        }

        @Test
        fun `GIVEN envelope error for one vault WHEN fetchBatch THEN that vault contributes nothing`() = runTest {
            // Arrange
            stub(
                VAULT_A,
                ApiResponse.Success(
                    P2PEthPoolResponse(
                        error = P2PEthPoolErrorDetailsDTO(101111, "boom", null),
                        result = P2PEthPoolAccountsListResponse(listOf(okItem(account(ADDR_1, VAULT_A)))),
                    ),
                ),
            )
            stub(VAULT_B, success(okItem(account(ADDR_2, VAULT_B))))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA, vaultB), setOf(ADDR_1, ADDR_2))

            // Assert
            assertThat(actual).isEqualTo(setOf(account(ADDR_2, VAULT_B)))
        }

        @Test
        fun `GIVEN success with null result WHEN fetchBatch THEN that vault contributes nothing`() = runTest {
            // Arrange
            stub(
                VAULT_A,
                ApiResponse.Success(P2PEthPoolResponse<P2PEthPoolAccountsListResponse>(error = null, result = null)),
            )
            stub(VAULT_B, success(okItem(account(ADDR_2, VAULT_B))))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA, vaultB), setOf(ADDR_1, ADDR_2))

            // Assert
            assertThat(actual).isEqualTo(setOf(account(ADDR_2, VAULT_B)))
        }

        @Test
        fun `GIVEN per item error WHEN fetchBatch THEN errored address filtered out`() = runTest {
            // Arrange
            stub(VAULT_A, success(okItem(account(ADDR_1, VAULT_A)), errorItem(ADDR_2)))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA), setOf(ADDR_1, ADDR_2))

            // Assert
            assertThat(actual).isEqualTo(setOf(account(ADDR_1, VAULT_A)))
        }

        @Test
        fun `GIVEN item without account and without error WHEN fetchBatch THEN item dropped`() = runTest {
            // Arrange
            stub(VAULT_A, success(P2PEthPoolAccountListItem(ADDR_1, account = null, error = null)))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA), setOf(ADDR_1))

            // Assert
            assertThat(actual).isEmpty()
        }

        @Test
        fun `GIVEN api throws for one vault WHEN fetchBatch THEN failure isolated to that vault`() = runTest {
            // Arrange
            coEvery { api.getAccountsList(any(), VAULT_A, any()) } throws IllegalStateException("io")
            stub(VAULT_B, success(okItem(account(ADDR_2, VAULT_B))))

            // Act
            val actual = fetcher.fetchBatch(listOf(vaultA, vaultB), setOf(ADDR_1, ADDR_2))

            // Assert
            assertThat(actual).isEqualTo(setOf(account(ADDR_2, VAULT_B)))
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Request {

        @Test
        fun `GIVEN addresses WHEN fetchBatch THEN each vault receives all addresses in one request`() = runTest {
            // Arrange
            stub(VAULT_A, success())
            stub(VAULT_B, success())

            // Act
            fetcher.fetchBatch(listOf(vaultA, vaultB), setOf(ADDR_1, ADDR_2))

            // Assert
            val expectedRequest = P2PEthPoolAccountsListRequest(listOf(ADDR_1, ADDR_2))
            coVerify(exactly = 1) { api.getAccountsList(any(), VAULT_A, expectedRequest) }
            coVerify(exactly = 1) { api.getAccountsList(any(), VAULT_B, expectedRequest) }
        }
    }

    private fun account(address: String, vault: String) = MockP2PEthPoolAccountResponseFactory.createWithBalance(
        stakingId = StakingID(MockStaking.P2P_ID, address),
        vaultAddress = vault,
    )

    private fun okItem(account: P2PEthPoolAccountResponse) =
        P2PEthPoolAccountListItem(account.delegatorAddress, account = account, error = null)

    private fun errorItem(address: String) = P2PEthPoolAccountListItem(
        delegatorAddress = address,
        account = account(address, VAULT_A),
        error = P2PEthPoolErrorDetailsDTO(code = 127108, message = "invalid", name = null, errors = null),
    )

    private fun success(vararg items: P2PEthPoolAccountListItem): ApiResponse<P2PEthPoolResponse<P2PEthPoolAccountsListResponse>> =
        ApiResponse.Success(
            P2PEthPoolResponse(error = null, result = P2PEthPoolAccountsListResponse(list = items.toList())),
        )

    @Suppress("UNCHECKED_CAST")
    private fun httpError() = ApiResponse.Error(ApiResponseError.NetworkException())
        as ApiResponse<P2PEthPoolResponse<P2PEthPoolAccountsListResponse>>

    private companion object {

        const val ADDR_1 = "0x1111111111111111111111111111111111111111"
        const val ADDR_2 = "0x2222222222222222222222222222222222222222"
        const val VAULT_A = "0xvaulta"
        const val VAULT_B = "0xvaultb"
    }
}