package com.tangem.data.virtualaccount.flow

import arrow.core.right
import com.tangem.blockchain.common.Blockchain
import com.tangem.data.common.network.NetworkFactory
import com.tangem.data.virtualaccount.store.VirtualAccountStatusesStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.single.SingleNetworkStatusFetcher
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultVirtualAccountStatusFetcherTest {

    private val virtualAccountStatusesStore: VirtualAccountStatusesStore = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val networkFactory: NetworkFactory = mockk()
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory = mockk()
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val singleNetworkStatusFetcher: SingleNetworkStatusFetcher = mockk()
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier = mockk(relaxed = true)

    private val fetcher = DefaultVirtualAccountStatusFetcher(
        virtualAccountStatusesStore = virtualAccountStatusesStore,
        dispatchers = dispatchers,
        networkFactory = networkFactory,
        tangemPayCurrencyFactory = tangemPayCurrencyFactory,
        userWalletsListRepository = userWalletsListRepository,
        singleNetworkStatusFetcher = singleNetworkStatusFetcher,
        singleNetworkStatusSupplier = singleNetworkStatusSupplier,
    )

    private val userWalletId = UserWalletId("011")
    private val userWallet: UserWallet = mockk { every { walletId } returns userWalletId }
    private val network: Network = mockk()
    private val token: CryptoCurrency.Token = mockk()

    @BeforeEach
    fun setUp() {
        clearMocks(networkFactory, tangemPayCurrencyFactory, singleNetworkStatusFetcher, userWalletsListRepository)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every { tangemPayCurrencyFactory.createVirtualAccountToken(userWalletId) } returns token
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
    }

    @Test
    fun `GIVEN network created WHEN invoke THEN on-chain status fetched with VA token`() = runTest {
        // Arrange
        every {
            networkFactory.create(any<Blockchain>(), any<Network.DerivationPath>(), any<UserWallet>())
        } returns network

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) {
            singleNetworkStatusFetcher(
                SingleNetworkStatusFetcher.Params(
                    userWalletId = userWalletId,
                    network = network,
                    extraTokens = setOf(token),
                ),
            )
        }
    }

    @Test
    fun `GIVEN network cannot be created WHEN invoke THEN on-chain fetch skipped`() = runTest {
        // Arrange
        every {
            networkFactory.create(any<Blockchain>(), any<Network.DerivationPath>(), any<UserWallet>())
        } returns null

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
    }
}