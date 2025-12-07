package com.tangem.data.staking.multi

import arrow.core.toOption
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.staking.store.P2PBalancesStore
import com.tangem.data.staking.store.StakingBalancesStore
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.test.core.assertEitherLeft
import com.tangem.test.core.assertEitherRight
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMultiStakingBalanceFetcherTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val stakingBalancesStore: StakingBalancesStore = mockk(relaxUnitFun = true)
    private val p2pBalancesStore: P2PBalancesStore = mockk(relaxUnitFun = true)
    private val stakeKitApi: StakeKitApi = mockk()
    private val p2pApi: P2PEthPoolApi = mockk()
    private val p2pVaultsStore: P2PEthPoolVaultsStore = mockk()

    private val fetcher = DefaultMultiStakingBalanceFetcher(
        userWalletsStore = userWalletsStore,
        stakingYieldsStore = stakingYieldsStore,
        stakingBalancesStore = stakingBalancesStore,
        p2pBalancesStore = p2pBalancesStore,
        stakeKitApi = stakeKitApi,
        p2pApi = p2pApi,
        p2pVaultsStore = p2pVaultsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsStore, stakingYieldsStore, stakingBalancesStore, stakeKitApi)
    }

    @Test
    fun `fetch staking balances successfully`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val yields = listOf(MockYieldDTOFactory.create(tonId), MockYieldDTOFactory.create(solanaId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = tonAndSolanaIds.map(YieldBalanceRequestBodyFactory::create)
        val result = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId),
        )

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns ApiResponse.Success(result)

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(requests)
            stakingBalancesStore.storeActual(userWalletId = userWalletId, values = result)
        }

        coVerify(inverse = true) { stakingBalancesStore.storeError(any(), any()) }

        assertEitherRight(actual)
    }

    @Test
    fun `fetch staking balances successfully if one of stakingIds is unavailable`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val yields = listOf(MockYieldDTOFactory.create(tonId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = listOf(YieldBalanceRequestBodyFactory.create(tonId))
        val result = setOf(MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId))

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns ApiResponse.Success(result)

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakingBalancesStore.storeError(userWalletId = userWalletId, stakingIds = setOf(solanaId))
            stakeKitApi.getMultipleYieldBalances(requests)
            stakingBalancesStore.storeActual(userWalletId = userWalletId, values = result)
        }

        assertEitherRight(actual)
    }

    @Test
    fun `fetch staking balances failure if user wallet is not supported`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWallet = MockUserWalletFactory.create().copy(isMultiCurrency = false)
        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder { userWalletsStore.getSyncOrNull(params.userWalletId) }

        coVerify(inverse = true) {
            stakingBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            stakingBalancesStore.storeActual(userWalletId = any(), values = any())
            stakingBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${userWallet.toOption()}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if userWalletsStore returns null`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns null

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder { userWalletsStore.getSyncOrNull(params.userWalletId) }

        coVerify(inverse = true) {
            stakingBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            stakingBalancesStore.storeActual(userWalletId = any(), values = any())
            stakingBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${null.toOption()}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if stakingYieldsStore getSyncWithTimeout returns null`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns null

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(params.userWalletId, tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakingBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakingBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if stakingYieldsStore getSyncWithTimeout returns empty list`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakingBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakingBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if yields converting is failed`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val yields = listOf(
            MockYieldDTOFactory.create(tonId).copy(id = null),
            MockYieldDTOFactory.create(solanaId).copy(id = null),
        )
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakingBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakingBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if available yields does not contain ids from params`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val yields = listOf(MockYieldDTOFactory.create(StakingID(integrationId = "polygon", address = "0x1")))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakingBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakingBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException(
            """
                No available yields to fetch yield balances:
                 – userWalletId: $userWalletId
                 – stakingIds: ${tonAndSolanaIds.joinToString()}
            """.trimIndent(),
        )

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if stakeKitApi getMultipleYieldBalances is failed`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val yields = listOf(MockYieldDTOFactory.create(tonId), MockYieldDTOFactory.create(solanaId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = setOf(tonId, solanaId).map(YieldBalanceRequestBodyFactory::create)

        @Suppress("UNCHECKED_CAST")
        val errorResponse = ApiResponse.Error(ApiResponseError.NetworkException())
            as ApiResponse<Set<YieldBalanceWrapperDTO>>

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns errorResponse

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(requests)
            stakingBalancesStore.storeError(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)
        }

        coVerify(inverse = true) { stakingBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = ApiResponseError.NetworkException()

        assertEitherLeft(actual, expected)
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val userWallet = MockUserWalletFactory.create()

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )

        val tonAndSolanaIds = setOf(tonId, solanaId)
    }
}