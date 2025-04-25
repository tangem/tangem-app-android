package com.tangem.data.staking.single

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
import com.tangem.datasource.api.stakekit.models.request.YieldBalanceRequestBody
import com.tangem.datasource.api.stakekit.models.response.model.BalanceDTO
import com.tangem.datasource.api.stakekit.models.response.model.NetworkTypeDTO
import com.tangem.datasource.api.stakekit.models.response.model.TokenDTO
import com.tangem.datasource.api.stakekit.models.response.model.YieldBalanceWrapperDTO
import com.tangem.datasource.local.token.StakingYieldsStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
internal class DefaultSingleYieldBalanceFetcherTest {

    private val userWalletsStore: UserWalletsStore = mockk()
    private val stakingYieldsStore: StakingYieldsStore = mockk()
    private val yieldsBalancesStore: YieldsBalancesStore = mockk()
    private val stakingIdFactory: StakingIdFactory = mockk()
    private val stakeKitApi: StakeKitApi = mockk()

    private val fetcher = DefaultSingleYieldBalanceFetcher(
        userWalletsStore = userWalletsStore,
        stakingYieldsStore = stakingYieldsStore,
        yieldsBalancesStore = yieldsBalancesStore,
        stakingIdFactory = stakingIdFactory,
        stakeKitApi = stakeKitApi,
        dispatchers = TestingCoroutineDispatcherProvider(),
    )

    @Test
    fun `fetch yields balances successfully`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
        } just Runs

        val yields = listOf(MockYieldDTOFactory.create(tonId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val request = YieldBalanceRequestBodyFactory.create(tonId)
        val result = listOf(createBalanceDTO())
        coEvery { stakeKitApi.getSingleYieldBalance(tonId.integrationId, request) } returns ApiResponse.Success(result)

        val values = result.mapTo(hashSetOf()) { it.toWrapper(request) }
        coEvery { yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = values) } just Runs

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = tonId.integrationId, body = request)
            yieldsBalancesStore.storeActual(userWalletId = userWalletId, values = values)
        }

        coVerify(inverse = true) { yieldsBalancesStore.storeError(any(), any()) }

        Truth.assertThat(actual.isRight()).isTrue()
    }

    @Test
    fun `fetch yields balances failure if user wallet is not supported`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        val userWallet = MockUserWalletFactory.create().copy(isMultiCurrency = false)
        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet

        val actual = fetcher(params)

        coVerify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        coVerify(inverse = true) {
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
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
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns null

        val actual = fetcher(params)

        coVerify { userWalletsStore.getSyncOrNull(params.userWalletId) }

        coVerify(inverse = true) {
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
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
    fun `fetch yields balances failure if stakingIdFactory createForDefault returns null`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns null

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(userWalletId = userWalletId, currencyId = ton.id, network = ton.network)
        }

        coVerify(inverse = true) {
            yieldsBalancesStore.refresh(userWalletId = any(), stakingIds = any())
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
            yieldsBalancesStore.storeError(userWalletId = any(), stakingIds = any())
        }

        val expected = IllegalStateException("Unable to create staking ids for $params: list is empty")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakingYieldsStore getSyncWithTimeout returns null`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
        } just Runs

        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns null
        coEvery { yieldsBalancesStore.storeError(userWalletId, setOf(tonId)) } just Runs

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, setOf(tonId))
        }

        coVerify(inverse = true) {
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakingYieldsStore getSyncWithTimeout returns empty list`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
        } just Runs

        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns emptyList()
        coEvery { yieldsBalancesStore.storeError(userWalletId, setOf(tonId)) } just Runs

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, setOf(tonId))
        }

        coVerify(inverse = true) {
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if yields converting is failed`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
        } just Runs

        val yields = listOf(MockYieldDTOFactory.create(tonId).copy(id = null))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields
        coEvery { yieldsBalancesStore.storeError(userWalletId, setOf(tonId)) } just Runs

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, setOf(tonId))
        }

        coVerify(inverse = true) {
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException("No enabled yields for ${params.userWalletId}")

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if available yields does not contain ids from params`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
        } just Runs

        val yields = listOf(MockYieldDTOFactory.create(StakingID(integrationId = "polygon", address = "0x1")))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields
        coEvery { yieldsBalancesStore.storeError(userWalletId, setOf(tonId)) } just Runs

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
            stakingYieldsStore.getSyncWithTimeout()
            yieldsBalancesStore.storeError(userWalletId, setOf(tonId))
        }

        coVerify(inverse = true) {
            stakeKitApi.getSingleYieldBalance(integrationId = any(), body = any())
            yieldsBalancesStore.storeActual(userWalletId = any(), values = any())
        }

        val expected = IllegalStateException(
            """
                No available yields to fetch yield balances:
                 – userWalletId: $userWalletId
                 – stakingIds: ${setOf(tonId).joinToString()}
            """.trimIndent(),
        )

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    @Test
    fun `fetch yields balances failure if stakeKitApi getMultipleYieldBalances is failed`() = runTest {
        val params = YieldBalanceFetcherParams.Single(
            userWalletId = userWalletId,
            currencyId = ton.id,
            network = ton.network,
        )

        coEvery { userWalletsStore.getSyncOrNull(params.userWalletId) } returns userWallet
        coEvery { stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network) } returns tonId
        coEvery {
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
        } just Runs

        val yields = listOf(MockYieldDTOFactory.create(tonId))
        coEvery { stakingYieldsStore.getSyncWithTimeout() } returns yields

        val request = YieldBalanceRequestBodyFactory.create(tonId)

        @Suppress("UNCHECKED_CAST")
        val errorResponse = ApiResponse.Error(ApiResponseError.NetworkException) as ApiResponse<List<BalanceDTO>>

        coEvery { stakeKitApi.getSingleYieldBalance(tonId.integrationId, request) } returns errorResponse
        coEvery { yieldsBalancesStore.storeError(userWalletId, setOf(tonId)) } just Runs

        val actual = fetcher(params)

        coVerify {
            userWalletsStore.getSyncOrNull(params.userWalletId)
            stakingIdFactory.createForDefault(params.userWalletId, ton.id, ton.network)
            yieldsBalancesStore.refresh(userWalletId = params.userWalletId, stakingIds = setOf(tonId))
            stakingYieldsStore.getSyncWithTimeout()
            stakeKitApi.getSingleYieldBalance(tonId.integrationId, request)
            yieldsBalancesStore.storeError(userWalletId = userWalletId, stakingIds = setOf(tonId))
        }

        coVerify(inverse = true) { yieldsBalancesStore.storeActual(userWalletId = any(), values = any()) }

        val expected = ApiResponseError.NetworkException

        Truth.assertThat(actual.isLeft()).isTrue()
        Truth.assertThat(actual.leftOrNull()).isInstanceOf(expected::class.java)
        Truth.assertThat(actual.leftOrNull()).hasMessageThat().isEqualTo(expected.message)
    }

    private fun createBalanceDTO(): BalanceDTO {
        return BalanceDTO(
            groupId = "dictas",
            type = BalanceDTO.BalanceTypeDTO.REWARDS,
            amount = BigDecimal.ONE,
            date = null,
            pricePerShare = BigDecimal.ONE,
            pendingActions = listOf(),
            pendingActionConstraints = listOf(),
            tokenDTO = TokenDTO(
                name = "Casandra Paul",
                network = NetworkTypeDTO.POLYGON,
                symbol = "vim",
                decimals = 3994,
                address = null,
                coinGeckoId = null,
                logoURI = null,
                isPoints = null,
            ),
            validatorAddress = null,
            validatorAddresses = listOf(),
            providerId = null,
        )
    }

    private fun BalanceDTO.toWrapper(request: YieldBalanceRequestBody): YieldBalanceWrapperDTO {
        return YieldBalanceWrapperDTO(
            balances = listOf(this),
            integrationId = request.integrationId,
            addresses = request.addresses,
        )
    }

    private companion object {
        val userWalletId = UserWalletId("011")
        val userWallet = MockUserWalletFactory.create()

        val mocks = MockCryptoCurrencyFactory()

        val ton = mocks.createCoin(Blockchain.TON)

        val tonId = MockYieldBalanceWrapperDTOFactory.defaultStakingId
    }
}