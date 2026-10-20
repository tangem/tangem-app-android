package com.tangem.data.staking.multi

import arrow.core.toOption
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedStore
import com.tangem.data.staking.converters.ethpool.P2PEthPoolStakingAccountConverter
import com.tangem.data.staking.store.DefaultP2PEthPoolBalancesStore
import com.tangem.data.staking.store.P2PEthPoolBalancesStore
import com.tangem.data.staking.store.WalletIdWithP2PEthPoolResponses
import com.tangem.data.staking.store.WalletIdWithP2PStakingBalances
import com.tangem.data.staking.store.StakeKitBalancesStore
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.core.remote.response.ApiResponse
import com.tangem.core.remote.response.ApiResponseError
import com.tangem.grow.datasource.ethpool.P2PEthPoolApi
import com.tangem.grow.datasource.ethpool.models.request.P2PEthPoolAccountsListRequest
import com.tangem.grow.datasource.ethpool.models.response.*
import com.tangem.grow.datasource.stakekit.StakeKitApi
import com.tangem.grow.datasource.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.P2PEthPoolVaultsStore
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.staking.model.ethpool.P2PEthPoolVault
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.assertEitherLeft
import com.tangem.test.core.datastore.MockStateDataStore
import com.tangem.test.core.assertEitherRight
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
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
            p2PEthPoolBalancesStore,
            p2pEthPoolApi,
            p2pEthPoolVaultsStore,
        )
    }

    private fun stubWallet() {
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
    }

    @Suppress("UNCHECKED_CAST")
    private fun p2pHttpError() = ApiResponse.Error(ApiResponseError.NetworkException())
        as ApiResponse<P2PEthPoolResponse<P2PEthPoolAccountsListResponse>>

    @Test
    fun `GIVEN empty staking ids WHEN invoke THEN Right without any call`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = emptySet())

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        assertEitherRight(actual)
        verify(exactly = 0) { userWalletsListRepository.userWallets }
        coVerify(exactly = 0) { stakeKitBalancesStore.refresh(any(), any<Set<StakingID>>()) }
        coVerify(exactly = 0) { p2PEthPoolBalancesStore.refresh(any(), any<Set<StakingID>>()) }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class P2PEthPool {

        @Test
        fun `GIVEN no vaults WHEN invoke THEN empty stored for p2p ids`() = runTest {
            // Arrange
            stubWallet()
            coEvery { p2pEthPoolVaultsStore.getSync() } returns emptyList()
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerifyOrder {
                p2PEthPoolBalancesStore.refresh(userWalletId, setOf(p2pId1))
                p2pEthPoolVaultsStore.getSync()
                p2PEthPoolBalancesStore.storeEmpty(userWalletId, setOf(p2pId1))
            }
            coVerify(exactly = 0) { p2pEthPoolApi.getAccountsList(any(), any(), any()) }
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.storeError(any(), any()) }
            assertEitherRight(actual)
        }

        @Test
        fun `GIVEN vaults store throws WHEN invoke THEN treated as no vaults`() = runTest {
            // Arrange
            stubWallet()
            coEvery { p2pEthPoolVaultsStore.getSync() } throws IllegalStateException("db")
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerify(exactly = 1) { p2PEthPoolBalancesStore.storeEmpty(userWalletId, setOf(p2pId1)) }
            coVerify(exactly = 0) { p2pEthPoolApi.getAccountsList(any(), any(), any()) }
            assertEitherRight(actual)
        }

        // Pins current behaviour: a total P2P fetch failure is swallowed into storeError and the fetcher still returns Right
        @Test
        fun `GIVEN every vault request fails WHEN invoke THEN error stored for all ids and Right returned`() = runTest {
            // Arrange
            stubWallet()
            coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))
            coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns p2pHttpError()
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1, p2pId2))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerify { p2PEthPoolBalancesStore.storeError(userWalletId, setOf(p2pId1, p2pId2)) }
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.storeActual(any(), any()) }
            assertEitherRight(actual)
        }

        // Pins current behaviour: missing-id check ignores case, so no error is stored for the requested id
        @Test
        fun `GIVEN response delegator in different case WHEN invoke THEN no error stored for that id`() = runTest {
            // Arrange
            stubWallet()
            coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))
            coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
                accountsListSuccess(accountResponse(ADDR_UPPER, VAULT_A))
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pIdLower))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerify {
                p2PEthPoolBalancesStore.storeActual(userWalletId, setOf(accountResponse(ADDR_UPPER, VAULT_A)))
            }
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.storeError(any(), any()) }
            assertEitherRight(actual)
        }

        // Pins current behaviour: the balance is stored under the response casing, so the requested id stays CACHE
        @Test
        fun `GIVEN real store and response delegator in different case WHEN invoke THEN requested id stays CACHE`() =
            runTest {
                // Arrange
                val runtimeStore = RuntimeSharedStore<WalletIdWithP2PStakingBalances>()
                val realStore = DefaultP2PEthPoolBalancesStore(
                    runtimeStore = runtimeStore,
                    persistenceStore = MockStateDataStore<WalletIdWithP2PEthPoolResponses>(default = emptyMap()),
                    scope = TestAppCoroutineScope(),
                )
                val realStoreFetcher = DefaultMultiStakingBalanceFetcher(
                    userWalletsListRepository = userWalletsListRepository,
                    stakingYieldsStore = stakingYieldsStore,
                    stakeKitBalancesStore = stakeKitBalancesStore,
                    p2PEthPoolBalancesStore = realStore,
                    stakeKitApi = stakeKitApi,
                    p2pEthPoolApi = p2pEthPoolApi,
                    p2pEthPoolVaultsStore = p2pEthPoolVaultsStore,
                    dispatchers = TestingCoroutineDispatcherProvider(),
                )
                val previousResponse = accountResponse(ADDR_UPPER.lowercase(), VAULT_A)
                val freshResponse = accountResponse(ADDR_UPPER, VAULT_A)
                runtimeStore.store(
                    mapOf(userWalletId to setOf(p2pBalance(p2pIdLower, StatusSource.ACTUAL, previousResponse))),
                )
                stubWallet()
                coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))
                coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
                    accountsListSuccess(freshResponse)
                val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pIdLower))

                // Act
                val actual = realStoreFetcher.invoke(params)

                // Assert
                val upperId = StakingID(integrationId = p2pIdLower.integrationId, address = ADDR_UPPER)
                assertThat(realStore.getAllSyncOrNull(userWalletId)).containsExactly(
                    p2pBalance(p2pIdLower, StatusSource.CACHE, previousResponse),
                    p2pBalance(upperId, StatusSource.ACTUAL, freshResponse),
                )
                assertEitherRight(actual)
            }

        // Pins current behaviour: every non-StakeKit integration id, including unknown ones, goes to the P2P branch
        @Test
        fun `GIVEN unknown integration id WHEN invoke THEN routed to p2p fetch`() = runTest {
            // Arrange
            stubWallet()
            coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))
            coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
                accountsListSuccess(accountResponse(ADDR_1, VAULT_A))
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(unknownId))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerify { p2PEthPoolBalancesStore.refresh(userWalletId, setOf(unknownId)) }
            coVerify {
                p2pEthPoolApi.getAccountsList(any(), VAULT_A, P2PEthPoolAccountsListRequest(listOf(ADDR_1)))
            }
            coVerify(exactly = 0) { stakeKitBalancesStore.refresh(any(), any<Set<StakingID>>()) }
            coVerify(exactly = 0) { stakingYieldsStore.getSyncWithTimeout() }
            assertEitherRight(actual)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Mixed {

        @Test
        fun `GIVEN stakeKit and p2p ids WHEN invoke THEN each provider fetched and stored`() = runTest {
            // Arrange
            stubWallet()
            coEvery { stakingYieldsStore.getSyncWithTimeout() } returns listOf(MockYieldDTOFactory.create(tonId))
            val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId)
            coEvery {
                stakeKitApi.getMultipleYieldBalances(listOf(YieldBalanceRequestBodyFactory.create(tonId)))
            } returns ApiResponse.Success(setOf(tonWrapper))
            coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))
            coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
                accountsListSuccess(accountResponse(ADDR_1, VAULT_A))
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(tonId, p2pId1))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerify {
                stakeKitBalancesStore.refresh(userWalletId, setOf(tonId))
                stakeKitBalancesStore.storeActual(userWalletId, setOf(tonWrapper))
                p2PEthPoolBalancesStore.refresh(userWalletId, setOf(p2pId1))
                p2PEthPoolBalancesStore.storeActual(userWalletId, setOf(accountResponse(ADDR_1, VAULT_A)))
            }
            coVerify(exactly = 0) { stakeKitBalancesStore.storeError(any(), any()) }
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.storeError(any(), any()) }
            assertEitherRight(actual)
        }

        // Pins current behaviour: onLeft marks both providers as failed even if only StakeKit failed
        @Test
        fun `GIVEN stakeKit branch fails WHEN invoke with mixed ids THEN Left and p2p ids also marked as error`() =
            runTest {
                // Arrange
                stubWallet()
                coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()
                coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A))
                coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
                    accountsListSuccess(accountResponse(ADDR_1, VAULT_A))
                val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(tonId, p2pId1))

                // Act
                val actual = fetcher.invoke(params)

                // Assert
                assertEitherLeft(actual, IllegalStateException("No enabled yields for $userWalletId"))
                coVerify { stakeKitBalancesStore.storeError(userWalletId, setOf(tonId)) }
                coVerify { p2PEthPoolBalancesStore.storeError(userWalletId, setOf(p2pId1)) }
            }

        @Test
        fun `GIVEN only stakeKit ids and stakeKit branch fails WHEN invoke THEN p2p store untouched`() = runTest {
            // Arrange
            stubWallet()
            coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(tonId))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            assertEitherLeft(actual, IllegalStateException("No enabled yields for $userWalletId"))
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.refresh(any(), any<Set<StakingID>>()) }
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.storeError(any(), any()) }
        }

        @Test
        fun `GIVEN only stakeKit ids WHEN invoke THEN p2p fetch not started`() = runTest {
            // Arrange
            stubWallet()
            coEvery { stakingYieldsStore.getSyncWithTimeout() } returns listOf(MockYieldDTOFactory.create(tonId))
            val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId)
            coEvery { stakeKitApi.getMultipleYieldBalances(any()) } returns ApiResponse.Success(setOf(tonWrapper))
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(tonId))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            assertEitherRight(actual)
            coVerify { stakeKitBalancesStore.storeActual(userWalletId, setOf(tonWrapper)) }
            coVerify(exactly = 0) { p2PEthPoolBalancesStore.refresh(any(), any<Set<StakingID>>()) }
        }

        @Test
        fun `GIVEN only p2p ids and p2p branch fails WHEN invoke THEN stakeKit store untouched`() = runTest {
            // Arrange
            stubWallet()
            val failure = IllegalStateException("p2p refresh")
            coEvery { p2PEthPoolBalancesStore.refresh(any(), any<Set<StakingID>>()) } throws failure
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            assertEitherLeft(actual, failure)
            coVerify { p2PEthPoolBalancesStore.storeError(userWalletId, setOf(p2pId1)) }
            coVerify(exactly = 0) { stakeKitBalancesStore.refresh(any(), any<Set<StakingID>>()) }
            coVerify(exactly = 0) { stakeKitBalancesStore.storeError(any(), any()) }
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StakeKitPartial {

        @Test
        fun `GIVEN stakeKit returns only some balances WHEN invoke THEN error stored for missing ids`() = runTest {
            // Arrange
            stubWallet()
            coEvery { stakingYieldsStore.getSyncWithTimeout() } returns
                listOf(MockYieldDTOFactory.create(tonId), MockYieldDTOFactory.create(solanaId))
            val tonWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId)
            coEvery { stakeKitApi.getMultipleYieldBalances(any()) } returns ApiResponse.Success(setOf(tonWrapper))
            val params = MultiStakingBalanceFetcher.Params(userWalletId, tonAndSolanaIds)

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerifyOrder {
                stakeKitBalancesStore.storeActual(userWalletId, setOf(tonWrapper))
                stakeKitBalancesStore.storeError(userWalletId, setOf(solanaId))
            }
            assertEitherRight(actual)
        }

        @Test
        fun `GIVEN stakeKit response with other address WHEN invoke THEN requested id marked as error`() = runTest {
            // Arrange
            stubWallet()
            coEvery { stakingYieldsStore.getSyncWithTimeout() } returns listOf(MockYieldDTOFactory.create(tonId))
            val otherAddressWrapper = MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId.copy(address = "0x2"))
            coEvery { stakeKitApi.getMultipleYieldBalances(any()) } returns
                ApiResponse.Success(setOf(otherAddressWrapper))
            val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(tonId))

            // Act
            val actual = fetcher.invoke(params)

            // Assert
            coVerify { stakeKitBalancesStore.storeActual(userWalletId, setOf(otherAddressWrapper)) }
            coVerify { stakeKitBalancesStore.storeError(userWalletId, setOf(tonId)) }
            assertEitherRight(actual)
        }
    }

    @Test
    fun `GIVEN two available stakeKit ids WHEN invoke THEN balances stored as actual`() = runTest {
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

        // Act
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
    fun `GIVEN more than 15 stakeKit integrations WHEN fetch THEN requests are chunked by 15`() = runTest {
        // Arrange
        // Regression for [REDACTED_TASK_KEY]: StakeKit rejects a single POST /yields/balances carrying
        // more than 15 integrations ("You can only submit maximum of 15 integrations"), so the
        // request must be split into chunks of at most 15.
        val integrationId = solanaId.integrationId
        val stakingIds = (1..18).mapTo(mutableSetOf()) { index ->
            StakingID(integrationId = integrationId, address = "0x$index")
        }
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = stakingIds)

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns listOf(MockYieldDTOFactory.create(solanaId))
        coEvery { stakeKitApi.getMultipleYieldBalances(any()) } returns ApiResponse.Success(emptySet())

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerify(exactly = 1) { stakeKitApi.getMultipleYieldBalances(match { it.size == 15 }) }
        coVerify(exactly = 1) { stakeKitApi.getMultipleYieldBalances(match { it.size == 3 }) }
        coVerify(inverse = true) { stakeKitApi.getMultipleYieldBalances(match { it.size > 15 }) }

        assertEitherRight(actual)
    }

    @Test
    fun `GIVEN one stakeKit id without yield WHEN invoke THEN error stored for it and the rest fetched`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val yields = listOf(MockYieldDTOFactory.create(tonId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = listOf(YieldBalanceRequestBodyFactory.create(tonId))
        val result = setOf(MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId))

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns ApiResponse.Success(result)

        // Act
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
    fun `GIVEN single currency wallet WHEN invoke THEN returns not supported Left`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWallet = MockUserWalletFactory.create().copy(isMultiCurrency = false)
        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder { userWalletsListRepository.userWallets }

        coVerify(exactly = 0) { stakeKitBalancesStore.refresh(userWalletId = any(), stakingIds = any()) }
        coVerify(exactly = 0) { stakingYieldsStore.getSyncWithTimeout() }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeError(userWalletId = any(), stakingIds = any()) }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${userWallet.toOption()}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `GIVEN wallets list is null WHEN invoke THEN returns not supported Left`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(null)
        coEvery { userWalletsListRepository.userWallets } returns userWalletsFlow

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder { userWalletsListRepository.userWallets }

        coVerify(exactly = 0) { stakeKitBalancesStore.refresh(userWalletId = any(), stakingIds = any()) }
        coVerify(exactly = 0) { stakingYieldsStore.getSyncWithTimeout() }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeError(userWalletId = any(), stakingIds = any()) }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${null.toOption()}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `GIVEN yields store returns null WHEN invoke THEN error stored and Left returned`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns null

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(params.userWalletId, tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(exactly = 0) { stakeKitApi.getMultipleYieldBalances(any()) }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `GIVEN yields store returns empty list WHEN invoke THEN error stored and Left returned`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(exactly = 0) { stakeKitApi.getMultipleYieldBalances(any()) }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `GIVEN yields without ids WHEN invoke THEN error stored and Left returned`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val yields = listOf(
            MockYieldDTOFactory.create(tonId).copy(id = null),
            MockYieldDTOFactory.create(solanaId).copy(id = null),
        )
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(exactly = 0) { stakeKitApi.getMultipleYieldBalances(any()) }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        assertEitherLeft(actual, expected)
    }

    @Test
    fun `GIVEN yields not matching requested ids WHEN invoke THEN error stored and Left returned`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)

        val userWalletsFlow = MutableStateFlow(listOf(userWallet))

        every { userWalletsListRepository.userWallets } returns userWalletsFlow

        val yields = listOf(MockYieldDTOFactory.create(StakingID(integrationId = "polygon", address = "0x1")))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerifyOrder {
            userWalletsListRepository.userWallets
            stakeKitBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(exactly = 0) { stakeKitApi.getMultipleYieldBalances(any()) }
        coVerify(exactly = 0) { stakeKitBalancesStore.storeActual(userWalletId = any(), values = any()) }

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
    fun `GIVEN stakeKit api error WHEN invoke THEN error stored and Left returned`() = runTest {
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

        // Act
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
    fun `GIVEN two vaults WHEN invoke with p2p ids THEN one batch request per vault and actual stored`() = runTest {
        // Arrange
        val params = MultiStakingBalanceFetcher.Params(userWalletId, setOf(p2pId1, p2pId2))

        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { p2pEthPoolVaultsStore.getSync() } returns listOf(vault(VAULT_A), vault(VAULT_B))

        coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_A, any()) } returns
            accountsListSuccess(accountResponse(ADDR_1, VAULT_A))
        coEvery { p2pEthPoolApi.getAccountsList(any(), VAULT_B, any()) } returns
            accountsListSuccess(accountResponse(ADDR_2, VAULT_B))

        // Act
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
        coVerify {
            p2PEthPoolBalancesStore.storeActual(
                userWalletId = userWalletId,
                values = setOf(accountResponse(ADDR_1, VAULT_A), accountResponse(ADDR_2, VAULT_B)),
            )
        }

        assertEitherRight(actual)
    }

    @Test
    fun `GIVEN per item error in batch WHEN invoke THEN error stored for that p2p id`() = runTest {
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

        // Act
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            p2PEthPoolBalancesStore.storeActual(
                userWalletId = userWalletId,
                values = setOf(accountResponse(ADDR_1, VAULT_A)),
            )
        }
        coVerify { p2PEthPoolBalancesStore.storeError(userWalletId = userWalletId, stakingIds = setOf(p2pId2)) }

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

        const val ADDR_UPPER = "0xABCDEFABCDEFABCDEFABCDEFABCDEFABCDEFABCD"

        val p2pId1 = StakingID(integrationId = "p2p-ethereum-pooled", address = ADDR_1)
        val p2pId2 = StakingID(integrationId = "p2p-ethereum-pooled", address = ADDR_2)
        val p2pIdLower = StakingID(integrationId = "p2p-ethereum-pooled", address = ADDR_UPPER.lowercase())
        val unknownId = StakingID(integrationId = "unknown-integration", address = ADDR_1)

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

        fun p2pBalance(id: StakingID, source: StatusSource, vararg responses: P2PEthPoolAccountResponse) =
            StakingBalance.Data.P2PEthPool(
                stakingId = id,
                source = source,
                accounts = responses.map(P2PEthPoolStakingAccountConverter::convert),
            )

        fun accountsListSuccess(vararg accounts: P2PEthPoolAccountResponse) = ApiResponse.Success(
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