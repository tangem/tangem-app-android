package com.tangem.data.staking.multi

import arrow.core.toOption
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.ethpool.P2PEthPoolApi
import com.tangem.datasource.api.ethpool.models.request.P2PEthPoolAccountsListRequest
import com.tangem.datasource.api.ethpool.models.response.*
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.test.core.assertEitherLeft
import com.tangem.test.core.assertEitherRight
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMultiStakingBalanceFetcherTest {

    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val stakeKitBalancesStore: StakeKitBalancesStore = mockk(relaxUnitFun = true)
    private val p2PEthPoolBalancesStore: P2PEthPoolBalancesStore = mockk(relaxUnitFun = true)
    private val stakeKitApi: StakeKitApi = mockk()
    private val p2pEthPoolApi: P2PEthPoolApi = mockk()
    private val p2pEthPoolVaultsStore: P2PEthPoolVaultsStore = mockk()

    private val fetcher = DefaultMultiStakingBalanceFetcher(
        userWalletsListRepository = userWalletsListRepository,
        stakingYieldsStore = stakingYieldsStore,
        stakeKitBalancesStore = stakeKitBalancesStore,
        p2PEthPoolBalancesStore = p2PEthPoolBalancesStore,
        stakeKitApi = stakeKitApi,
        p2pEthPoolApi = p2pEthPoolApi,
        p2pEthPoolVaultsStore = p2pEthPoolVaultsStore,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            userWalletsListRepository,
            stakingYieldsStore,
            stakeKitBalancesStore,
            stakeKitApi,
            p2pEthPoolApi,
            p2pEthPoolVaultsStore,
        )
    }

    @Test
    fun `fetch staking balances successfully`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

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
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(requests)
            stakeKitBalancesStore.storeActual(userWalletId = userWalletId, values = result)
        }

        coVerify(inverse = true) { stakeKitBalancesStore.storeError(any(), any()) }

        assertEitherRight(actual)
    }

    @Test
    fun `fetch staking balances successfully if one of stakingIds is unavailable`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val yields = listOf(MockYieldDTOFactory.create(tonId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = listOf(YieldBalanceRequestBodyFactory.create(tonId))
        val result = setOf(MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId))

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns ApiResponse.Success(result)

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId = userWalletId, stakingIds = setOf(solanaId))
            stakeKitApi.getMultipleYieldBalances(requests)
            stakeKitBalancesStore.storeActual(userWalletId = userWalletId, values = result)
        }

        assertEitherRight(actual)
    }

    @Test
    fun `fetch staking balances failure if user wallet is not supported`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWallet = MockUserWalletFactory.create().copy(isMultiCurrency = false)
        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder { userWalletsListRepository.userWallets }

        coVerify(inverse = true) {
            stakeKitBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            stakeKitBalancesStore.storeActual(userWalletId = any(), values = any())
            stakeKitBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${userWallet.toOption()}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if userWalletsStore returns null`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(null)
        coEvery { userWalletsListRepository.userWallets } returns userWalletsFlow

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder { userWalletsListRepository.userWallets }

        coVerify(inverse = true) {
            stakeKitBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            stakeKitBalancesStore.storeActual(userWalletId = any(), values = any())
            stakeKitBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${null.toOption()}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if stakingYieldsStore getSyncWithTimeout returns null`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns null

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(params.userWalletId, tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakeKitBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if stakingYieldsStore getSyncWithTimeout returns empty list`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakeKitBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if yields converting is failed`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val yields = listOf(
            MockYieldDTOFactory.create(tonId).copy(id = null),
            MockYieldDTOFactory.create(solanaId).copy(id = null),
        )
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakeKitBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch staking balances failure if available yields does not contain ids from params`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val yields = listOf(MockYieldDTOFactory.create(StakingID(integrationId = "polygon", address = "0x1")))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            stakeKitBalancesStore.storeActual(userWalletId = any(), values = any())
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

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

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
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(requests)
            stakeKitBalancesStore.storeError(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)
        }

        coVerify(inverse = true) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = ApiResponseError.NetworkException()

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `fetch P2P balances via batch endpoint`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1, p2pId2))

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A), vault(VAULT_B))

        coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
            accountsListSuccess(accountResponse(ADDR_1, VAULT_A))
        coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_B, any()) } returns
            accountsListSuccess(accountResponse(ADDR_2, VAULT_B))

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify(exactly = 1) {
            p2pEthPoolApi.getAccountsList(
                network = any(),
                vaultAddress = VAULT_A,
                body = match { it.delegatorAddresses.containsAll(listOf(ADDR_1, ADDR_2)) },
            )
        }
        coVerify(exactly = 1) {
            p2pEthPoolApi.getAccountsList(network = any(), vaultAddress = VAULT_B, body = any())
        }
        coVerify(inverse = true) { p2pEthPoolApi.getAccountInfo(any(), any(), any()) }
        coVerify { p2PEthPoolBalancesStore.storeActual(userWalletId = userWalletId, values = any()) }

        assertEitherRight(actual)
    }

    @Test
    fun `fetch P2P batch maps per-item error to missing stakingId`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1, p2pId2))

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))

        coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
            ApiResponse.Success(
                P2PEthPoolResponse(
                    error = null,
                    result = P2PEthPoolAccountsListResponse(
                        list = listOf(
                            P2PEthPoolAccountListItem(
                                delegatorAddress = ADDR_1,
                                account = accountResponse(ADDR_1, VAULT_A),
                                error = null,
                            ),
                            P2PEthPoolAccountListItem(
                                delegatorAddress = ADDR_2,
                                account = null,
                                error = P2PEthPoolErrorDetailsDTO(
                                    code = 127108,
                                    message = "invalid",
                                    name = null,
                                    errors = null,
                                ),
                            ),
                        ),
                    ),
                ),
            )

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify { p2PEthPoolBalancesStore.storeActual(userWalletId = userWalletId, values = any()) }
        coVerify {
            p2PEthPoolBalancesStore.storeError(
                userWalletId = userWalletId,
                stakingIds = match { it == setOf(p2pId2) },
            )
        }

        assertEitherRight(actual)
    }

    private companion object {
        val userWallet = MockUserWalletFactory.create()
        val userWalletId = userWallet.walletId

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )

        val tonAndSolanaIds = setOf(tonId, solanaId)

        const val ADDR_1 = "0x1111111111111111111111111111111111111111"
        const val ADDR_2 = "0x2222222222222222222222222222222222222222"
        const val VAULT_A = "0xVaultAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"
        const val VAULT_B = "0xVaultBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"

        val p2pId1 = StakingID(integrationId = "p2p-ethereum-pooled", address = ADDR_1)
        val p2pId2 = StakingID(integrationId = "p2p-ethereum-pooled", address = ADDR_2)

        fun vault(address: String) = P2PEthPoolVault(
            vaultAddress = address,
            displayName = "Vault",
            apy = BigDecimal("4.5"),
            baseApy = BigDecimal("4.0"),
            capacity = BigDecimal("1000"),
            totalAssets = BigDecimal("100"),
            feePercent = BigDecimal("10"),
            isPrivate = false,
            isGenesis = false,
            isSmoothingPool = true,
            isErc20 = false,
            tokenName = null,
            tokenSymbol = null,
            createdAt = 0L,
        )

        fun accountResponse(address: String, vaultAddress: String) = P2PEthPoolAccountResponse(
            delegatorAddress = address,
            vaultAddress = vaultAddress,
            stake = P2PEthPoolStakeDTO(assets = BigDecimal("1.5"), totalEarnedAssets = BigDecimal("0.1")),
            availableToUnstake = BigDecimal.ZERO,
            availableToWithdraw = BigDecimal.ZERO,
            exitQueue = P2PEthPoolExitQueueDTO(total = BigDecimal.ZERO, requests = emptyList()),
        )

        fun accountsListSuccess(vararg accounts: P2PEthPoolAccountResponse) =
            ApiResponse.Success(
                P2PEthPoolResponse(
                    error = null,
                    result = P2PEthPoolAccountsListResponse(
                        list = accounts.map {
                            P2PEthPoolAccountListItem(
                                delegatorAddress = it.delegatorAddress,
                                account = it,
                                error = null,
                            )
                        },
                    ),
                ),
            )
    }
}