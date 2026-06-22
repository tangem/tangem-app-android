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
import com.tangem.domain.wallets.extension.hasDerivation
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

private const val USER_WALLET_EXTENSIONS = "com.tangem.domain.wallets.extension.UserWalletExtensionsKt"

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
        mockkStatic(USER_WALLET_EXTENSIONS)
        clearMocks(networkFactory, tangemPayCurrencyFactory, singleNetworkStatusFetcher, userWalletsListRepository)
        every { userWalletsListRepository.userWallets } returns MutableStateFlow(listOf(userWallet))
        every {
            networkFactory.create(any<Blockchain>(), any<Network.DerivationPath>(), any<UserWallet>())
        } returns network
        every { tangemPayCurrencyFactory.createVirtualAccountToken(userWalletId) } returns token
        coEvery { singleNetworkStatusFetcher(any()) } returns Unit.right()
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(USER_WALLET_EXTENSIONS)
    }

    @Test
    fun `GIVEN VA derivation missing WHEN invoke THEN on-chain fetch skipped`() = runTest {
        // Arrange
        every { userWallet.hasDerivation(any(), any()) } returns false

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 0) { singleNetworkStatusFetcher(any()) }
    }

    @Test
    fun `GIVEN VA derivation present WHEN invoke THEN on-chain fetch performed`() = runTest {
        // Arrange
        every { userWallet.hasDerivation(any(), any()) } returns true

        // Act
        fetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId))

        // Assert
        coVerify(exactly = 1) { singleNetworkStatusFetcher(any()) }
    }
}