package com.tangem.data.managetokens

import com.google.common.truth.Truth.assertThat
import com.tangem.blockchainsdk.utils.ExcludedBlockchains
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.data.common.account.SupportedAccountTypes
import com.tangem.data.common.account.WalletAccountsFetcher
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.managetokens.testnet.TestnetTokensStorage
import com.tangem.core.remote.response.ApiResponse
import com.tangem.datasource.api.tangemTech.TangemTechApi
import com.tangem.datasource.api.tangemTech.models.CoinsResponse
import com.tangem.datasource.api.tangemTech.models.account.GetWalletAccountsResponse
import com.tangem.datasource.api.tangemTech.models.account.WalletAccountDTO
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.managetokens.model.ManageTokensListBatchFlow
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.managetokens.model.ManageTokensUpdateAction
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.pagination.BatchAction
import com.tangem.pagination.BatchingContext
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultManageTokensRepositoryPagingTest {

    private val tangemTechApi: TangemTechApi = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val walletAccountsFetcher: WalletAccountsFetcher = mockk()
    private val supportedAccountTypes: SupportedAccountTypes = mockk()
    private val userWallet = MockUserWalletFactory.create()
    private val accountId = AccountId.forCryptoPortfolio(userWallet.walletId, DerivationIndex.Main)

    @Test
    fun `GIVEN a coin returned on two pages WHEN load more THEN the second batch drops it`() = runTest {
        // Arrange
        stubCoinsPage(offset = 0, ids = listOf("tether", "zenith-protocol"))
        stubCoinsPage(offset = 2, ids = listOf("zenith-protocol", "dai"))
        val (actions, batchFlow, scope) = createBatchFlow()

        // Act
        actions.emit(BatchAction.Reload(requestParams = config()))
        advanceUntilIdle()
        actions.emit(BatchAction.LoadMore())
        advanceUntilIdle()

        // Assert
        val ids = batchFlow.state.value.data.map { batch -> batch.data.map { it.id.value } }
        assertThat(ids).containsExactly(listOf("tether", "zenith-protocol"), listOf("dai")).inOrder()
        scope.cancel()
    }

    @Test
    fun `GIVEN a full page of repeats WHEN load more THEN the next page is loaded in its place`() = runTest {
        // Arrange
        stubCoinsPage(offset = 0, ids = listOf("tether", "dai"))
        stubCoinsPage(offset = 2, ids = listOf("dai", "tether"))
        stubCoinsPage(offset = 4, ids = listOf("zenith-protocol", "usd-coin"))
        val (actions, batchFlow, scope) = createBatchFlow()

        // Act
        actions.emit(BatchAction.Reload(requestParams = config()))
        advanceUntilIdle()
        actions.emit(BatchAction.LoadMore())
        advanceUntilIdle()

        // Assert
        val ids = batchFlow.state.value.data.map { batch -> batch.data.map { it.id.value } }
        assertThat(ids).containsExactly(listOf("tether", "dai"), listOf("zenith-protocol", "usd-coin")).inOrder()
        scope.cancel()
    }

    @Test
    fun `GIVEN loaded pages WHEN the list is reloaded THEN the first page is shown in full again`() = runTest {
        // Arrange
        stubCoinsPage(offset = 0, ids = listOf("tether", "dai"))
        stubCoinsPage(offset = 2, ids = listOf("zenith-protocol", "usd-coin"))
        val (actions, batchFlow, scope) = createBatchFlow()
        actions.emit(BatchAction.Reload(requestParams = config()))
        advanceUntilIdle()
        actions.emit(BatchAction.LoadMore())
        advanceUntilIdle()

        // Act
        actions.emit(BatchAction.Reload(requestParams = config()))
        advanceUntilIdle()

        // Assert
        val ids = batchFlow.state.value.data.map { batch -> batch.data.map { it.id.value } }
        assertThat(ids).containsExactly(listOf("tether", "dai"))
        scope.cancel()
    }

    private fun TestScope.createBatchFlow(): Triple<
        MutableSharedFlow<BatchAction<Int, ManageTokensListConfig, ManageTokensUpdateAction>>,
        ManageTokensListBatchFlow,
        CoroutineScope,
        > {
        val dispatcher = StandardTestDispatcher(testScheduler)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        coEvery { walletAccountsFetcher.getSaved(userWallet.walletId) } returns accountsResponse()
        every { supportedAccountTypes.isSupported(any<WalletAccountDTO>()) } returns true

        val repository = DefaultManageTokensRepository(
            tangemTechApi = tangemTechApi,
            userWalletsListRepository = userWalletsListRepository,
            manageTokensUpdateFetcher = mockk(relaxed = true),
            testnetTokensStorage = mockk<TestnetTokensStorage>(),
            excludedBlockchains = ExcludedBlockchains(),
            dispatchers = TestingCoroutineDispatcherProvider(
                main = dispatcher,
                mainImmediate = dispatcher,
                io = dispatcher,
                default = dispatcher,
                single = dispatcher,
            ),
            walletAccountsFetcher = walletAccountsFetcher,
            supportedAccountTypes = supportedAccountTypes,
            networkFactory = NetworkFactory(ExcludedBlockchains()),
        )
        val actions = MutableSharedFlow<BatchAction<Int, ManageTokensListConfig, ManageTokensUpdateAction>>(replay = 1)
        // not backgroundScope: advanceUntilIdle does not run its tasks, and the source's collectors never complete
        val scope = CoroutineScope(coroutineContext + Job())
        val batchFlow = repository.getTokenListBatchFlow(
            context = BatchingContext(actionsFlow = actions, coroutineScope = scope),
            loadUserTokensFromRemote = false,
            batchSize = PAGE_SIZE,
        )
        return Triple(actions, batchFlow, scope)
    }

    private fun config() = ManageTokensListConfig(accountId = accountId, searchText = null)

    private fun stubCoinsPage(offset: Int, ids: List<String>) {
        coEvery {
            tangemTechApi.getCoins(
                cacheControl = any(),
                contractAddress = any(),
                contractAddresses = any(),
                exchangeable = any(),
                networkIds = any(),
                networkId = any(),
                active = any(),
                searchText = any(),
                offset = offset,
                limit = any(),
            )
        } returns ApiResponse.Success(CoinsResponse(imageHost = null, coins = ids.map(::coin), total = 100))
    }

    private fun coin(id: String) = CoinsResponse.Coin(
        id = id,
        name = id,
        symbol = id.uppercase(),
        active = true,
        networks = listOf(
            CoinsResponse.Coin.Network(
                networkId = "ethereum",
                contractAddress = "0x$id",
                decimalCount = BigDecimal(DECIMALS),
            ),
        ),
    )

    private fun accountsResponse(): GetWalletAccountsResponse {
        val account = WalletAccountDTO(
            id = accountId.value,
            name = null,
            derivationIndex = 0,
            icon = "",
            iconColor = "",
            type = WalletAccountDTO.Type.CRYPTO,
            tokens = emptyList(),
        )
        return GetWalletAccountsResponse(
            wallet = GetWalletAccountsResponse.Wallet(
                group = null,
                sort = null,
                totalAccounts = 1,
                totalArchivedAccounts = 0,
            ),
            accounts = listOf(account),
            unassignedTokens = emptyList(),
        )
    }

    private companion object {
        const val PAGE_SIZE = 2
        const val DECIMALS = 6
    }
}