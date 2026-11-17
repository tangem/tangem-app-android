package com.tangem.features.earn.impl.deeplink

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.routing.AppRoute
import com.tangem.common.routing.deeplink.DeeplinkConst.EARN_PRODUCT_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.test.domain.token.MockCryptoCurrencyFactory
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.deeplink.DeepLinkRequest
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.impl.overview.EarnAvailability
import com.tangem.features.earn.impl.overview.EarnProductsRegistry
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnDeepLinkHandlerTest {

    private val router: Router = mockk(relaxed = true)
    private val userWalletsListRepository: UserWalletsListRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()
    private val earnAvailability: EarnAvailability = mockk()
    private val productsRegistry: EarnProductsRegistry = mockk()

    private val selectedWallet = MutableStateFlow<UserWallet?>(null)
    private var stack: List<Route> = emptyList()

    private val handler = EarnDeepLinkHandler(
        userWalletsListRepository = userWalletsListRepository,
        singleAccountListSupplier = singleAccountListSupplier,
        earnAvailability = earnAvailability,
        productsRegistry = productsRegistry,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(router, userWalletsListRepository, singleAccountListSupplier, earnAvailability, productsRegistry)
        stack = emptyList()
        every { router.navigate(any(), any()) } answers {
            stack = firstArg<(List<Route>) -> List<Route>>().invoke(stack)
        }
        selectedWallet.value = wallet
        every { userWalletsListRepository.selectedUserWallet } returns selectedWallet
        coEvery { singleAccountListSupplier.getSyncOrNull(WALLET_ID) } returns accountList(main = listOf(eth))
        every { earnAvailability.isEarnAvailable(any(), any()) } returns true
        every { productsRegistry.find(any()) } returns null
        every { productsRegistry.find(LENDING) } returns mockk()
    }

    @Test
    fun `GIVEN token params without product WHEN handle THEN opens Earn entry with Auto start`() = runTest {
        // Act
        handle(params = ethParams())

        // Assert
        assertThat(stack).containsExactly(entry(start = AppRoute.EarnEntry.Start.Auto))
    }

    @Test
    fun `GIVEN known product WHEN handle THEN opens Earn entry with Info start`() = runTest {
        // Act
        handle(params = ethParams() + (EARN_PRODUCT_KEY to "lending"))

        // Assert
        assertThat(stack).containsExactly(entry(start = AppRoute.EarnEntry.Start.Info(LENDING)))
    }

    @Test
    fun `GIVEN capitalised product WHEN handle THEN product id is lowercased`() = runTest {
        // Act
        handle(params = ethParams() + (EARN_PRODUCT_KEY to "Lending"))

        // Assert
        assertThat(stack).containsExactly(entry(start = AppRoute.EarnEntry.Start.Info(LENDING)))
    }

    @Test
    fun `GIVEN unknown product WHEN handle THEN opens Earn entry with Auto start`() = runTest {
        // Act
        handle(params = ethParams() + (EARN_PRODUCT_KEY to "bogus"))

        // Assert
        assertThat(stack).containsExactly(entry(start = AppRoute.EarnEntry.Start.Auto))
    }

    @Test
    fun `GIVEN params in another case WHEN handle THEN currency is still found`() = runTest {
        // Arrange
        val params = mapOf(
            TOKEN_ID_KEY to eth.id.rawCurrencyId!!.value.uppercase(),
            NETWORK_ID_KEY to eth.network.rawId.uppercase(),
        )

        // Act
        handle(params = params)

        // Assert
        assertThat(stack).containsExactly(entry(start = AppRoute.EarnEntry.Start.Auto))
    }

    @Test
    fun `GIVEN ETH in main and second account WHEN handle THEN main account coin is used`() = runTest {
        // Arrange
        val secondAccountEth = eth.copy(name = "Ethereum #1")
        coEvery { singleAccountListSupplier.getSyncOrNull(WALLET_ID) } returns
            accountList(main = listOf(eth), second = listOf(secondAccountEth), isSecondListedFirst = true)

        // Act
        handle(params = ethParams())

        // Assert
        assertThat(stack).containsExactly(entry(start = AppRoute.EarnEntry.Start.Auto, currency = eth))
    }

    @Test
    fun `GIVEN ETH only in second account WHEN handle THEN that coin is used`() = runTest {
        // Arrange
        val secondAccountEth = eth.copy(name = "Ethereum #1")
        coEvery { singleAccountListSupplier.getSyncOrNull(WALLET_ID) } returns
            accountList(main = emptyList(), second = listOf(secondAccountEth))

        // Act
        handle(params = ethParams())

        // Assert
        assertThat(stack).containsExactly(
            entry(start = AppRoute.EarnEntry.Start.Auto, currency = secondAccountEth),
        )
    }

    @Test
    fun `GIVEN the same Earn entry in the stack WHEN handle THEN it is not pushed again`() = runTest {
        // Arrange
        val initialStack = listOf(AppRoute.Wallet, entry(start = AppRoute.EarnEntry.Start.Auto))
        stack = initialStack

        // Act
        handle(params = ethParams())

        // Assert
        assertThat(stack).isEqualTo(initialStack)
    }

    @Test
    fun `GIVEN blank token_id WHEN handle THEN account list is not loaded`() = runTest {
        // Act
        handle(params = ethParams() + (TOKEN_ID_KEY to " "))

        // Assert
        coVerify(exactly = 0) { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN unusable link WHEN handle THEN opens main screen`(model: FallbackModel) = runTest {
        // Arrange
        selectedWallet.value = model.selectedWallet
        coEvery { singleAccountListSupplier.getSyncOrNull(WALLET_ID) } returns model.accountList
        every { earnAvailability.isEarnAvailable(any(), any()) } returns model.isEarnAvailable

        // Act
        handle(params = model.params)

        // Assert
        verify(exactly = 1) { router.popTo(AppRoute.Wallet, any()) }
        assertThat(stack).isEmpty()
    }

    private fun provideTestModels() = listOf(
        FallbackModel(description = "token_id missing", params = ethParams() - TOKEN_ID_KEY),
        FallbackModel(description = "network_id missing", params = ethParams() - NETWORK_ID_KEY),
        FallbackModel(description = "token_id blank", params = ethParams() + (TOKEN_ID_KEY to "")),
        FallbackModel(description = "no selected wallet", selectedWallet = null),
        FallbackModel(description = "account list unavailable", accountList = null),
        FallbackModel(description = "token not in wallet", params = ethParams() + (TOKEN_ID_KEY to "tether")),
        FallbackModel(description = "Earn unavailable", isEarnAvailable = false),
    )

    private fun TestScope.handle(params: Map<String, String>) {
        handler.handle(
            DeepLinkRequest(
                uri = mockk<Uri>(),
                queryParams = params,
                scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
                isFromOnNewIntent = false,
                router = router,
            ),
        )
    }

    private fun entry(start: AppRoute.EarnEntry.Start, currency: CryptoCurrency = eth) = AppRoute.EarnEntry(
        userWalletId = WALLET_ID,
        cryptoCurrency = currency,
        source = AppRoute.EarnEntry.Source.Deeplink,
        start = start,
    )

    data class FallbackModel(
        val description: String,
        val params: Map<String, String> = ethParams(),
        val selectedWallet: UserWallet? = wallet,
        val accountList: AccountList? = accountList(main = listOf(eth)),
        val isEarnAvailable: Boolean = true,
    ) {
        override fun toString(): String = description
    }

    private companion object {
        val WALLET_ID = UserWalletId("011")
        val LENDING = EarnProductId("lending")

        val eth: CryptoCurrency.Coin = MockCryptoCurrencyFactory().createCoin(Blockchain.Ethereum)
        val wallet: UserWallet = mockk { every { walletId } returns WALLET_ID }

        fun ethParams(): Map<String, String> = mapOf(
            TOKEN_ID_KEY to eth.id.rawCurrencyId!!.value,
            NETWORK_ID_KEY to eth.network.rawId,
        )

        fun accountList(
            main: List<CryptoCurrency>,
            second: List<CryptoCurrency> = emptyList(),
            isSecondListedFirst: Boolean = false,
        ): AccountList {
            val mainAccount = Account.Personal.createMainAccount(userWalletId = WALLET_ID, cryptoCurrencies = main)
            val secondAccount = MockAccounts.createAccount(
                derivationIndex = 1,
                cryptoCurrencies = second,
                userWalletId = WALLET_ID,
            )
            return AccountList(
                userWalletId = WALLET_ID,
                accounts = if (isSecondListedFirst) {
                    listOf(secondAccount, mainAccount)
                } else {
                    listOf(mainAccount, secondAccount)
                },
                totalAccounts = 2,
                totalArchivedAccounts = 0,
                totalJointAccounts = 0,
            ).getOrNull()!!
        }
    }
}