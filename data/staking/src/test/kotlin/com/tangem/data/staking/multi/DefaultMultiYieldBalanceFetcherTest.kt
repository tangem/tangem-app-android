package com.tangem.data.staking.multi

import arrow.core.toOption
import com.google.common.truth.Truth
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.test.data.staking.MockYieldBalanceWrapperDTOFactory
import com.tangem.common.test.data.staking.MockYieldDTOFactory
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.staking.store.YieldsBalancesStore
import com.tangem.data.staking.utils.StakingIdFactory
import com.tangem.data.staking.utils.YieldBalanceRequestBodyFactory
import com.tangem.datasource.api.common.response.ApiResponse
import com.tangem.datasource.api.common.response.ApiResponseError
import com.tangem.datasource.api.stakekit.StakeKitApi
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.multi.MultiYieldBalanceFetcher
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
internal class DefaultMultiYieldBalanceFetcherTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val yieldsBalancesStore: YieldsBalancesStore = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val stakeKitApi: StakeKitApi = mockk()

    private val fetcher = DefaultMultiYieldBalanceFetcher(
        userWalletsStore = userWalletsStore,
        stakingYieldsStore = stakingYieldsStore,
        yieldsBalancesStore = yieldsBalancesStore,
        stakingIdFactory = stakingIdFactory,
        stakeKitApi = stakeKitApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(userWalletsStore, stakingYieldsStore, yieldsBalancesStore, stakingIdFactory, stakeKitApi)
    }

    @Test
    fun `fetch yields balances successfully`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs

        val yields = listOf(MockYieldDTOFactory.create(tonId), MockYieldDTOFactory.create(solanaId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = tonAndSolanaIds.map(YieldBalanceRequestBodyFactory::create).sortedBy { it.integrationId }
        val result = setOf(
            MockYieldBalanceWrapperDTOFactory.createWithBalance(solanaId),
            MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId),
        )
        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns ApiResponse.Success(result)
        coEvery { yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = result) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(requests)
            yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = result)
        }

        coVerify(inverse = true) { yieldsBalancesStore.storeError(any(), any()) }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch yields balances successfully if one of stakingIds is unavailable`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs

        val yields = listOf(MockYieldDTOFactory.create(tonId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        coEvery { yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = setOf(solanaId)) } just Runs

        val requests = listOf(YieldBalanceRequestBodyFactory.create(tonId))
        val result = setOf(MockYieldBalanceWrapperDTOFactory.createWithBalance(tonId))

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns ApiResponse.Success(result)
        coEvery { yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = result) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = setOf(solanaId))
            stakeKitApi.getMultipleYieldBalances(requests)
            yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = result)
        }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch yields balances failure if user wallet is not supported`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        val userWallet = MockUserWalletFactory.create().copy(isMultiCurrency = false)
        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        coVerify(inverse = true) {
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
            yieldsBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${userWallet.toOption()}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if userWalletsStore returns null`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns null

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        coVerify(inverse = true) {
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
            yieldsBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Wallet ${params.userWalletId} is not supported: ${null.toOption()}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakingIdFactory returns null`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns null
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns null

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
        }

        coVerify(inverse = true) {
            yieldsBalancesStore.refresh(any(), any<Set<StakingID>>())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(any())
            yieldsBalancesStore.storeActual(any(), any())
            yieldsBalancesStore.storeError(any(), any())
        }

        val expected = IllegalStateException("Unable to create staking ids for $params: list is empty")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakingYieldsStore getSyncWithTimeout returns null`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns null
        coEvery { yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakingYieldsStore getSyncWithTimeout returns empty list`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()
        coEvery { yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if yields converting is failed`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs

        val yields = listOf(
            MockYieldDTOFactory.create(tonId).copy(id = null),
            MockYieldDTOFactory.create(solanaId).copy(id = null),
        )
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields
        coEvery { yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if available yields does not contain ids from params`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs

        val yields = listOf(MockYieldDTOFactory.create(StakingID(integrationId = "polygon", address = "0x1")))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields
        coEvery { yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds)
        }

        coVerify(inverse = true) {
            stakeKitApi.getMultipleYieldBalances(any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException(
            """
                No available yields to fetch yield balances:
                 – userWalletId: $userWalletId
                 – stakingIds: ${setOf(solanaId, tonId).joinToString()}
            """.trimIndent(),
        )

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakeKitApi getMultipleYieldBalances is failed`() = runTest {
        // Arrange
        val currencyIdWithNetworkMap = mapOf(ton.id to ton.network, solana.id to solana.network)

        val params = MultiYieldBalanceFetcher.Params(userWalletId, currencyIdWithNetworkMap)

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.create(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery { stakingIdFactory.create(params.userWalletId, solana.id, solana.network) } returns solanaId
        coEvery { yieldsBalancesStore.refresh(params.userWalletId, tonAndSolanaIds) } just Runs

        val yields = listOf(MockYieldDTOFactory.create(tonId), MockYieldDTOFactory.create(solanaId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val requests = setOf(solanaId, tonId).map(YieldBalanceRequestBodyFactory::create)

        @Suppress("UNCHECKED_CAST")
        val errorResponse = ApiResponse.Error(ApiResponseError.NetworkException)
            as ApiResponse<Set<YieldBalanceWrapperDTO>>

        coEvery { stakeKitApi.getMultipleYieldBalances(requests) } returns errorResponse
        coEvery { yieldsBalancesStore.storeError(userWalletId, tonAndSolanaIds) } just Runs

        // Actual
        val actual = fetcher.invoke(params)

        // Assert
        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.create(params.userWalletId, ton.id, ton.network)
            stakingIdFactory.create(params.userWalletId, solana.id, solana.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = tonAndSolanaIds)
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getMultipleYieldBalances(requests)
            yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = tonAndSolanaIds)
        }

        coVerify(inverse = true) { yieldsBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = ApiResponseError.NetworkException

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val userWallet = MockUserWalletFactory.create()

        val mocks = MockCryptoCurrencyFactory()

        val ton = mocks.createCoin(Blockchain.TON)
        val solana = mocks.createCoin(Blockchain.Solana)

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
        val solanaId = StakingID(
            integrationId = "solana-sol-native-multivalidator-staking",
            address = "0x1",
        )

        val tonAndSolanaIds = setOf(tonId, solanaId)
    }
}